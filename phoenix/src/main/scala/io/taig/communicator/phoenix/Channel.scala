package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.phoenix.message.Inbound
import monix.reactive.Observable

case class Channel(
        topic:  Topic,
        reader: Observable[Inbound],
        writer: ChannelWriter
) {
    def leave(): Unit = {
        logger.info( s"Leaving channel $topic" )
        writer.send( Event.Leave, Json.Null )
    }
}