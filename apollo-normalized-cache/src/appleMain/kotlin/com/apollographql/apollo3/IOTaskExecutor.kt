package com.apollographql.apollo3.cache.normalized.sql.internal

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSThread
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.posix.QOS_CLASS_DEFAULT
import platform.posix.open
import platform.posix.pthread_self
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach
import kotlin.native.concurrent.freeze

fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}
