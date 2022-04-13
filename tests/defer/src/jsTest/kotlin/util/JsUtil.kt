package util

@Suppress("NOTHING_TO_INLINE")
inline fun jsObject(noinline init: dynamic.() -> Unit): dynamic {
  val js = js("{}")
  init(js)
  return js
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> jsObject(noinline init: T.() -> Unit): T = (js("{}") as T).apply(init)

@Suppress("NOTHING_TO_INLINE")
inline fun objectFromEntries(@Suppress("UNUSED_PARAMETER") entries: dynamic) = js("Object.fromEntries(entries)")
