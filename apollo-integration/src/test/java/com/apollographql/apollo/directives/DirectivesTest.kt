package com.apollographql.apollo.directives

import com.apollographql.apollo.integration.directives.MyQuery
import com.google.common.truth.Truth
import okio.buffer
import okio.source
import org.junit.Test
import com.apollographql.apollo.api.Input

class DirectivesTest {
  @Test
  fun `parse does not crash with absent input`() {
    val responseJson = """
        {
          "data": {
            "getCityByName": {
              "__typename": "City",
              "id": "2643743"
            }
          }
        }
      """.trimIndent()
    val data = MyQuery(Input.absent()).parse(responseJson.byteInputStream().source().buffer()).data
    Truth.assertThat(data?.getCityByName?.__typename).isEqualTo("City")
  }

  @Test
  fun `parse does not over fetch with skip directive`() {
    val responseJson = """
        {
          "data": {
            "getCityByName": {
              "__typename": "City",
              "id": "2643743"
            }
          }
        }
      """.trimIndent()
    val data = MyQuery(Input.optional(false)).parse(responseJson.byteInputStream().source().buffer()).data
    Truth.assertThat(data!!.getCityByName!!.id).isEqualTo(null)
  }
}