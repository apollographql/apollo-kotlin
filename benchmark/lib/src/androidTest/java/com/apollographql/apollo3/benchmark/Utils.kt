package com.apollographql.apollo3.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.calendar.operation.ItemsQuery
import com.apollographql.apollo3.calendar.response.ItemsQuery.Data.Items.Edge.Node.Companion.itemFragment
import com.apollographql.apollo3.calendar.response.fragment.CalendarFragment.Provider.Node.Companion.calendarProviderFragment
import com.apollographql.apollo3.calendar.response.fragment.ItemFragment.Calendar.Node.Companion.calendarFragment
import okio.Buffer
import okio.BufferedSource
import okio.blackholeSink
import okio.buffer
import okio.source
import java.io.File

object Utils {
  private val cache = mutableMapOf<Int, BufferedSource>()

  const val dbName = "testDb"
  val dbFile: File = InstrumentationRegistry.getInstrumentation().context.getDatabasePath(dbName)
   val responseBasedQuery = com.apollographql.apollo3.calendar.response.ItemsQuery(endingAfter = "", startingBefore = "")
   val operationBasedQuery = ItemsQuery(endingAfter = "", startingBefore = "")

  /**
   * Reads a resource into a fully buffered [BufferedSource]. This function returns a peeked [BufferedSource]
   * so that very little allocation is done and the segments can be reused across invocations
   *
   * @return a [BufferedSource] containing the given resource id
   */
  fun resource(id: Int): BufferedSource {
    return cache.getOrPut(id) {
      InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(id)
          .source()
          .use {
            Buffer().apply {
              writeAll(it)
            }
          }
    }.peek()
  }

  private val cacheSizes = mutableMapOf<String, Long>()
  internal fun registerCacheSize(testName: String, size: Long) {
    cacheSizes.put(testName, size)
    File("/sdcard/cacheSizes").writeText(
        buildJsonString {
          AnyAdapter.toJson(this, CustomScalarAdapters.Empty, cacheSizes)
        }
    )
  }

  internal fun checkOperationBased(data: ItemsQuery.Data) {
    check(data.items!!.edges[248].node.itemFragment.calendar!!.node.calendarFragment.provider.node.calendarProviderFragment.id == "cc8e4c28-f178-11ec-8ea0-0242ac120002")
  }

  internal  fun checkResponseBased(data: com.apollographql.apollo3.calendar.response.ItemsQuery.Data) {
    check(data.items!!.edges[248].node.itemFragment()!!.calendar!!.node.calendarFragment()!!.provider.node.calendarProviderFragment()!!.id == "cc8e4c28-f178-11ec-8ea0-0242ac120002")
  }

}