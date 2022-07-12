import androidx.test.platform.app.InstrumentationRegistry
import okio.buffer
import okio.source

object Utils {
  fun resource(id: Int) = InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(id)
      .source()
      .buffer()
}