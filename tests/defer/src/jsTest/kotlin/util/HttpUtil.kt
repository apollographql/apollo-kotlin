package util

import http.OutgoingHttpHeaders
import kotlin.js.json

fun OutgoingHttpHeaders(vararg headers: Pair<String, Any?>) = json(*headers).unsafeCast<OutgoingHttpHeaders>()
