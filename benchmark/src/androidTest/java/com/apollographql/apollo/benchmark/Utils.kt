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
import com.apollographql.apollo.internal.response.RealResponseWriter
import com.apollographql.apollo.response.OperationResponseParser
import okio.BufferedSource
import okio.buffer
import okio.source

object Utils {
  fun bufferedSource() = InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(R.raw.largesample)
      .source()
      .buffer()

  private val responseNormalizer = object : ResponseNormalizer<Map<String, Any?>>() {
    override fun cacheKeyBuilder() = RealCacheKeyBuilder()

    override fun resolveCacheKey(field: ResponseField, record: Map<String, Any?>) = CacheKey.NO_KEY
  }

  fun <D : Operation.Data> computeRecordsDuringParsing(operation: Operation<D>, bufferedSource: BufferedSource): Collection<Record?>? {
    OperationResponseParser(
        operation,
        CustomScalarAdapters.DEFAULT,
        responseNormalizer
    ).parse(bufferedSource)

    return responseNormalizer.records()
  }

  fun <D : Operation.Data> computeRecordsAfterParsing(operation: Operation<D>, bufferedSource: BufferedSource): Collection<Record?>? {
    val response = OperationResponseParser(
        operation,
        CustomScalarAdapters.DEFAULT,
        ResponseNormalizer.NO_OP_NORMALIZER as ResponseNormalizer<MutableMap<String, Any>>?
    ).parse(bufferedSource)

    val writer = RealResponseWriter(operation.variables(), CustomScalarAdapters.DEFAULT)
    operation.adapter().toResponse(writer, response.data!!)
    responseNormalizer.willResolveRootQuery(operation);
    writer.resolveFields(responseNormalizer as ResolveDelegate<Map<String, Any>?>)
    return responseNormalizer.records()
  }
}