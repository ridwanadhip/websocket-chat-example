package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_SERVER_PORT
import io.vertx.core.Vertx
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatVerticleTest {
  private lateinit var verticle: ChatVerticle
  
  @BeforeAll
  fun setUp() {
    verticle = ChatVerticle()
    Vertx.vertx().deployVerticle(verticle)
  }
  
  @AfterAll
  fun tearDown() {
    verticle.vertx.close()
  }
  
  @Test
  fun testHttpServerConnection() {
    val client = verticle.vertx.createHttpClient()
    client.getNow("http://localhost:$HTTP_SERVER_PORT") { response ->
      assertEquals(200, response.statusCode())
    }
  }
  
  @Test
  fun testSqlConnection() {
    val database = verticle.database
    database.call("SELECT 1") { result ->
      assertTrue(result.succeeded())
    }
  }
}