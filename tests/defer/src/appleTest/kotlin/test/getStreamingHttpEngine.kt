package test

import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.StreamingNSURLSessionHttpEngine

actual fun getStreamingHttpEngine(): HttpEngine = StreamingNSURLSessionHttpEngine()
