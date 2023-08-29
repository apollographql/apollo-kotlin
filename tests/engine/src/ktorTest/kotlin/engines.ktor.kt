import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.KtorHttpEngine
import com.apollographql.apollo3.network.ws.KtorWebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketEngine

actual fun httpEngine(timeoutMillis: Long): HttpEngine {
  return KtorHttpEngine(timeoutMillis)
}

actual fun webSocketEngine(): WebSocketEngine {
  return KtorWebSocketEngine()
}

actual val isKtor = true