/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_PORT
import com.ridwan.chat.HTTP_DOMAIN
import com.ridwan.chat.controller.BusCommand
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

/**
 * Unit tests of ChatVerticle class. The main objective of the tests is to make
 * sure that every components of verticle (http server, database, etc.) behaves
 * normally.
 */
@ExtendWith(VertxExtension::class)
internal class ChatVerticleTest {
  private lateinit var verticle: ChatVerticle
  
  /**
   * Deploy verticle before running test.
   */
  @BeforeEach
  fun setUp(vertx: Vertx, test: VertxTestContext) {
    verticle = ChatVerticle()
    vertx.deployVerticle(verticle, test.completing())
  }
  
  /**
   * Clear database and undeploy verticle before executing other test.
   */
  @AfterEach
  fun tearDown(vertx: Vertx, test: VertxTestContext) {
    verticle.database.call("DROP SCHEMA PUBLIC CASCADE") {
      vertx.undeploy(verticle.deploymentID(), test.completing())
    }
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
    val content = "testSendMessageSuccess"

    val requestBody = jsonObjectOf(
      "content" to content
    )
  
    // verify that sent message is published. If true, then finish this test.
    val eventBus = vertx.eventBus()
    val command = BusCommand.SEND_MESSAGE.address
    val busConsumer = eventBus.consumer<String>(command)
    busConsumer.handler { message -> test.verify {
      assertEquals(content, message.body())
      busConsumer.unregister()
      test.completeNow()
    } }
  
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
          assertEquals(content, data.first().getString("content"))
          assertNotNull(data.first().getInstant("received_at"))
        } }
      }
    }

    postRequest
      .putHeader("Content-Type", "application/json")
      .end(requestBody.encode())
  }
  
  /**
   * Check if send message API will fail gracefully after receiving request with
   * wrong HTTP verb.
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
   * Check if send message API will fail gracefully after receiving request with
   * wrong content type header.
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
   * Check if send message API will fail gracefully after receiving request with
   * bad data.
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
   * Check if get messages API perform correctly.
   */
  @Test
  fun testGetMessagesSuccess(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val path = "/get-messages"
  
    val sqlQuery = """
      INSERT INTO message ("content", "received_at")
      VALUES (?, ?)
      """.trimMargin()
    
    val content = "testGetMessagesSuccess"
    val now = Instant.now()
    val params = jsonArrayOf(content, now.toString())
    val database = verticle.database
    
    database.updateWithParams(sqlQuery, params) { sqlResult -> test.verify {
      assertNull(sqlResult.cause())
      
      client.get(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
        assertEquals(200, response.statusCode())
        assertEquals("application/json", response.getHeader("Content-Type"))
    
        response.bodyHandler { body -> test.verify {
          val messages = Json.decodeValue(body.toString()) as JsonArray
          assertEquals(1, messages.size())
          val first = messages.first() as JsonObject
      
          assertNotNull(first.getInteger("id"))
          assertEquals(content, first.getString("content"))
          assertEquals(now, first.getInstant("received_at"))
          test.completeNow()
        } }
      } }.end()
    } }
  }
  
  /**
   * Check if get messages API will fail gracefully after receiving request with
   * wrong HTTP verb.
   */
  @Test
  fun testGetMessagesNotGet(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val requestBody = jsonObjectOf(
      "content" to "test"
    )
    
    val path = "/get-messages"
    client.post(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
      assertEquals(404, response.statusCode())
      test.completeNow()
    } }
      .putHeader("Content-Type", "application/json")
      .end(requestBody.encode())
  }
  
  /**
   * Check if get messages api will return proper result when database empty.
   */
  @Test
  fun testGetMessagesEmpty(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
    val path = "/get-messages"
    
    client.get(HTTP_PORT, HTTP_DOMAIN, path) { response -> test.verify {
      assertEquals(200, response.statusCode())
      assertEquals("application/json", response.getHeader("Content-Type"))
  
      response.bodyHandler { body -> test.verify {
        val messages = Json.decodeValue(body.toString()) as JsonArray
        assertEquals(0, messages.size())
        test.completeNow()
      } }
    } }.end()
  }
  
  /**
   * Check if display messages api handled properly.
   */
  @Test
  fun testDisplayMessageSuccess(vertx: Vertx, test: VertxTestContext) {
    val client = vertx.createHttpClient()
  
    // collect all messages sent by websocket server into an list
    val path = "/display-messages"
    val displayedMessages = mutableListOf<String>()
    client.websocket(HTTP_PORT, HTTP_DOMAIN, path) { connection ->
      connection.textMessageHandler { message ->
        displayedMessages.add(message)
      }
    }

    // test above code by calling send message API multiple times
    val message1 = "testDisplayMessageSuccess1"
    val message2 = "testDisplayMessageSuccess2"
    val message3 = "testDisplayMessageSuccess3"
    val expectedResult = listOf(message1, message2, message3)
    val jsonBody1 = jsonObjectOf("content" to message1)
    val jsonBody2 = jsonObjectOf("content" to message2)
    val jsonBody3 = jsonObjectOf("content" to message3)
    val sendMessagePath = "/send-message"
    
    client.jsonPost(sendMessagePath, jsonBody1) { test.verify {
      client.jsonPost(sendMessagePath, jsonBody2) { test.verify {
        client.jsonPost(sendMessagePath, jsonBody3) { test.verify {
          // wait until user received all messages
          vertx.setTimer(3000) { test.verify {
            assertIterableEquals(expectedResult, displayedMessages)
            test.completeNow()
          }}
        }}
      }}
    }}
  }
  
  /**
   * Helper method for calling post request with JSON body.
   * @param path the url path
   * @param body the JSON body
   * @param action code that needs to be executed after request had been
   *        completed
   */
  private fun HttpClient.jsonPost(path: String, body: JsonObject, action: () -> Unit) {
    this.post(HTTP_PORT, HTTP_DOMAIN, path) {
      action()
    }.putHeader("Content-Type", "application/json")
      .end(body.encode())
  }
}