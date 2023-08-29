
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketEngine

actual fun httpEngine(timeoutMillis: Long): HttpEngine {
  return DefaultHttpEngine(timeoutMillis)
}

actual fun webSocketEngine(): WebSocketEngine {
  return DefaultWebSocketEngine()
}

actual val isKtor = false