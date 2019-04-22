package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_SERVER_PORT
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.kotlin.core.json.*

class ChatVerticle: AbstractVerticle() {
  private lateinit var httpServer: HttpServer
  lateinit var database: JDBCClient
  
  override fun start(startFuture: Future<Void>?) {
    val dbConfig = json { obj(
      "url" to "jdbc:hsqldb:mem:test?shutdown=true",
      "driver_class" to "org.hsqldb.jdbcDriver"
    )}
    
    database = JDBCClient.createShared(vertx, dbConfig)
    httpServer = vertx.createHttpServer()
    httpServer.requestHandler { request -> request.response().end() }
    httpServer.listen(HTTP_SERVER_PORT)
    startFuture?.complete()
  }
  
  override fun stop(stopFuture: Future<Void>?) {
    httpServer.close()
    stopFuture?.complete()
  }
}