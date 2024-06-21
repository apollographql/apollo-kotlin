import com.apollographql.apollo3.engine.tests.Platform
import com.apollographql.apollo3.engine.tests.platform
import com.apollographql.apollo3.engine.tests.runAllTests
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test

class AllTests {
  @Test

  fun runAllTest() = runTest {
    runAllTests(
        engine = { DefaultHttpEngine(it) },
        webSocketEngine = { DefaultWebSocketEngine() },
        platform() != Platform.Native
    )
  }
}