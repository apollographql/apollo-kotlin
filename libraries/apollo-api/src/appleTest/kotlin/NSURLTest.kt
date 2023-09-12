import com.apollographql.apollo3.api.http.internal.urlEncode
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NSURLTest {
  @Test
  fun invalidCharThrows() {
    try {
      NSURL(string = "https://example.com/?a=À")
      fail("An exception was expected")
    } catch (e: NullPointerException) {

    }
  }

  // https://github.com/apollographql/apollo-kotlin/issues/5233
  @Test
  fun singleQuoteIsPercentEncoded() {
    val url = NSURL(string = "https://example.com/?a=" + "oreilly’".urlEncode())
    assertEquals(url.description, "https://example.com/?a=oreilly%E2%80%99")
  }

  @Test
  fun invalidCharIsPercentEncodedUsingUtf8() {
    val url = NSURL(string = "https://example.com/?a=" + "À".urlEncode())
    assertEquals(url.description, "https://example.com/?a=%C3%80")
  }
}