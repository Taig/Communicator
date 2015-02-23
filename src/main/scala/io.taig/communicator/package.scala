package io.taig

import scala.concurrent.Future
import scala.language.implicitConversions

package object communicator
{
	type CanceledIOException = exception.io.Canceled

	private[communicator] implicit def `Function0 -> Runnable`( f: => Unit ): Runnable = new Runnable
	{
		override def run() = f
	}

	implicit class RichFuture[T]( future: Future[T] )
	{
//		def flatRequest[R <: Response, I <: interceptor.Plain]( f: T => Request[R, I] )( implicit executor: ExecutionContext ): Request[R, I] =
//		{
//			future.flatMap( f ).asInstanceOf[Request[R, I]]
//		}
	}
}