/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_PORT
import com.ridwan.chat.HTTP_DOMAIN
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.core.json.obj
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * Unit tests of ChatVerticle class. The main objective of the tests is to make
 * sure that every components of verticle (http server, database, etc.) behaves
 * normally.
 */
@ExtendWith(VertxExtension::class)
internal class ChatVerticleTest {
  private lateinit var verticle: ChatVerticle
  
  /**
   * Prepare unit test class.
   */
  @BeforeEach
  fun setUp(vertx: Vertx, test: VertxTestContext) {
    verticle = ChatVerticle()
    vertx.deployVerticle(verticle, test.completing())
  }
  
  /**
   * Check if http server is working or not.
   */
  @Test
  fun testHttpServerConnection(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    client.get(HTTP_PORT, HTTP_DOMAIN, "/") { response -> test.verify {
      assertEquals(200, response.statusCode())
      test.completeNow()
    } }.end()
  }
  
  /**
   * Check if database is working or not.
   */
  @Test
  fun testSqlConnection(test: VertxTestContext) {
    val database = verticle.database
    database.getConnection { connection -> test.verify {
      assertNull(connection.cause())
      test.completeNow()
    } }
  }

  /**
   * Check if send message API is working or not.
   */
  @Test
  fun testSendMessageSuccess(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val message = "test"

    val requestBody = jsonObjectOf(
      "content" to message
    )
  
    val path = "/send-message"
    val postRequest = client.post(HTTP_PORT, HTTP_DOMAIN, path) { response ->
      test.verify {
        assertEquals(200, response.statusCode())
        val database = verticle.database
        val sqlQuery = """SELECT "content", "received_at" FROM message LIMIT 1"""
    
        // verify that sent message is saved in database
        database.query(sqlQuery) { queryResult -> test.verify {
          assertNull(queryResult.cause())
          val data = queryResult.result().rows
          assertEquals(1, data.size)
          assertEquals(message, data.first().getString("content"))
          assertNotNull(data.first().getInstant("received_at"))
          test.completeNow()
        } }
      }
    }

    postRequest
      .putHeader("Content-Type", "application/json")
      .end(requestBody.encode())
  }
  
  /**
   * Check if API will fail gracefully after receiving request with wrong HTTP
   * verb.
   */
  @Test
  fun testSendMessageNotPost(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val requestBody = jsonObjectOf(
      "content" to "test"
    )
  
    val path = "/send-message"
    client.get(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
      assertEquals(404, response.statusCode())
      test.completeNow()
    } }
    .putHeader("Content-Type", "application/json")
    .end(requestBody.encode())
  }
  
  /**
   * Check if API will fail gracefully after receiving request with wrong
   * content type header.
   */
  @Test
  fun testSendMessageWrongContentType(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val path = "/send-message"
    val requestBody = jsonObjectOf(
      "content" to "test"
    )
    
    // send request without content type header
    client.post(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
      assertEquals(400, response.statusCode())
  
      // send request with wrong content type header
      client.post(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
        assertEquals(400, response.statusCode())
        test.completeNow()
      } }
        .putHeader("Content-Type", "application/xml")
        .end(requestBody.encode())
    } }
      .end(requestBody.encode())
  }
  
  /**
   * Check if API will fail gracefully after receiving request with bad data.
   */
  @Test
  fun testSendMessageWrongBody(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val path = "/send-message"
    val requestBody = jsonObjectOf()

    // send empty body
    client.post(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
      assertEquals(400, response.statusCode())
  
      // send empty json
      client.post(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
        assertEquals(400, response.statusCode())
        test.completeNow()
      } }
        .putHeader("Content-Type", "application/json")
        .end(requestBody.encode())
    } }
      .putHeader("Content-Type", "application/json")
      .end()
  }
  
  /**
   * Check if get messages API perform correctly
   */
  @Test
  fun testGetMessagesSuccess(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val path = "/get-messages"
    
    client.get(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
      assertEquals(200, response.statusCode())
      assertEquals("application/json", response.getHeader("Content-Type"))
      
      response.bodyHandler { body -> test.verify {
        val messages = Json.decodeValue(body.toString()) as JsonArray
        val first = messages.first() as JsonObject
        
        assertNotNull(first.getInteger("id"))
        assertNotNull(first.getString("content"))
        assertNotEquals("", first.getString("content"))
        assertNotNull(first.getInstant("received_at"))
        test.completeNow()
      } }
    } }.end()
  }
}