import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.benchmark.test.R
import okio.buffer
import okio.source

object Utils {
  fun bufferedSource() = InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(R.raw.largesample)
      .source()
      .buffer()
}