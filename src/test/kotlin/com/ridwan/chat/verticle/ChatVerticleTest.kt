/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_SERVER_PORT
import io.vertx.core.Vertx
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests of ChatVerticle class. The main objective of the tests is to make
 * sure that every components of verticle (http server, database, etc.) behaves
 * normally.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatVerticleTest {
  private lateinit var verticle: ChatVerticle
  
  /**
   * Prepare unit test class.
   */
  @BeforeAll
  fun setUp() {
    verticle = ChatVerticle()
    Vertx.vertx().deployVerticle(verticle)
  }
  
  /**
   * Clean unit test class.
   */
  @AfterAll
  fun tearDown() {
    verticle.vertx.close()
  }
  
  /**
   * Check if http server is working or not.
   */
  @Test
  fun testHttpServerConnection() {
    val client = verticle.vertx.createHttpClient()
    client.getNow("http://localhost:$HTTP_SERVER_PORT") { response ->
      assertEquals(200, response.statusCode())
    }
  }
  
  /**
   * Check if database is working or not.
   */
  @Test
  fun testSqlConnection() {
    val database = verticle.database
    database.call("SELECT 1") { result ->
      assertTrue(result.succeeded())
    }
  }
}