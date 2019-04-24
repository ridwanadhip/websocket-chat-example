/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_PORT
import com.ridwan.chat.HTTP_DOMAIN
import io.vertx.core.Vertx
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
  fun testSendMessage(vertx: Vertx, test: VertxTestContext) {
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
        val sqlQuery = "SELECT content, received_at FROM message LIMIT 1"
    
        database.query(sqlQuery) { queryResult -> test.verify {
          assertNull(queryResult.cause())
          val data = queryResult.result().rows
          assertEquals(1, data.size)
          assertEquals(message, data.first().getString("CONTENT"))
          assertDoesNotThrow {
            data.first().getInstant("RECEIVED_AT") // check timestamp format
          }
          
          test.completeNow()
        } }
      }
    }

    postRequest
      .putHeader("Content-Type", "application/json")
      .end(requestBody.encode())
  }
}