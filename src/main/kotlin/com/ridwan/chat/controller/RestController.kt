/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.controller

import com.ridwan.chat.verticle.ChatVerticle
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import java.lang.Exception
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

/**
 * Controller class for processing HTTP request which passed by a chat verticle.
 * @param verticle the verticle that passing incoming HTTP request.
 */
class RestController(val verticle: ChatVerticle) : Handler<HttpServerRequest> {
  
  /**
   * Redirect each request to it's handler method, so it can be processed
   * furthermore.
   * @param request the passed HTTP request.
   */
  override fun handle(request: HttpServerRequest) {
    when (request.path()) {
      "/" -> handleIndex(request)
      "/send-message" -> handleSendMessage(request)
      "/get-messages" -> handleGetMessages(request)
      else -> request.response().setStatusCode(404).end()
    }
  }
  
  /**
   * Handle passed index request. Return 200 response if success.
   * @param request the passed HTTP request.
   */
  private fun handleIndex(request: HttpServerRequest) {
    val response = request.response()
    response.setStatusCode(200).end()
  }
  
  /**
   * Handle send message API. The request muse be POST, it's body must be JSON,
   * and requester must include message content as "content" param within
   * request body. If success, return 200 response with empty JSON body.
   * @param request the passed HTTP request.
   */
  private fun handleSendMessage(request: HttpServerRequest) {
    val response = request.response()
    
    if (request.method() != HttpMethod.POST) {
      response.setStatusCode(404).end()
      return
    }
  
    val contentType = request.headers().get("Content-Type")
    if (contentType.isNullOrBlank() || contentType != "application/json") {
      response.setStatusCode(400).end()
      return
    }
    
    request.bodyHandler { body ->
      // check if message content is JSON
      val jsonData = try {
        Json.decodeValue(body.toString()) as JsonObject
      } catch (e: DecodeException) {
        response.setStatusCode(400).end()
        return@bodyHandler
      }
      
      // check if message content is empty
      val content = jsonData.getString("content")
      if (content.isNullOrBlank()) {
        response.setStatusCode(400).end()
        return@bodyHandler
      }
      
      val sqlQuery = """
        INSERT INTO message ("content", "received_at")
        VALUES (?, ?)
        """.trimMargin()
  
      val now = Instant.now().toString()
      val params = jsonArrayOf(content, now)
      val database = verticle.database
      
      database.updateWithParams(sqlQuery, params) { sqlResult ->
        if (sqlResult.failed()) {
          response.setStatusCode(500).end()
          return@updateWithParams
        }
        
        val eventBus = verticle.vertx.eventBus()
        val command = BusCommand.SEND_MESSAGE.address
        eventBus.publish(command, content)
  
        response
          .putHeader("Content-Type", "application/json")
          .end(jsonObjectOf().encode()) // return empty json object
      }
    }
  }
  
  /**
   * Handle get messages API request. The HTTP request must be GET. If success,
   * return 200 response with JSON body containing all previously sent messages.
   * @param request the passed HTTP request.
   */
  private fun handleGetMessages(request: HttpServerRequest) {
    val response = request.response()
    
    if (request.method() != HttpMethod.GET) {
      response.setStatusCode(404).end()
      return
    }
  
    val sqlQuery = "SELECT \"id\", \"content\", \"received_at\" FROM message"
    val database = verticle.database
    database.query(sqlQuery) { sqlResult ->
      if (sqlResult.failed()) {
        response.setStatusCode(500).end()
        return@query
      }
  
      val result = JsonArray(sqlResult.result().rows)
      response
        .putHeader("Content-Type", "application/json")
        .end(result.encode())
    }
  }
}