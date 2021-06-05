package com.apollographql.apollo3.mpp

expect fun currentTimeMillis(): Long
expect fun currentThreadId(): String
expect fun ensureNeverFrozen(obj: Any)
expect fun isFrozen(obj: Any): Boolean