package test

import com.apollographql.apollo.api.Optional
import com.example.CreateUser2Query
import com.example.CreateUserQuery
import com.example.type.FindUserInput
import com.example.type.UserInput
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InputTest {
  @Test
  fun simple() {
    val input = UserInput.Builder()
        .name("Test User")
        .email("test@example.com")
        .build()

    assertEquals("Test User", input.name)
    assertEquals(Optional.present("test@example.com"), input.email)

    val query1 = CreateUserQuery.Builder()
        .input(input)
        .build()
    assertEquals("Test User", query1.input.name)

    val query2 = CreateUser2Query.Builder()
        .input(input)
        .build()
    assertEquals("Test User", query2.input?.name)
  }

  @Test
  fun oneOfWithConstructor() {
    FindUserInput(email = Optional.present("test@example.com"))

    var e = assertFailsWith<IllegalArgumentException> {
      FindUserInput(
          email = Optional.present("test@example.com"),
          name = Optional.present("Test User"),
      )
    }
    assertEquals("@oneOf input must have one field set (got 2)", e.message)

    e = assertFailsWith<IllegalArgumentException> {
      FindUserInput()
    }
    assertEquals("@oneOf input must have one field set (got 0)", e.message)

    e = assertFailsWith<IllegalArgumentException> {
      FindUserInput(email = Optional.present(null))
    }
    assertEquals("The value set on @oneOf input field must be non-null", e.message)

    e = assertFailsWith<IllegalArgumentException> {
      FindUserInput(
          email = Optional.present(null),
          name = Optional.present("Test User")
      )
    }
    assertEquals("@oneOf input must have one field set (got 2)", e.message)
  }

  @Test
  fun oneOfWithBuilder() {
    FindUserInput.Builder()
        .email("test@example.com")
        .build()

    var e = assertFailsWith<IllegalArgumentException> {
      FindUserInput.Builder()
          .email("test@example.com")
          .name("Test User")
          .build()
    }
    assertEquals("@oneOf input must have one field set (got 2)", e.message)

    e = assertFailsWith<IllegalArgumentException> {
      FindUserInput.Builder()
          .build()
    }
    assertEquals("@oneOf input must have one field set (got 0)", e.message)

    e = assertFailsWith<IllegalArgumentException> {
      FindUserInput.Builder()
          .email(null)
          .build()
    }
    assertEquals("The value set on @oneOf input field must be non-null", e.message)

    e = assertFailsWith<IllegalArgumentException> {
      FindUserInput.Builder()
          .email(null)
          .name("Test User")
          .build()
    }
    assertEquals("@oneOf input must have one field set (got 2)", e.message)
  }
}
