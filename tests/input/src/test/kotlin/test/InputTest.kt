package test

import com.apollographql.apollo3.api.Optional
import com.example.GetUser2Query
import com.example.GetUserQuery
import com.example.type.UserInput
import org.junit.Test
import kotlin.test.assertEquals

class InputTest {
  @Test
  fun simple() {
    val input = UserInput.Builder()
        .name("Test User")
        .email("test@example.com")
        .build()

    assertEquals("Test User", input.name)
    assertEquals(Optional.present("test@example.com"), input.email)

    val query1 = GetUserQuery.Builder()
        .input(input)
        .build()
    assertEquals("Test User", query1.input.name)

    val query2 = GetUser2Query.Builder()
        .input(input)
        .build()
    assertEquals("Test User", query2.input?.name)
  }
}
