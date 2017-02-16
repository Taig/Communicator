package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.OkHttpWebSocket
import io.taig.phoenix.models.{ Event ⇒ PEvent, _ }
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

case class Channel( topic: Topic )(
        val socket:  OkHttpWebSocket,
        val stream:  Observable[Inbound],
        val timeout: FiniteDuration
) extends io.taig.phoenix.Channel[Observable, Task] {
    override def send( event: PEvent, payload: Json ): Task[Option[Response]] = {
        val request = Request( topic, event, payload )
        Phoenix.send( request )( socket, stream, timeout )
    }
}

object Channel {
    def join(
        topic:   Topic,
        payload: Json  = Json.Null
    )(
        phoenix: Observable[Phoenix.Event]
    ): Observable[Event] = phoenix.flatMap {
        case Phoenix.Event.Available( phoenix ) ⇒
            import phoenix._

            val request = Request( topic, PEvent.Join, payload )
            val task = Phoenix.send( request )( socket, stream, timeout )

            Observable.fromTask( task ).map {
                case Some( Response.Confirmation( _, _, _ ) ) ⇒
                    val channel = Channel( topic )( socket, stream, timeout )
                    Event.Available( channel )
                case Some( error: Response.Error ) ⇒
                    Event.Failure( Some( error ) )
                case None ⇒ Event.Failure( None )
            }
        case Phoenix.Event.Unavailable ⇒
            Observable.now( Event.Unavailable )
    }

    sealed trait Event

    object Event {
        case class Available( channel: Channel ) extends Event

        sealed trait Error extends Event
        case class Failure( response: Option[Response.Error] ) extends Error
        case object Unavailable extends Error
    }
}