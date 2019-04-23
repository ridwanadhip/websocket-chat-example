/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat

import com.ridwan.chat.verticle.ChatVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx

/**
 * Main method for running this example program.
 *
 * @param args the command line arguments
 */
fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(ChatVerticle::class.java, DeploymentOptions())
}

