/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo.cache.http.internal

import com.apollographql.apollo.cache.http.internal.DiskLruCache.Editor
import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Sink
import okio.Source
import okio.blackholeSink
import okio.buffer
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.Flushable
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache entry has a string key
 * and a fixed number of values. Each key must match the regex **[a-z0-9_-]{1,64}**.
 * Values are byte sequences, accessible as streams or files. Each value must be between `0`
 * and `Integer.MAX_VALUE` bytes in length.
 *
 *
 * The cache stores its data in a directory on the filesystem. This directory must be exclusive
 * to the cache; the cache may delete or overwrite files from its directory. It is an error for
 * multiple processes to use the same cache directory at the same time.
 *
 *
 * This cache limits the number of bytes that it will store on the filesystem. When the number of
 * stored bytes exceeds the limit, the cache will remove entries in the background until the limit
 * is satisfied. The limit is not strict: the cache may temporarily exceed it while waiting for
 * files to be deleted. The limit does not include filesystem overhead or the cache journal so
 * space-sensitive applications should set a conservative limit.
 *
 *
 * Clients call [.edit] to create or update the values of an entry. An entry may have only
 * one editor at one time; if a value is not available to be edited then [.edit] will return
 * null.
 *
 *
 *  * When an entry is being **created** it is necessary to supply a full set of
 * values; the empty value should be used as a placeholder if necessary.
 *  * When an entry is being **edited**, it is not necessary to supply data for
 * every value; values default to their previous value.
 *
 *
 *
 * Every [.edit] call must be matched by a call to [Editor.commit] or [ ][Editor.abort]. Committing is atomic: a read observes the full set of values as they were before
 * or after the commit, but never a mix of values.
 *
 *
 * Clients call [.get] to read a snapshot of an entry. The read will observe the value at
 * the time that [.get] was called. Updates and removals after the call do not impact ongoing
 * reads.
 *
 *
 * This class is tolerant of some I/O errors. If files are missing from the filesystem, the
 * corresponding entries will be dropped from the cache. If an error occurs while writing a cache
 * value, the edit will fail silently. Callers should handle other problems by catching `IOException` and responding appropriately.
 *
 *
 * Copied from OkHttp 3.14.2: https://github.com/square/okhttp/blob/
 * b8b6ee831c65208940c741f8e091ff02425566d5/
 * okhttp/src/main/java/okhttp3/internal/cache/DiskLruCache.java
 */

