package io.taig.communicator.phoenix

import java.util.concurrent.TimeUnit

import cats.syntax.either._
import io.circe.parser._
import io.circe.syntax._
import io.circe.Json
import io.taig.communicator.phoenix.message.{ Inbound, Request, Response }
import io.taig.communicator.{ OkHttpRequest, OkHttpWebSocket }
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import monix.reactive.{ Observable, OverflowStrategy }
import okhttp3.OkHttpClient

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.concurrent.duration.Duration.{ Inf, Infinite }
import scala.language.postfixOps

class Phoenix(
        socket:     OkHttpWebSocket,
        val stream: Observable[Inbound],
        connection: Cancelable,
        heartbeat:  Cancelable,
        timeout:    Duration
) {
    //    val stream: Observable[Inbound] = {
    //        observable.collect {
    //            case WebSocket.Event.Message( Right( message ) ) ⇒
    //                decode[Inbound]( message ).valueOr( throw _ )
    //        }.doOnError( logger.error( "Failed to process message", _ ) )
    //            .doOnTerminate( logger.debug( "Stream terminated" ) )
    //    }

    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    ): Task[Either[Option[Response.Error], Channel]] = {
        Channel.join( topic, payload )(
            socket,
            stream.filter( topic isSubscribedTo _.topic ),
            timeout
        )
    }

    def close(): Unit = {
        heartbeat.cancel()

        val close = socket.close( 1000, null )

        if ( close ) {
            logger.debug( "Closing connection gracefully" )
        } else {
            logger.debug {
                "Cancelling connection, because socket can not be closed " +
                    "gracefully"
            }

            connection.cancel()
        }
    }
}

object Phoenix {
    def apply(
        request:   OkHttpRequest,
        strategy:  OverflowStrategy.Synchronous[WebSocket.Event] = OverflowStrategy.Unbounded,
        heartbeat: Option[FiniteDuration]                        = Default.heartbeat
    )(
        implicit
        ohc: OkHttpClient,
        s:   Scheduler
    ): Task[Phoenix] = Task.defer {
        val observable = WebSocket( request, strategy )
            .doOnNext {
                case WebSocket.Event.Open( _, _ ) ⇒
                    logger.debug( s"Opened socket connection" )
                case WebSocket.Event.Message( Right( message ) ) ⇒
                    logger.debug( s"Received message: $message" )
                case WebSocket.Event.Closing( code, _ ) ⇒
                    logger.debug( s"Closing connection: $code" )
                case WebSocket.Event.Closed( code, _ ) ⇒
                    logger.debug( s"Closed connection: $code" )
                case event ⇒
                    logger.warn( s"Received unexpected event (discarding): $event" )
            }
            .doOnError( logger.error( "Failed to process message", _ ) )
            .doOnTerminate( logger.debug( "Terminated connection" ) )
            .publish

        val connection = observable.connect()

        val timeout = ohc.readTimeoutMillis match {
            case 0            ⇒ Inf
            case milliseconds ⇒ Duration( milliseconds, TimeUnit.MILLISECONDS )
        }

        observable.collect {
            case WebSocket.Event.Open( socket, _ ) ⇒
                val heartbeats = heartbeat.fold( Cancelable.empty ) { delay ⇒
                    this.heartbeat( delay )
                        .doOnSubscriptionCancel {
                            logger.debug( "Cancelling heartbeat" )
                        }
                        .foreach { request ⇒
                            logger.debug( s"Sending heartbeat: $request" )
                            socket.send( request.asJson.noSpaces )
                        }
                }

                val stream = observable.collect {
                    case WebSocket.Event.Message( Right( message ) ) ⇒
                        decode[Inbound]( message ).valueOr( throw _ )
                }

                new Phoenix(
                    socket,
                    stream,
                    connection,
                    heartbeats,
                    timeout
                )
        }.firstL
    }

    def send(
        topic:   Topic,
        event:   Event,
        payload: Json  = Json.Null,
        ref:     Ref   = Ref.unique()
    )(
        socket:  OkHttpWebSocket,
        stream:  Observable[Inbound],
        timeout: Duration
    ): Task[Option[Response]] = {
        val request = Request( topic, event, payload, ref )

        val channel = stream
            .collect { case response: Response ⇒ response }
            .filter( _.ref == request.ref )
            .headOptionL

        val withTimeout = timeout match {
            case _: Infinite ⇒ channel
            case timeout: FiniteDuration ⇒
                channel.timeout( timeout ).onErrorRecover {
                    case _: TimeoutException ⇒ None
                }
        }

        val send = Task {
            socket.send( request.asJson.noSpaces )
        }

        Task.mapBoth( withTimeout, send )( ( left, _ ) ⇒ left )
    }

    def heartbeat( delay: FiniteDuration ): Observable[Request] = {
        Observable.intervalWithFixedDelay( delay, delay ).map { _ ⇒
            Request( Topic.Phoenix, Event( "heartbeat" ) )
        }
    }
}