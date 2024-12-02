package test

import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import kotlin.test.Ignore
import kotlin.test.Test


class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    println("starting...")

    // Starting a thread is important because the thread holds a ThreadLocal
    // reference to the session that won't be garbage collected if using the main thread.
    Thread {
      SqlNormalizedCacheFactory("foo.db")
          .create()
          .merge(Record("9", mapOf("key" to "value")), CacheHeaders.NONE)
    }.start()

    System.gc()
    println("wait for the connection to leak, you should see it in logcat.")
    // The leak should look like
    // A SQLiteConnection object for database '/data/user/0/com.example.myapplication/databases/foo.db' was leaked!
    Thread.sleep(60_000)
  }
}