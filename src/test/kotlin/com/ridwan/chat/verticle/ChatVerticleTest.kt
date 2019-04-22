package com.ridwan.chat.verticle

import com.ridwan.chat.HTTP_SERVER_PORT
import io.vertx.core.Vertx
import org.junit.jupiter.api.*

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
  fun testConditionAfterStartingUp() {
    val client = verticle.vertx.createHttpClient()
    client.getNow("http://localhost:$HTTP_SERVER_PORT") { response ->
      Assertions.assertEquals(200, response.statusCode())
    }
  }
}