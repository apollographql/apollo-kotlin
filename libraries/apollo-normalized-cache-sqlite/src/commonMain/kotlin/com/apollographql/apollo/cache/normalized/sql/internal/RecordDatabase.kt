package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.api.Record

/**
 * A database that can store [Record]
 *
 * All calls are synchronous, the calling code is expected to handle threading.
 *
 */
internal interface RecordDatabase {
  /**
   * @return the [Record] of null if there is no record for the given id
   */
  fun select(key: String): Record?

  /**
   * @return the list of records for the given ids
   * This is an optimization to avoid doing too many queries.
   *
   * @param ids the ids to get the record for. [ids.size] must be less than 999
   * @return the [Record] for the ids. If some [Record]s are missing, the returned list size might be
   * less that [ids]
   */
  fun select(keys: Collection<String>): List<Record>

  fun selectAll(): List<Record>

  /**
   * executes code in a transaction
   */
  fun <T> transaction(
      noEnclosing: Boolean = false,
      body: () -> T
  ): T

  fun delete(key: String)

  fun deleteMatching(pattern: String)

  fun deleteAll()

  /**
   * Returns the number of rows affected by the last query
   */
  fun changes(): Long

  fun insert(record: Record)
  fun update(record: Record)
}