import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import okio.buffer
import okio.source

object Utils {
  fun bufferedSource() = InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(R.raw.largesample)
      .source()
      .buffer()

  val responseNormalizer = object : ResponseNormalizer<Map<String, Any>?>() {
    override fun cacheKeyBuilder() = RealCacheKeyBuilder()

    override fun resolveCacheKey(field: ResponseField, record: Map<String, Any>?) = CacheKey.NO_KEY
  }
}