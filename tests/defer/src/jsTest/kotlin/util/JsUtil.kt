package util

import kotlin.js.Promise

external object Symbol {
  val asyncIterator: dynamic
}

@Suppress("NOTHING_TO_INLINE")
inline fun dynamicObject(noinline init: dynamic.() -> Unit): dynamic {
  val js = js("{}")
  init(js)
  return js
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> dynamicObject(noinline init: T.() -> Unit): T = (js("{}") as T).apply(init)

@Suppress("NOTHING_TO_INLINE")
inline fun objectFromEntries(@Suppress("UNUSED_PARAMETER") entries: dynamic) = js("Object.fromEntries(entries)")

private external interface JsIteratorYield<out T> {
  val value: T
  val done: Boolean
}

// Inspired by https://github.com/JetBrains/kotlin-wrappers/pull/1457
private fun <T> jsAsyncIterator(iteratorYield: JsIteratorYield<T>): dynamic {
  val asyncIteratorResult = dynamicObject {
    next = {
      Promise.resolve(iteratorYield)
    }
  }
  val result = dynamicObject {}
  result[Symbol.asyncIterator] = { asyncIteratorResult }
  return result
}

fun <T> jsAsyncIterator(next: () -> T, hasNext: () -> Boolean) = jsAsyncIterator(object : JsIteratorYield<T> {
  override val value: T
    get() = next()
  override val done: Boolean
    get() = !hasNext()
})
