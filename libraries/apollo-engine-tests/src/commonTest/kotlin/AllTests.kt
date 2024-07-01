import com.apollographql.apollo.engine.tests.Platform
import com.apollographql.apollo.engine.tests.platform
import com.apollographql.apollo.engine.tests.runAllTests
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo.testing.internal.runTest
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