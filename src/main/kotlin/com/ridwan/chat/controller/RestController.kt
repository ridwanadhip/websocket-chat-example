/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.controller

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest

class RestController : Handler<HttpServerRequest> {
  override fun handle(request: HttpServerRequest) {
    when (request.path()) {
      "/send-message" -> handleSendMessage(request)
      "/get-messages" -> handleGetMessages(request)
      else -> request.response().setStatusCode(404).end()
    }
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
  
    response.setStatusCode(200).end()
  }
  
  private fun handleGetMessages(request: HttpServerRequest) {
    val response = request.response()
    
    if (request.method() != HttpMethod.GET) {
      response.setStatusCode(404).end()
      return
    }
  
    response
      .putHeader("Content-Type", "application/json")
      .setStatusCode(200)
      .end()
  }
}