internal class DiskLruCache(
/*
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */
    private val fileSystem: FileSystem,
    /** Returns the directory where this cache stores its data.  */
    private val directory: File,
    private val appVersion: Int, valueCount: Int, maxSize: Long,
    executor: Executor,
) : Closeable, Flushable {
  private val journalFile: File = File(directory, JOURNAL_FILE)
  private val journalFileTmp: File
  private val journalFileBackup: File
  private var maxSize: Long
  val valueCount: Int
  private var size: Long = 0
  var journalWriter: BufferedSink? = null
  val lruEntries = LinkedHashMap<String, Entry?>(0, 0.75f, true)
  var redundantOpCount = 0
  var hasJournalErrors = false

  // Must be read and written when synchronized on 'this'.
  var initialized = false

  /** Returns true if this cache has been closed.  */
  @get:Synchronized
  var isClosed = false
  var mostRecentTrimFailed = false
  var mostRecentRebuildFailed = false

  /**
   * To differentiate between old and current snapshots, each entry is given a sequence number each
   * time an edit is committed. A snapshot is stale if its sequence number is not equal to its
   * entry's sequence number.
   */
  private var nextSequenceNumber: Long = 0

  /** Used to run 'cleanupRunnable' for journal rebuilds.  */
  private val executor: Executor
  private val cleanupRunnable: Runnable = object : Runnable {
    override fun run() {
      synchronized(this@DiskLruCache) {
        if (!initialized || isClosed) {
          return  // Nothing to do
        }
        try {
          trimToSize()
        } catch (ignored: IOException) {
          mostRecentTrimFailed = true
        }
        try {
          if (journalRebuildRequired()) {
            rebuildJournal()
            redundantOpCount = 0
          }
        } catch (e: IOException) {
          mostRecentRebuildFailed = true
          journalWriter = blackholeSink().buffer()
        }
      }
    }
  }

  @Synchronized
  @Throws(IOException::class)
  fun initialize() {
    assert(Thread.holdsLock(this))
    if (initialized) {
      return  // Already initialized.
    }

    // If a bkp file exists, use it instead.
    if (fileSystem.exists(journalFileBackup)) {
      // If journal file also exists just delete backup file.
      if (fileSystem.exists(journalFile)) {
        fileSystem.delete(journalFileBackup)
      } else {
        fileSystem.rename(journalFileBackup, journalFile)
      }
    }

    // Prefer to pick up where we left off.
    if (fileSystem.exists(journalFile)) {
      try {
        readJournal()
        processJournal()
        initialized = true
        return
      } catch (journalIsCorrupt: IOException) {
        //logger.w("DiskLruCache " + directory + " is corrupt: "
        //    + journalIsCorrupt.getMessage() + ", removing", journalIsCorrupt);
      }

      // The cache is corrupted, attempt to delete the contents of the directory. This can throw and
      // we'll let that propagate out as it likely means there is a severe filesystem problem.
      try {
        delete()
      } finally {
        isClosed = false
      }
    }
    rebuildJournal()
    initialized = true
  }

  @Throws(IOException::class)
  private fun readJournal() {
    fileSystem.source(journalFile).buffer().use { source ->
      val magic = source.readUtf8LineStrict()
      val version = source.readUtf8LineStrict()
      val appVersionString = source.readUtf8LineStrict()
      val valueCountString = source.readUtf8LineStrict()
      val blank = source.readUtf8LineStrict()
      if (MAGIC != magic
          || VERSION_1 != version
          || appVersion.toString() != appVersionString
          || valueCount.toString() != valueCountString
          || "" != blank) {
        throw IOException("unexpected journal header: [" + magic + ", " + version + ", "
            + valueCountString + ", " + blank + "]")
      }
      var lineCount = 0
      while (true) {
        try {
          readJournalLine(source.readUtf8LineStrict())
          lineCount++
        } catch (endOfJournal: EOFException) {
          break
        }
      }
      redundantOpCount = lineCount - lruEntries.size

      // If we ended on a truncated line, rebuild the journal before appending to it.
      if (!source.exhausted()) {
        rebuildJournal()
      } else {
        journalWriter = newJournalWriter()
      }
    }
  }

  @Throws(FileNotFoundException::class)
  private fun newJournalWriter(): BufferedSink {
    val fileSink = fileSystem.appendingSink(journalFile)
    val faultHidingSink: Sink = object : FaultHidingSink(fileSink) {
      override fun onException(e: IOException?) {
        assert(Thread.holdsLock(this@DiskLruCache))
        hasJournalErrors = true
      }
    }
    return faultHidingSink.buffer()
  }

  @Throws(IOException::class)
  private fun readJournalLine(line: String) {
    val firstSpace = line.indexOf(' ')
    if (firstSpace == -1) {
      throw IOException("unexpected journal line: $line")
    }
    val keyBegin = firstSpace + 1
    val secondSpace = line.indexOf(' ', keyBegin)
    val key: String
    if (secondSpace == -1) {
      key = line.substring(keyBegin)
      if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
        lruEntries.remove(key)
        return
      }
    } else {
      key = line.substring(keyBegin, secondSpace)
    }
    var entry = lruEntries[key]
    if (entry == null) {
      entry = Entry(key)
      lruEntries[key] = entry
    }
    if (secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN)) {
      val parts = line.substring(secondSpace + 1).split(" ").toTypedArray()
      entry.readable = true
      entry.currentEditor = null
      entry.setLengths(parts)
    } else if (secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY)) {
      entry.currentEditor = Editor(entry)
    } else if (secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ)) {
      // This work was already done by calling lruEntries.get().
    } else {
      throw IOException("unexpected journal line: $line")
    }
  }

  /**
   * Computes the initial size and collects garbage as a part of opening the cache. Dirty entries
   * are assumed to be inconsistent and will be deleted.
   */
  @Throws(IOException::class)
  private fun processJournal() {
    fileSystem.delete(journalFileTmp)
    val i = lruEntries.values.iterator()
    while (i.hasNext()) {
      val entry = i.next()
      if (entry!!.currentEditor == null) {
        for (t in 0 until valueCount) {
          size += entry.lengths[t]
        }
      } else {
        entry.currentEditor = null
        for (t in 0 until valueCount) {
          fileSystem.delete(entry.cleanFiles[t])
          fileSystem.delete(entry.dirtyFiles[t])
        }
        i.remove()
      }
    }
  }

  /**
   * Creates a new journal that omits redundant information. This replaces the current journal if it
   * exists.
   */
  @Synchronized
  @Throws(IOException::class)
  fun rebuildJournal() {
    if (journalWriter != null) {
      journalWriter!!.close()
    }
    fileSystem.sink(journalFileTmp).buffer().use { writer ->
      writer.writeUtf8(MAGIC).writeByte('\n'.code)
      writer.writeUtf8(VERSION_1).writeByte('\n'.code)
      writer.writeDecimalLong(appVersion.toLong()).writeByte('\n'.code)
      writer.writeDecimalLong(valueCount.toLong()).writeByte('\n'.code)
      writer.writeByte('\n'.code)
      for (entry in lruEntries.values) {
        if (entry!!.currentEditor != null) {
          writer.writeUtf8(DIRTY).writeByte(' '.code)
          writer.writeUtf8(entry.key)
          writer.writeByte('\n'.code)
        } else {
          writer.writeUtf8(CLEAN).writeByte(' '.code)
          writer.writeUtf8(entry.key)
          entry.writeLengths(writer)
          writer.writeByte('\n'.code)
        }
      }
    }
    if (fileSystem.exists(journalFile)) {
      fileSystem.rename(journalFile, journalFileBackup)
    }
    fileSystem.rename(journalFileTmp, journalFile)
    fileSystem.delete(journalFileBackup)
    journalWriter = newJournalWriter()
    hasJournalErrors = false
    mostRecentRebuildFailed = false
  }

  /**
   * Returns a snapshot of the entry named `key`, or null if it doesn't exist is not currently
   * readable. If a value is returned, it is moved to the head of the LRU queue.
   */
  @Synchronized
  @Throws(IOException::class)
  operator fun get(key: String): Snapshot? {
    initialize()
    checkNotClosed()
    validateKey(key)
    val entry = lruEntries[key]
    if (entry == null || !entry.readable) return null
    val snapshot = entry.snapshot() ?: return null
    redundantOpCount++
    journalWriter!!.writeUtf8(READ).writeByte(' '.code).writeUtf8(key).writeByte('\n'.code)
    if (journalRebuildRequired()) {
      executor.execute(cleanupRunnable)
    }
    return snapshot
  }

  /**
   * Returns an editor for the entry named `key`, or null if another edit is in progress.
   */
  @Throws(IOException::class)
  fun edit(key: String): Editor? {
    return edit(key, ANY_SEQUENCE_NUMBER)
  }

  @Synchronized
  @Throws(IOException::class)
  fun edit(key: String, expectedSequenceNumber: Long): Editor? {
    initialize()
    checkNotClosed()
    validateKey(key)
    var entry = lruEntries[key]
    if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
            || entry.sequenceNumber != expectedSequenceNumber)) {
      return null // Snapshot is stale.
    }
    if (entry != null && entry.currentEditor != null) {
      return null // Another edit is in progress.
    }
    if (mostRecentTrimFailed || mostRecentRebuildFailed) {
      // The OS has become our enemy! If the trim job failed, it means we are storing more data than
      // requested by the user. Do not allow edits so we do not go over that limit any further. If
      // the journal rebuild failed, the journal writer will not be active, meaning we will not be
      // able to record the edit, causing file leaks. In both cases, we want to retry the clean up
      // so we can get out of this state!
      executor.execute(cleanupRunnable)
      return null
    }

    // Flush the journal before creating files to prevent file leaks.
    journalWriter!!.writeUtf8(DIRTY).writeByte(' '.code).writeUtf8(key).writeByte('\n'.code)
    journalWriter!!.flush()
    if (hasJournalErrors) {
      return null // Don't edit; the journal can't be written.
    }
    if (entry == null) {
      entry = Entry(key)
      lruEntries[key] = entry
    }
    val editor = Editor(entry)
    entry.currentEditor = editor
    return editor
  }

  /**
   * Changes the maximum number of bytes the cache can store and queues a job to trim the existing
   * store, if necessary.
   */
  @Synchronized
  fun setMaxSize(maxSize: Long) {
    this.maxSize = maxSize
    if (initialized) {
      executor.execute(cleanupRunnable)
    }
  }

  /**
   * Returns the number of bytes currently being used to store the values in this cache. This may be
   * greater than the max size if a background deletion is pending.
   */
  @Synchronized
  @Throws(IOException::class)
  fun size(): Long {
    initialize()
    return size
  }

  @Synchronized
  @Throws(IOException::class)
  fun completeEdit(editor: Editor, success: Boolean) {
    val entry = editor.entry
    check(entry.currentEditor == editor)

    // If this edit is creating the entry for the first time, every index must have a value.
    if (success && !entry.readable) {
      for (i in 0 until valueCount) {
        if (!editor.written!![i]) {
          editor.abort()
          throw IllegalStateException("Newly created entry didn't create value for index $i")
        }
        if (!fileSystem.exists(entry.dirtyFiles[i])) {
          editor.abort()
          return
        }
      }
    }
    for (i in 0 until valueCount) {
      val dirty = entry.dirtyFiles[i]
      if (success) {
        if (fileSystem.exists(dirty)) {
          val clean = entry.cleanFiles[i]
          fileSystem.rename(dirty, clean)
          val oldLength = entry.lengths[i]
          val newLength = fileSystem.size(clean)
          entry.lengths[i] = newLength
          size = size - oldLength + newLength
        }
      } else {
        fileSystem.delete(dirty)
      }
    }
    redundantOpCount++
    entry.currentEditor = null
    if (entry.readable || success) {
      entry.readable = true
      journalWriter!!.writeUtf8(CLEAN).writeByte(' '.code)
      journalWriter!!.writeUtf8(entry.key)
      entry.writeLengths(journalWriter)
      journalWriter!!.writeByte('\n'.code)
      if (success) {
        entry.sequenceNumber = nextSequenceNumber++
      }
    } else {
      lruEntries.remove(entry.key)
      journalWriter!!.writeUtf8(REMOVE).writeByte(' '.code)
      journalWriter!!.writeUtf8(entry.key)
      journalWriter!!.writeByte('\n'.code)
    }
    journalWriter!!.flush()
    if (size > maxSize || journalRebuildRequired()) {
      executor.execute(cleanupRunnable)
    }
  }

  /**
   * We only rebuild the journal when it will halve the size of the journal and eliminate at least
   * 2000 ops.
   */
  fun journalRebuildRequired(): Boolean {
    val redundantOpCompactThreshold = 2000
    return (redundantOpCount >= redundantOpCompactThreshold
        && redundantOpCount >= lruEntries.size)
  }

  /**
   * Drops the entry for `key` if it exists and can be removed. If the entry for `key`
   * is currently being edited, that edit will complete normally but its value will not be stored.
   *
   * @return true if an entry was removed.
   */
  @Synchronized
  @Throws(IOException::class)
  fun remove(key: String): Boolean {
    initialize()
    checkNotClosed()
    validateKey(key)
    val entry = lruEntries[key] ?: return false
    val removed = removeEntry(entry)
    if (removed && size <= maxSize) mostRecentTrimFailed = false
    return removed
  }

  @Throws(IOException::class)
  fun removeEntry(entry: Entry?): Boolean {
    if (entry!!.currentEditor != null) {
      entry.currentEditor!!.detach() // Prevent the edit from completing normally.
    }
    for (i in 0 until valueCount) {
      fileSystem.delete(entry.cleanFiles[i])
      size -= entry.lengths[i]
      entry.lengths[i] = 0
    }
    redundantOpCount++
    journalWriter!!.writeUtf8(REMOVE).writeByte(' '.code).writeUtf8(entry.key).writeByte('\n'.code)
    lruEntries.remove(entry.key)
    if (journalRebuildRequired()) {
      executor.execute(cleanupRunnable)
    }
    return true
  }

  @Synchronized
  private fun checkNotClosed() {
    check(!isClosed) { "cache is closed" }
  }

  /** Force buffered operations to the filesystem.  */
  @Synchronized
  @Throws(IOException::class)
  override fun flush() {
    if (!initialized) return
    checkNotClosed()
    trimToSize()
    journalWriter!!.flush()
  }

  /** Closes this cache. Stored values will remain on the filesystem.  */
  @Synchronized
  @Throws(IOException::class)
  override fun close() {
    if (!initialized || isClosed) {
      isClosed = true
      return
    }
    // Copying for safe iteration.
    for (entry in lruEntries.values.toTypedArray()) {
      if (entry?.currentEditor != null) {
        entry.currentEditor!!.abort()
      }
    }
    trimToSize()
    journalWriter!!.close()
    journalWriter = null
    isClosed = true
  }

  @Throws(IOException::class)
  fun trimToSize() {
    while (size > maxSize) {
      val toEvict = lruEntries.values.iterator().next()
      removeEntry(toEvict)
    }
    mostRecentTrimFailed = false
  }

  /**
   * Closes the cache and deletes all of its stored values. This will delete all files in the cache
   * directory including files that weren't created by the cache.
   */
  @Throws(IOException::class)
  fun delete() {
    close()
    fileSystem.deleteRecursively(directory)
  }

  /**
   * Deletes all stored values from the cache. In-flight edits will complete normally but their
   * values will not be stored.
   */
  @Synchronized
  @Throws(IOException::class)
  fun evictAll() {
    initialize()
    // Copying for safe iteration.
    for (entry in lruEntries.values.toTypedArray()) {
      removeEntry(entry)
    }
    mostRecentTrimFailed = false
  }

  private fun validateKey(key: String) {
    val matcher = LEGAL_KEY_PATTERN.matcher(key)
    require(matcher.matches()) { "keys must match regex [a-z0-9_-]{1,120}: \"$key\"" }
  }

  /**
   * Returns an iterator over the cache's current entries. This iterator doesn't throw `ConcurrentModificationException`, but if new entries are added while iterating, those new
   * entries will not be returned by the iterator. If existing entries are removed during iteration,
   * they will be absent (unless they were already returned).
   *
   *
   * If there are I/O problems during iteration, this iterator fails silently. For example, if
   * the hosting filesystem becomes unreachable, the iterator will omit elements rather than
   * throwing exceptions.
   *
   *
   * **The caller must [close][Snapshot.close]** each snapshot returned by
   * [Iterator.next]. Failing to do so leaks open files!
   *
   *
   * The returned iterator supports [Iterator.remove].
   */
  @Synchronized
  @Throws(IOException::class)
  fun snapshots(): MutableIterator<Snapshot> {
    initialize()
    return object : MutableIterator<Snapshot> {
      /** Iterate a copy of the entries to defend against concurrent modification errors.  */
      val delegate: Iterator<Entry?> = ArrayList(lruEntries.values).iterator()

      /** The snapshot to return from [.next]. Null if we haven't computed that yet.  */
      var nextSnapshot: Snapshot? = null

      /** The snapshot to remove with [.remove]. Null if removal is illegal.  */
      var removeSnapshot: Snapshot? = null
      override fun hasNext(): Boolean {
        if (nextSnapshot != null) return true
        synchronized(this@DiskLruCache) {

          // If the cache is closed, truncate the iterator.
          if (isClosed) return false
          while (delegate.hasNext()) {
            val entry = delegate.next()
            val snapshot = entry!!.snapshot() ?: continue
            // Evicted since we copied the entries.
            nextSnapshot = snapshot
            return true
          }
        }
        return false
      }

      override fun next(): Snapshot {
        if (!hasNext()) throw NoSuchElementException()
        removeSnapshot = nextSnapshot
        nextSnapshot = null
        return removeSnapshot!!
      }

      override fun remove() {
        checkNotNull(removeSnapshot) { "remove() before next()" }
        try {
          this@DiskLruCache.remove(removeSnapshot!!.key)
        } catch (ignored: IOException) {
          // Nothing useful to do here. We failed to remove from the cache. Most likely that's
          // because we couldn't update the journal, but the cached entry will still be gone.
        } finally {
          removeSnapshot = null
        }
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun closeQuietly(closeable: Closeable?, name: String?) {
    try {
      closeable?.close()
    } catch (e: Exception) {
      //logger.w(e, "Failed to close " + name);
    }
  }

  /** A snapshot of the values for an entry.  */
  inner class Snapshot internal constructor(val key: String, private val sequenceNumber: Long, private val sources: Array<Source>, private val lengths: LongArray) : Closeable {
    fun key(): String {
      return key
    }

    /**
     * Returns an editor for this snapshot's entry, or null if either the entry has changed since
     * this snapshot was created or if another edit is in progress.
     */
    @Throws(IOException::class)
    fun edit(): Editor? {
      return this@DiskLruCache.edit(key, sequenceNumber)
    }

    /** Returns the unbuffered stream with the value for `index`.  */
    fun getSource(index: Int): Source {
      return sources[index]
    }

    /** Returns the byte length of the value for `index`.  */
    fun getLength(index: Int): Long {
      return lengths[index]
    }

    override fun close() {
      for (`in` in sources) {
        closeQuietly(`in`, "source")
      }
    }
  }

  /** Edits the values for an entry.  */
  inner class Editor internal constructor(val entry: Entry) {
    val written: BooleanArray? = if (entry.readable) null else BooleanArray(valueCount)
    private var done = false

    /**
     * Prevents this editor from completing normally. This is necessary either when the edit causes
     * an I/O error, or if the target entry is evicted while this editor is active. In either case
     * we delete the editor's created files and prevent new files from being created. Note that once
     * an editor has been detached it is possible for another editor to edit the entry.
     */
    fun detach() {
      if (entry.currentEditor == this) {
        for (i in 0 until valueCount) {
          try {
            fileSystem.delete(entry.dirtyFiles[i])
          } catch (e: IOException) {
            // This file is potentially leaked. Not much we can do about that.
          }
        }
        entry.currentEditor = null
      }
    }

    /**
     * Returns an unbuffered input stream to read the last committed value, or null if no value has
     * been committed.
     */
    fun newSource(index: Int): Source? {
      synchronized(this@DiskLruCache) {
        check(!done)
        return if (!entry.readable || entry.currentEditor != this) {
          null
        } else try {
          fileSystem.source(entry.cleanFiles[index])
        } catch (e: FileNotFoundException) {
          null
        }
      }
    }

    /**
     * Returns a new unbuffered output stream to write the value at `index`. If the underlying
     * output stream encounters errors when writing to the filesystem, this edit will be aborted
     * when [.commit] is called. The returned output stream does not throw IOExceptions.
     */
    fun newSink(index: Int): Sink {
      synchronized(this@DiskLruCache) {
        check(!done)
        if (entry.currentEditor != this) {
          return blackholeSink()
        }
        if (!entry.readable) {
          written!![index] = true
        }
        val dirtyFile = entry.dirtyFiles[index]
        val sink: Sink? = try {
          fileSystem.sink(dirtyFile)
        } catch (e: FileNotFoundException) {
          return blackholeSink()
        }
        return object : FaultHidingSink(sink) {
          override fun onException(e: IOException?) {
            synchronized(this@DiskLruCache) { detach() }
          }
        }
      }
    }

    /**
     * Commits this edit so it is visible to readers.  This releases the edit lock so another edit
     * may be started on the same key.
     */
    @Throws(IOException::class)
    fun commit() {
      synchronized(this@DiskLruCache) {
        check(!done)
        if (entry.currentEditor == this) {
          completeEdit(this, true)
        }
        done = true
      }
    }

    /**
     * Aborts this edit. This releases the edit lock so another edit may be started on the same
     * key.
     */
    @Throws(IOException::class)
    fun abort() {
      synchronized(this@DiskLruCache) {
        check(!done)
        if (entry.currentEditor == this) {
          completeEdit(this, false)
        }
        done = true
      }
    }

    fun abortUnlessCommitted() {
      synchronized(this@DiskLruCache) {
        if (!done && entry.currentEditor == this) {
          try {
            completeEdit(this, false)
          } catch (ignored: IOException) {
          }
        }
      }
    }
  }

  inner class Entry internal constructor(val key: String) {
    /** Lengths of this entry's files.  */
    val lengths: LongArray = LongArray(valueCount)
    val cleanFiles: Array<File>
    val dirtyFiles: Array<File>

    /** True if this entry has ever been published.  */
    var readable = false

    /** The ongoing edit or null if this entry is not being edited.  */
    var currentEditor: Editor? = null

    /** The sequence number of the most recently committed edit to this entry.  */
    var sequenceNumber: Long = 0

    /** Set lengths using decimal numbers like "10123".  */
    @Throws(IOException::class)
    fun setLengths(strings: Array<String>) {
      if (strings.size != valueCount) {
        throw invalidLengths(strings)
      }
      try {
        for (i in strings.indices) {
          lengths[i] = strings[i].toLong()
        }
      } catch (e: NumberFormatException) {
        throw invalidLengths(strings)
      }
    }

    /** Append space-prefixed lengths to `writer`.  */
    @Throws(IOException::class)
    fun writeLengths(writer: BufferedSink?) {
      for (length in lengths) {
        writer!!.writeByte(' '.code).writeDecimalLong(length)
      }
    }

    @Throws(IOException::class)
    private fun invalidLengths(strings: Array<String>): IOException {
      throw IOException("unexpected journal line: " + strings.contentToString())
    }

    /**
     * Returns a snapshot of this entry. This opens all streams eagerly to guarantee that we see a
     * single published snapshot. If we opened streams lazily then the streams could come from
     * different edits.
     */
    fun snapshot(): Snapshot? {
      if (!Thread.holdsLock(this@DiskLruCache)) throw AssertionError()
      val sources = arrayOfNulls<Source>(valueCount)
      val lengths = lengths.clone() // Defensive copy since these can be zeroed out.
      return try {
        for (i in 0 until valueCount) {
          sources[i] = fileSystem.source(cleanFiles[i])
        }
        @Suppress("UNCHECKED_CAST")
        Snapshot(key, sequenceNumber, sources as Array<Source>, lengths)
      } catch (e: FileNotFoundException) {
        // A file must have been deleted manually!
        var i = 0
        while (i < valueCount) {
          if (sources[i] != null) {
            closeQuietly(sources[i], "file")
          } else {
            break
          }
          i++
        }
        // Since the entry is no longer valid, remove it so the metadata is accurate (i.e. the cache
        // size.)
        try {
          removeEntry(this)
        } catch (ignored: IOException) {
        }
        null
      }
    }

    init {
      val tmpCleanFiles = mutableListOf<File>()
      val tmpDirtyFiles = mutableListOf<File>()

      // The names are repetitive so re-use the same builder to avoid allocations.
      val fileBuilder = StringBuilder(key).append('.')
      val truncateTo = fileBuilder.length
      for (i in 0 until valueCount) {
        fileBuilder.append(i)
        tmpCleanFiles.add(File(directory, fileBuilder.toString()))
        fileBuilder.append(".tmp")
        tmpDirtyFiles.add(File(directory, fileBuilder.toString()))
        fileBuilder.setLength(truncateTo)
      }
      cleanFiles = tmpCleanFiles.toTypedArray()
      dirtyFiles = tmpDirtyFiles.toTypedArray()
    }
  }

  companion object {
    const val JOURNAL_FILE = "journal"
    const val JOURNAL_FILE_TEMP = "journal.tmp"
    const val JOURNAL_FILE_BACKUP = "journal.bkp"
    const val MAGIC = "libcore.io.DiskLruCache"
    const val VERSION_1 = "1"
    const val ANY_SEQUENCE_NUMBER: Long = -1
    val LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,120}")
    private const val CLEAN = "CLEAN"
    private const val DIRTY = "DIRTY"
    private const val REMOVE = "REMOVE"
    private const val READ = "READ"

    /**
     * Create a cache which will reside in `directory`. This cache is lazily initialized on
     * first access and will be created if it does not exist.
     *
     * @param directory a writable directory
     * @param valueCount the number of values per cache entry. Must be positive.
     * @param maxSize the maximum number of bytes this cache should use to store
     */
    @JvmStatic
    fun create(fileSystem: FileSystem, directory: File, appVersion: Int,
               valueCount: Int, maxSize: Long): DiskLruCache {
      require(maxSize > 0) { "maxSize <= 0" }
      require(valueCount > 0) { "valueCount <= 0" }

      // Use a single background thread to evict entries.
      val executor: Executor = ThreadPoolExecutor(
          0,
          1,
          60L,
          TimeUnit.SECONDS,
          LinkedBlockingQueue()
      ) { runnable ->
        val result = Thread(runnable, "OkHttp DiskLruCache")
        result.isDaemon = true
        result
      }
      return DiskLruCache(fileSystem, directory, appVersion, valueCount, maxSize, executor)
    }
  }

  init {
    journalFileTmp = File(directory, JOURNAL_FILE_TEMP)
    journalFileBackup = File(directory, JOURNAL_FILE_BACKUP)
    this.valueCount = valueCount
    this.maxSize = maxSize
    this.executor = executor
  }
}

private fun FileSystem.exists(file: File) = exists(file.toOkioPath())
private fun FileSystem.delete(file: File) = delete(file.toOkioPath())
private fun FileSystem.rename(from: File, to: File) = atomicMove(from.toOkioPath(), to.toOkioPath())
private fun FileSystem.source(file: File) = source(file.toOkioPath())
private fun FileSystem.appendingSink(file: File) = appendingSink(file.toOkioPath())
private fun FileSystem.sink(file: File): Sink {
  if (!file.exists()) {
    file.parentFile.mkdirs()
    file.createNewFile()
  }
  return sink(file.toOkioPath())
}

private fun FileSystem.deleteRecursively(file: File) = deleteRecursively(file.toOkioPath())
private fun FileSystem.size(file: File) = metadata(file.toOkioPath()).size ?: 0
