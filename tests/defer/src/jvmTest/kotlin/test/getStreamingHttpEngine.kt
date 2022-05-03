package test

import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine

actual fun getStreamingHttpEngine(): HttpEngine = DefaultHttpEngine()
