import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.ws.WebSocketEngine

expect fun httpEngine(timeoutMillis: Long = 60_000): HttpEngine
expect fun webSocketEngine(): WebSocketEngine
expect val isKtor: Boolean