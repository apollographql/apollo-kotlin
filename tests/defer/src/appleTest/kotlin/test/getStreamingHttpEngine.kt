package test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.StreamingNSURLSessionHttpEngine

@OptIn(ApolloExperimental::class)
actual fun getStreamingHttpEngine(): HttpEngine = StreamingNSURLSessionHttpEngine()
