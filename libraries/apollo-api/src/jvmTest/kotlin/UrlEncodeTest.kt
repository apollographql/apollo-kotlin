import com.apollographql.apollo.api.http.internal.urlEncode
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlEncodeTest {

  @Test
  fun test() {
    listOf(
        "%" to "%25",
        " " to "%20",
        "{}" to "%7B%7D",
        "e30=" to "e30%3D", // {} base64 encoded
    ).forEach {
      assertEquals(it.second, it.first.urlEncode())
    }
  }
}