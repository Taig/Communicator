package io.taig.communicator.result

import java.io.InputStream

import io.taig.communicator.Response

trait Handler
{
	def handle( response: Response, stream: InputStream ): Unit
}