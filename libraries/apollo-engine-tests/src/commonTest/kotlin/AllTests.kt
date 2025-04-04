import com.apollographql.apollo.engine.tests.Platform
import com.apollographql.apollo.engine.tests.platform
import com.apollographql.apollo.engine.tests.runAllTests
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.ws.DefaultWebSocketEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class AllTests {

  @Test
  fun runAllTest() = runTest {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
      runAllTests(
          engine = { DefaultHttpEngine(it) },
          webSocketEngine = { DefaultWebSocketEngine() },
          platform() != Platform.Native
      )
    }
  }
}