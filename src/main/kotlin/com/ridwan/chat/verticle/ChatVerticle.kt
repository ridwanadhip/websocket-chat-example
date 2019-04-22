package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_SERVER_PORT
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer

class ChatVerticle: AbstractVerticle() {
  private lateinit var httpServer: HttpServer
  
  override fun start(startFuture: Future<Void>?) {
    httpServer = vertx.createHttpServer()
    httpServer.requestHandler { request ->
      request.response().end("Hello World!")
    }
    
    httpServer.listen(HTTP_SERVER_PORT)
  }
  
  override fun stop(stopFuture: Future<Void>?) {
    httpServer.close()
  }
}