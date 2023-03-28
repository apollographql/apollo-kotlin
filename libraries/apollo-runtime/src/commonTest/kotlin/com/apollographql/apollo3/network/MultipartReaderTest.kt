package com.apollographql.apollo3.network

import com.apollographql.apollo3.internal.MultipartReader
import okio.Buffer
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultipartReaderTest {
  private val response = buildString {
    append("--graphql\r\n")
    append("content-type: application/json\r\n")
    append("\r\n")
    append("{\"payload\":{\"data\":{\"aReviewWasAdded\":{\"id\":3,\"body\":\"A new review for Apollo Studio\"}}},\"done\":false}")

    append("\r\n--graphql\r\n")
    append("content-type: application/json\r\n")
    append("\r\n")
    append("{\"payload\":{\"data\":{\"aReviewWasAdded\":{\"id\":23,\"body\":\"A new review for Apollo Studio\"}}},\"done\":false}")

    append("\r\n--graphql\r\n")
    append("content-type: application/json\r\n")
    append("\r\n")
    append("{\"errors\":[{\"message\":\"cannot read message from websocket\",\"extensions\":{\"code\":\"WEBSOCKET_MESSAGE_ERROR\"}}],\"done\":true}")

    append("\r\n--graphql--\r\n")
  }

  @Test
  fun finishesGraceFully() {
    var partCount = 0
    MultipartReader(Buffer().writeUtf8(response), "graphql").use {
      while (true) {
        val part = it.nextPart() ?: break
        //assertTrue(part.body.readUtf8().isNotEmpty())
        println(part.body.readUtf8())
        partCount++
      }
    }

    assertEquals(3, partCount)
  }
}