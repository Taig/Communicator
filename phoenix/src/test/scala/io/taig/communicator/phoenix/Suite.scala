package io.taig.communicator.phoenix

import cats.data.EitherT
import io.taig.communicator.OkHttpRequest
import monix.eval.Task
import org.scalatest.Assertion

import scala.concurrent.Future
import scala.language.implicitConversions

trait Suite extends io.taig.communicator.websocket.Suite {
    override val request = new OkHttpRequest.Builder()
        .url( s"ws://localhost:4000/socket/websocket" )
        .build()

    implicit def eitherTaskToFuture[A](
        either: EitherT[Task, A, Assertion]
    ): Future[Assertion] = either.value.map {
        case Right( assertion ) ⇒ assertion
        case Left( _ )          ⇒ fail()
    }
}