package com.ridwan.chat

import com.ridwan.chat.verticle.ChatVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(ChatVerticle::class.java, DeploymentOptions())
}

