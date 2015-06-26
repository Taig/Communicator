package io.taig.communicator

import com.squareup.okhttp.Call

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.Try

trait	Features[+R <: Response]
extends	Future[R]
{
	def wrapped: Future[R]

	def call: Call

	def interceptor: Interceptor

	def isCanceled = call.isCanceled

	def cancel() = call.cancel()

	def onSend( f: Progress.Send => Unit )( implicit executor: ExecutionContext ): this.type =
	{
		interceptor.onSend( ( progress: Progress.Send ) => executor.execute( f( progress ) ) )
		this
	}

	def onReceive( f: Progress.Receive => Unit )( implicit executor: ExecutionContext ): this.type =
	{
		interceptor.onReceive( ( progress: Progress.Receive ) => executor.execute( f( progress ) ) )
		this
	}

	override def onComplete[U]( f: Try[R] => U )( implicit executor: ExecutionContext ): Unit = wrapped.onComplete( f )

	override def isCompleted = wrapped.isCompleted

	override def value = wrapped.value

	@throws[Exception]
	override def result( atMost: Duration )( implicit permit: CanAwait ) = wrapped.result( atMost )

	@throws[InterruptedException]
	@throws[TimeoutException]
	override def ready( atMost: Duration )( implicit permit: CanAwait ) =
	{
		wrapped.ready( atMost )
		this
	}
}