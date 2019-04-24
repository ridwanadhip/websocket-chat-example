/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_PORT
import com.ridwan.chat.controller.RestController
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.kotlin.core.json.*

/**
 * The main component of the program. Before delving into details, here are some
 * jargon that may help reader to understanding the concepts:
 *
 * Vertx
 * In vert-x framework, a vertx is a class that responsible for executing code,
 * managing tasks, and handling network connections.
 *
 * Verticle
 * A verticle is piece of code that will be executed by vertx instance. Each
 * verticle is isolated to each other, so it will be safe if we want to deploy
 * multiple verticle with same or different class in one program.
 *
 * In this example program, this verticle is responsible for:
 * 1. Handling request for "send message" and "get all messages" API.
 * 2. Act as websocket server for handling "display message" API.
 *
 * Each responsibility represented in controller class. When user made request
 * to program, it will passed and processed to corresponding controller.
 *
 * When user send new message through "send message" API, it will be saved to
 * in-memory SQL database (HSQLDB). Then after been stored, user can retrieving
 * all sent messages from database using "get all messages" API. Each API url
 * written in RPC style, and consume/produce JSON data format.
 *
 * As a websocket server, this verticle will create long live connection after
 * user made request using "display message" API. Each time a new message saved
 * to database, this verticle will broadcast that message to all connected
 * clients via websocket connection. If a user connection is not reachable then
 * it will be disconnected automatically.
 */
class ChatVerticle: AbstractVerticle() {
  private lateinit var httpServer: HttpServer
  lateinit var database: JDBCClient
  
  /**
   * Prepare all resources of ChatVerticle instance. This method will be called
   * after verticle instance successfully deployed by vertx.
   * @param startFuture a future object that should to be called when everything
   *        is done.
   */
  override fun start(startFuture: Future<Void>?) {
    val dbConfig = jsonObjectOf(
      "url" to "jdbc:hsqldb:mem:test?shutdown=true",
      "driver_class" to "org.hsqldb.jdbcDriver"
    )
    
    database = JDBCClient.createShared(vertx, dbConfig)
    httpServer = vertx.createHttpServer()
    httpServer.requestHandler(RestController(this))
    httpServer.listen(HTTP_PORT)
    setupDatabase(startFuture)
  }
  
  /**
   * Clean up all resources before vertx undeploy the verticle instance.
   * @param stopFuture a future object that should to be called when everything
   *        is done.
   */
  override fun stop(stopFuture: Future<Void>?) {
    httpServer.close()
    stopFuture?.complete()
  }
  
  /**
   * Initialize database and it's table.
   * @param future a future object that should to be called when everything
   *        is done.
   */
  private fun setupDatabase(future: Future<Void>?) {
    val sqlQuery = """
      CREATE TABLE IF NOT EXISTS message (
      "id" INT IDENTITY PRIMARY KEY,
      "content" VARCHAR(255) NOT NULL,
      "received_at" TIMESTAMP NOT NULL
      )""".trimMargin()
    
    database.call(sqlQuery) { queryResult ->
      if (queryResult.failed()) {
        future?.fail(queryResult.cause())
        return@call
      }
      
      future?.complete()
    }
  }
}