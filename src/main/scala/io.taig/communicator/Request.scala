package io.taig.communicator

import java.io.IOException

import io.taig.communicator.event.Progress
import io.taig.communicator.interceptor.Interceptor
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import io.taig.communicator.request.{Content, Payload, Plain}
import io.taig.communicator.result.{Handler, Parser}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{CanAwait, ExecutionContext => Context, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

trait	Request[+R <: Response, +E <: event.Request, +I <: Interceptor[R, E]]
extends	Future[R]
with	Cancelable
{
	def client: OkHttpClient

	def request: okhttp.Request

	def executor: Context

	def interceptor: I

	protected val events = mutable.ListBuffer.empty[( Try[Response] => Any, Context )]

	protected val future = Future
	{
		try
		{
			val client = this.client.clone()
			client.networkInterceptors().add( interceptor )
			interceptor.wrap( client.newCall( request ).execute() )
		}
		catch
		{
			case error: IOException if error.getMessage == "Canceled" => cancel( -1 )
		}
	}( executor )

	// Execute stored events on underlying future complete
	future.onComplete( _ =>
	{
		events.foreach{ case ( event, executor ) => future.onComplete( event )( executor ) }
		events.clear()
	} )( executor )

	/**
	 * Enqueue an onComplete event
	 * 
	 * The events cannot be passed to onComplete right away while the Future is still running because this will
	 * lead to a wrong execution order.
	 * 
	 * @param event Event to execute when the Future is complete
	 */
	protected def enqueue( event: Try[R] => Any )( implicit executor: Context ): Unit =
	{
		if( isCompleted )
		{
			future.onComplete( event )
		}
		else
		{
			events.synchronized
			{
				events.append( ( event.asInstanceOf[Try[Response] => Any], executor ) )
			}
		}
	}

	override def isCanceled = interceptor.isCanceled

	override def cancel() = interceptor.cancel()

	def onSend( f: Progress.Send => Unit )( implicit executor: Context ): this.type =
	{
		interceptor.event.send = Option( ( progress: Progress.Send ) => executor.execute( f( progress ) ) )
		this
	}

	override def onFailure[U]( pf: PartialFunction[Throwable, U] )( implicit executor: Context ) =
	{
		onFailure( ( error: Throwable ) => pf.apply( error ): Unit )
	}

	def onFailure( f: Throwable => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Failure( error ) => f( error )
		case _ =>
	}

	override def onSuccess[U]( pf: PartialFunction[R, U] )( implicit executor: Context ) =
	{
		onSuccess( ( response: R ) => pf.apply( response ): Unit )
	}

	def onSuccess( f: R => Unit )( implicit executor: Context ): this.type = onFinish
	{
		case Success( response ) => f( response )
		case _ =>
	}

	def onFinish( f: ( Try[R] ) => Any )( implicit executor: Context ): this.type =
	{
		enqueue( f )
		this
	}

	override def onComplete[U]( f: ( Try[R] ) => U )( implicit executor: Context ) = onFinish( f )

	override def isCompleted = future.isCompleted

	override def value = future.value

	@throws[Exception]
	override def result( atMost: Duration )( implicit permit: CanAwait ) = future.result( atMost )

	@throws[InterruptedException]
	@throws[TimeoutException]
	override def ready( atMost: Duration )( implicit permit: CanAwait ) =
	{
		future.ready( atMost )
		this
	}
}

object Request
{
	def apply( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context ): Plain =
	{
		new Plain( client, request, executor )
	}

	def apply( request: okhttp.Request )( implicit client: OkHttpClient, executor: Context ): Plain =
	{
		apply( client, request )
	}

	def handle( client: OkHttpClient, request: okhttp.Request, handler: Handler )( implicit executor: Context ): Payload =
	{
		new Payload( client, request, handler, executor )
	}

	def handle( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context, handler: Handler ): Payload =
	{
		handle( client, request, handler )
	}

	def handle( request: okhttp.Request, handler: Handler )( implicit client: OkHttpClient, executor: Context ): Payload =
	{
		handle( client, request, handler )
	}

	def handle( request: okhttp.Request )( implicit client: OkHttpClient, handler: Handler, executor: Context ): Payload =
	{
		handle( client, request, handler )
	}

	def parse[T]( client: OkHttpClient, request: okhttp.Request, parser: Parser[T] )( implicit executor: Context ): Content[T] =
	{
		new Content[T]( client, request, parser, executor )
	}

	def parse[T]( client: OkHttpClient, request: okhttp.Request )( implicit executor: Context, parser: Parser[T] ): Content[T] =
	{
		parse( client, request, parser )
	}

	def parse[T]( request: okhttp.Request, parser: Parser[T] )( implicit client: OkHttpClient, executor: Context ): Content[T] =
	{
		parse( client, request, parser )
	}

	def parse[T]( request: okhttp.Request )( implicit client: OkHttpClient, parser: Parser[T], executor: Context ): Content[T] =
	{
		parse( client, request, parser )
	}
}