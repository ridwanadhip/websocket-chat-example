/*
 * Copyright (c) 2019 by Ridwan Adhi Pratama
 */

package com.ridwan.chat.controller

/**
 * List of all event bus command.
 */
enum class BusCommand(val address: String) {
  SEND_MESSAGE("message:send")
}