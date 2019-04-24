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
   * @param connection the websocket connection of a client.
   */
  override fun handle(connection: ServerWebSocket) {
  
  }
  
  /**
   * Publish newly written message to all connected websocket client.
   * @param connection the websocket connection of a client.
   */
  private fun publishMessage(connection: ServerWebSocket) {
  
  }
}