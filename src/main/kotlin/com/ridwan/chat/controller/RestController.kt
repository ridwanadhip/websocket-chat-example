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

class RestController(val verticle: ChatVerticle) : Handler<HttpServerRequest> {
  override fun handle(request: HttpServerRequest) {
    when (request.path()) {
      "/" -> handleIndex(request)
      "/send-message" -> handleSendMessage(request)
      "/get-messages" -> handleGetMessages(request)
      else -> request.response().setStatusCode(404).end()
    }
  }
  
  private fun handleIndex(request: HttpServerRequest) {
    val response = request.response()
    response.setStatusCode(200).end()
  }
  
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
      val jsonData = try {
        Json.decodeValue(body.toString()) as JsonObject
      } catch (e: DecodeException) {
        response.setStatusCode(400).end()
        return@bodyHandler
      }
      
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
  
        response
          .putHeader("Content-Type", "application/json")
          .end(jsonObjectOf().encode()) // return empty json object
      }
    }
  }
  
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