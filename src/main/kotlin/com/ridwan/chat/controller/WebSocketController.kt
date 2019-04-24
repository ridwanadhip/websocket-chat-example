/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.controller

import com.ridwan.chat.verticle.ChatVerticle
import io.vertx.core.Handler
import io.vertx.core.http.ServerWebSocket

/**
 * Controller class for processing websocket data from/to chat verticle.
 * @param verticle the verticle that controlling websocket server instance.
 */
class WebSocketController(val verticle: ChatVerticle) : Handler<ServerWebSocket> {
  
  /**
   * Catch all event that related to websocket and then passed it to a handler
   * method for further processing.
   * @param connection the individual websocket connection of a client.
   */
  override fun handle(connection: ServerWebSocket) {
    when (connection.path()) {
      "/display-messages" -> publishMessage(connection)
      else -> connection.reject(404)
    }
  }
  
  /**
   * Publish newly written message to all connected websocket clients.
   * @param connection the individual websocket connection of a client.
   */
  private fun publishMessage(connection: ServerWebSocket) {
    val eventBus = verticle.vertx.eventBus()
    val command = BusCommand.SEND_MESSAGE.address
  
    // broadcast message to all websocket clients.
    val busConsumer = eventBus.consumer<String>(command)
    connection.closeHandler {
      busConsumer.unregister()
    }
    
    busConsumer.handler { message ->
      connection.writeTextMessage(message.body())
    }
  }
}