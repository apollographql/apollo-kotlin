package com.apollographql.apollo3.cache.normalized.sql.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.internal.BlobRecordSerializer
import com.apollographql.apollo3.cache.internal.blob.BlobQueries

internal class BlobRecordDatabase(private val blobQueries: BlobQueries): RecordDatabase {
  override fun select(key: String): Record? {
    return blobQueries.recordForKey(key).executeAsList()
        .map {
          BlobRecordSerializer.deserialize(it.key, it.blob)
        }
        .singleOrNull()
  }

  override fun select(keys: Collection<String>): List<Record> {
    return blobQueries.recordsForKeys(keys).executeAsList()
        .map {
          BlobRecordSerializer.deserialize(it.key, it.blob)
        }
  }

  override fun <T> transaction(noEnclosing: Boolean, body: () -> T): T {
    return blobQueries.transactionWithResult {
      body()
    }
  }

  override fun delete(key: String) {
    blobQueries.delete(key)
  }

  override fun deleteMatching(pattern: String) {
    blobQueries.deleteRecordsWithKeyMatching(pattern, "\\")
  }

  override fun deleteAll() {
    blobQueries.deleteAll()
  }

  override fun changes(): Long {
    return blobQueries.changes().executeAsOne()
  }

  override fun insert(record: Record) {
    blobQueries.insert(record.key, BlobRecordSerializer.serialize(record))
  }

  override fun update(record: Record) {
    blobQueries.update(BlobRecordSerializer.serialize(record), record.key)
  }

  override fun selectAll(): List<Record> {
    TODO("Not yet implemented")
  }
}