import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ResolveDelegate
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.RealCacheKeyBuilder
import okio.buffer
import okio.source

object Utils {
  fun bufferedSource() = InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(R.raw.largesample)
      .source()
      .buffer()
}