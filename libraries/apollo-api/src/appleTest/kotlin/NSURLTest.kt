import com.apollographql.apollo.api.http.internal.urlEncode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class NSURLTest {
  @OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, ExperimentalNativeApi::class)
  @Test
  fun invalidCharThrows() {
    // Behaviour changed in newer OS versions. See https://developer.apple.com/documentation/foundation/nsurl
    val isFixed = NSProcessInfo.processInfo().operatingSystemVersion().useContents {
      val fixVersion = when(Platform.osFamily) {
        OsFamily.IOS -> 17
        OsFamily.TVOS -> 17
        OsFamily.WATCHOS -> 10
        OsFamily.MACOSX -> 14
        else -> Int.MAX_VALUE
      }

      majorVersion >= fixVersion
    }

    if (!isFixed) {
      try {
        NSURL(string = "https://example.com/?a=À")
        fail("An exception was expected")
      } catch (e: NullPointerException) {

      }
    } else {
      // New versions do percent encoding automatically
      assertEquals("https://example.com/?a=%C3%80", NSURL(string = "https://example.com/?a=À").description)
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