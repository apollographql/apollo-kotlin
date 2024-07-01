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

import com.apollographql.apollo.cache.http.internal.DiskLruCache.Companion.create
import com.google.common.truth.Truth
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Sink
import okio.Source
import okio.buffer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import java.io.File
import java.io.IOException
import java.util.ArrayDeque
import java.util.Deque
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Copied from OkHttp 3.14.2:
 * https://github.com/square/okhttp/blob/b8b6ee831c65208940c741f8e091ff02425566d5/okhttp-tests
 * /src/test/java/okhttp3/internal/cache/DiskLruCacheTest.java
 */
class DiskLruCacheTest {
  @get:Rule
  val tempDir = TemporaryFolder()

  @get:Rule
  val timeout = Timeout(60, TimeUnit.SECONDS)

  private val fileSystem = FaultyFileSystem(FileSystem.SYSTEM)
  private val appVersion = 100
  private var cacheDir: File? = null
  private var journalFile: File? = null
  private var journalBkpFile: File? = null
  private val executor = TestExecutor()
  private var cache: DiskLruCache? = null
  private val toClose: Deque<DiskLruCache> = ArrayDeque()

  @Throws(IOException::class)
  private fun createNewCache() {
    createNewCacheWithSize(Int.MAX_VALUE)
  }

  @Throws(IOException::class)
  private fun createNewCacheWithSize(maxSize: Int) {
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, maxSize.toLong(), executor)
    synchronized(cache!!) { cache!!.initialize() }
    toClose.add(cache)
  }

  @Before
  @Throws(Exception::class)
  fun setUp() {
    cacheDir = tempDir.root
    journalFile = File(cacheDir, DiskLruCache.JOURNAL_FILE)
    journalBkpFile = File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP)
    createNewCache()
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    while (!toClose.isEmpty()) {
      toClose.pop().close()
    }
  }

  @Test
  @Throws(Exception::class)
  fun emptyCache() {
    cache!!.close()
    assertJournalEquals()
  }

  @Test
  @Throws(IOException::class)
  fun recoverFromInitializationFailure() {
    // Add an uncommitted entry. This will get detected on initialization, and the cache will
    // attempt to delete the file. Do not explicitly close the cache here so the entry is left as
    // incomplete.
    val creator = cache!!.edit("k1")
    val sink = creator!!.newSink(0).buffer()
    sink.writeUtf8("Hello")
    sink.close()

    // Simulate a severe filesystem failure on the first initialization.
    fileSystem.setFaultyDelete(File(cacheDir, "k1.0.tmp"), true)
    fileSystem.setFaultyDelete(cacheDir!!, true)
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, Int.MAX_VALUE.toLong(), executor)
    toClose.add(cache)
    try {
      cache!!["k1"]
      Assert.fail()
    } catch (expected: IOException) {
    }

    // Now let it operate normally.
    fileSystem.setFaultyDelete(File(cacheDir, "k1.0.tmp"), false)
    fileSystem.setFaultyDelete(cacheDir!!, false)
    val snapshot = cache!!["k1"]
    Truth.assertThat(snapshot).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun validateKey() {
    var key: String? = null
    try {
      key = "has_space "
      cache!!.edit(key)
      Assert.fail("Expecting an IllegalArgumentException as the key was invalid.")
    } catch (iae: IllegalArgumentException) {
      Truth.assertThat(iae.message).isEqualTo(
          "keys must match regex [a-z0-9_-]{1,120}: \"$key\"")
    }
    try {
      key = "has_CR\r"
      cache!!.edit(key)
      Assert.fail("Expecting an IllegalArgumentException as the key was invalid.")
    } catch (iae: IllegalArgumentException) {
      Truth.assertThat(iae.message).isEqualTo(
          "keys must match regex [a-z0-9_-]{1,120}: \"$key\"")
    }
    try {
      key = "has_LF\n"
      cache!!.edit(key)
      Assert.fail("Expecting an IllegalArgumentException as the key was invalid.")
    } catch (iae: IllegalArgumentException) {
      Truth.assertThat(iae.message).isEqualTo(
          "keys must match regex [a-z0-9_-]{1,120}: \"$key\"")
    }
    try {
      key = "has_invalid/"
      cache!!.edit(key)
      Assert.fail("Expecting an IllegalArgumentException as the key was invalid.")
    } catch (iae: IllegalArgumentException) {
      Truth.assertThat(iae.message).isEqualTo(
          "keys must match regex [a-z0-9_-]{1,120}: \"$key\"")
    }
    try {
      key = "has_invalid\u2603"
      cache!!.edit(key)
      Assert.fail("Expecting an IllegalArgumentException as the key was invalid.")
    } catch (iae: IllegalArgumentException) {
      Truth.assertThat(iae.message).isEqualTo(
          "keys must match regex [a-z0-9_-]{1,120}: \"$key\"")
    }
    try {
      key = ("this_is_way_too_long_this_is_way_too_long_this_is_way_too_long_"
          + "this_is_way_too_long_this_is_way_too_long_this_is_way_too_long")
      cache!!.edit(key)
      Assert.fail("Expecting an IllegalArgumentException as the key was too long.")
    } catch (iae: IllegalArgumentException) {
      Truth.assertThat(iae.message).isEqualTo(
          "keys must match regex [a-z0-9_-]{1,120}: \"$key\"")
    }

    // Test valid cases.

    // Exactly 120.
    key = ("0123456789012345678901234567890123456789012345678901234567890123456789"
        + "01234567890123456789012345678901234567890123456789")
    cache!!.edit(key)!!.abort()
    // Contains all valid characters.
    key = "abcdefghijklmnopqrstuvwxyz_0123456789"
    cache!!.edit(key)!!.abort()
    // Contains dash.
    key = "-20384573948576"
    cache!!.edit(key)!!.abort()
  }

  @Test
  @Throws(Exception::class)
  fun writeAndReadEntry() {
    val creator = cache!!.edit("k1")
    setString(creator, 0, "ABC")
    setString(creator, 1, "DE")
    Truth.assertThat(creator!!.newSource(0)).isNull()
    Truth.assertThat(creator.newSource(1)).isNull()
    creator.commit()
    val snapshot = cache!!["k1"]
    assertSnapshotValue(snapshot, 0, "ABC")
    assertSnapshotValue(snapshot, 1, "DE")
  }

  @Test
  @Throws(Exception::class)
  fun readAndWriteEntryAcrossCacheOpenAndClose() {
    val creator = cache!!.edit("k1")
    setString(creator, 0, "A")
    setString(creator, 1, "B")
    creator!!.commit()
    cache!!.close()
    createNewCache()
    val snapshot = cache!!["k1"]
    assertSnapshotValue(snapshot, 0, "A")
    assertSnapshotValue(snapshot, 1, "B")
    snapshot!!.close()
  }

  @Test
  @Throws(Exception::class)
  fun readAndWriteEntryWithoutProperClose() {
    val creator = cache!!.edit("k1")
    setString(creator, 0, "A")
    setString(creator, 1, "B")
    creator!!.commit()

    // Simulate a dirty close of 'cache' by opening the cache directory again.
    createNewCache()
    val snapshot = cache!!["k1"]
    assertSnapshotValue(snapshot, 0, "A")
    assertSnapshotValue(snapshot, 1, "B")
    snapshot!!.close()
  }

  @Test
  @Throws(Exception::class)
  fun journalWithEditAndPublish() {
    val creator = cache!!.edit("k1")
    assertJournalEquals("DIRTY k1") // DIRTY must always be flushed.
    setString(creator, 0, "AB")
    setString(creator, 1, "C")
    creator!!.commit()
    cache!!.close()
    assertJournalEquals("DIRTY k1", "CLEAN k1 2 1")
  }

  @Test
  @Throws(Exception::class)
  fun revertedNewFileIsRemoveInJournal() {
    val creator = cache!!.edit("k1")
    assertJournalEquals("DIRTY k1") // DIRTY must always be flushed.
    setString(creator, 0, "AB")
    setString(creator, 1, "C")
    creator!!.abort()
    cache!!.close()
    assertJournalEquals("DIRTY k1", "REMOVE k1")
  }

  @Test
  @Throws(Exception::class)
  fun unterminatedEditIsRevertedOnClose() {
    cache!!.edit("k1")
    cache!!.close()
    assertJournalEquals("DIRTY k1", "REMOVE k1")
  }

  @Test
  @Throws(Exception::class)
  fun journalDoesNotIncludeReadOfYetUnpublishedValue() {
    val creator = cache!!.edit("k1")
    Truth.assertThat(cache!!["k1"]).isNull()
    setString(creator, 0, "A")
    setString(creator, 1, "BC")
    creator!!.commit()
    cache!!.close()
    assertJournalEquals("DIRTY k1", "CLEAN k1 1 2")
  }

  @Test
  @Throws(Exception::class)
  fun journalWithEditAndPublishAndRead() {
    val k1Creator = cache!!.edit("k1")
    setString(k1Creator, 0, "AB")
    setString(k1Creator, 1, "C")
    k1Creator!!.commit()
    val k2Creator = cache!!.edit("k2")
    setString(k2Creator, 0, "DEF")
    setString(k2Creator, 1, "G")
    k2Creator!!.commit()
    val k1Snapshot = cache!!["k1"]
    k1Snapshot!!.close()
    cache!!.close()
    assertJournalEquals("DIRTY k1", "CLEAN k1 2 1", "DIRTY k2", "CLEAN k2 3 1", "READ k1")
  }

  @Test
  @Throws(Exception::class)
  fun cannotOperateOnEditAfterPublish() {
    val editor = cache!!.edit("k1")
    setString(editor, 0, "A")
    setString(editor, 1, "B")
    editor!!.commit()
    assertInoperable(editor)
  }

  @Test
  @Throws(Exception::class)
  fun cannotOperateOnEditAfterRevert() {
    val editor = cache!!.edit("k1")
    setString(editor, 0, "A")
    setString(editor, 1, "B")
    editor!!.abort()
    assertInoperable(editor)
  }

  @Test
  @Throws(Exception::class)
  fun explicitRemoveAppliedToDiskImmediately() {
    val editor = cache!!.edit("k1")
    setString(editor, 0, "ABC")
    setString(editor, 1, "B")
    editor!!.commit()
    val k1 = getCleanFile("k1", 0)
    Truth.assertThat(readFile(k1)).isEqualTo("ABC")
    cache!!.remove("k1")
    Truth.assertThat(fileSystem.exists(k1)).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun removePreventsActiveEditFromStoringAValue() {
    set("a", "a", "a")
    val a = cache!!.edit("a")
    setString(a, 0, "a1")
    Truth.assertThat(cache!!.remove("a")).isTrue()
    setString(a, 1, "a2")
    a!!.commit()
    assertAbsent("a")
  }

  /**
   * Each read sees a snapshot of the file at the time read was called. This means that two reads of
   * the same key can see different data.
   */
  @Test
  @Throws(Exception::class)
  fun readAndWriteOverlapsMaintainConsistency() {
    val v1Creator = cache!!.edit("k1")
    setString(v1Creator, 0, "AAaa")
    setString(v1Creator, 1, "BBbb")
    v1Creator!!.commit()
    val snapshot1 = cache!!["k1"]
    val inV1 = snapshot1!!.getSource(0).buffer()
    Truth.assertThat(inV1.readByte()).isEqualTo('A'.code.toByte())
    Truth.assertThat(inV1.readByte()).isEqualTo('A'.code.toByte())
    val v1Updater = cache!!.edit("k1")
    setString(v1Updater, 0, "CCcc")
    setString(v1Updater, 1, "DDdd")
    v1Updater!!.commit()
    val snapshot2 = cache!!["k1"]
    assertSnapshotValue(snapshot2, 0, "CCcc")
    assertSnapshotValue(snapshot2, 1, "DDdd")
    snapshot2!!.close()
    Truth.assertThat(inV1.readByte()).isEqualTo('a'.code.toByte())
    Truth.assertThat(inV1.readByte()).isEqualTo('a'.code.toByte())
    assertSnapshotValue(snapshot1, 1, "BBbb")
    snapshot1.close()
  }

  @Test
  @Throws(Exception::class)
  fun openWithDirtyKeyDeletesAllFilesForThatKey() {
    cache!!.close()
    val cleanFile0 = getCleanFile("k1", 0)
    val cleanFile1 = getCleanFile("k1", 1)
    val dirtyFile0 = getDirtyFile("k1", 0)
    val dirtyFile1 = getDirtyFile("k1", 1)
    writeFile(cleanFile0, "A")
    writeFile(cleanFile1, "B")
    writeFile(dirtyFile0, "C")
    writeFile(dirtyFile1, "D")
    createJournal("CLEAN k1 1 1", "DIRTY   k1")
    createNewCache()
    Truth.assertThat(fileSystem.exists(cleanFile0)).isFalse()
    Truth.assertThat(fileSystem.exists(cleanFile1)).isFalse()
    Truth.assertThat(fileSystem.exists(dirtyFile0)).isFalse()
    Truth.assertThat(fileSystem.exists(dirtyFile1)).isFalse()
    Truth.assertThat(cache!!["k1"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun openWithInvalidVersionClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournalWithHeader(DiskLruCache.MAGIC, "0", "100", "2", "")
    createNewCache()
    assertGarbageFilesAllDeleted()
  }

  @Test
  @Throws(Exception::class)
  fun openWithInvalidAppVersionClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournalWithHeader(DiskLruCache.MAGIC, "1", "101", "2", "")
    createNewCache()
    assertGarbageFilesAllDeleted()
  }

  @Test
  @Throws(Exception::class)
  fun openWithInvalidValueCountClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournalWithHeader(DiskLruCache.MAGIC, "1", "100", "1", "")
    createNewCache()
    assertGarbageFilesAllDeleted()
  }

  @Test
  @Throws(Exception::class)
  fun openWithInvalidBlankLineClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournalWithHeader(DiskLruCache.MAGIC, "1", "100", "2", "x")
    createNewCache()
    assertGarbageFilesAllDeleted()
  }

  @Test
  @Throws(Exception::class)
  fun openWithInvalidJournalLineClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournal("CLEAN k1 1 1", "BOGUS")
    createNewCache()
    assertGarbageFilesAllDeleted()
    Truth.assertThat(cache!!["k1"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun openWithInvalidFileSizeClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournal("CLEAN k1 0000x001 1")
    createNewCache()
    assertGarbageFilesAllDeleted()
    Truth.assertThat(cache!!["k1"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun openWithTruncatedLineDiscardsThatLine() {
    cache!!.close()
    writeFile(getCleanFile("k1", 0), "A")
    writeFile(getCleanFile("k1", 1), "B")
    val sink = fileSystem.sink(journalFile!!).buffer()
    sink.writeUtf8("""
  ${DiskLruCache.MAGIC}
  ${DiskLruCache.VERSION_1}
  100
  2
  
  CLEAN k1 1 1
  """.trimIndent()) // no trailing newline
    sink.close()
    createNewCache()
    Truth.assertThat(cache!!["k1"]).isNull()

    // The journal is not corrupt when editing after a truncated line.
    set("k1", "C", "D")
    cache!!.close()
    createNewCache()
    assertValue("k1", "C", "D")
  }

  @Test
  @Throws(Exception::class)
  fun openWithTooManyFileSizesClearsDirectory() {
    cache!!.close()
    generateSomeGarbageFiles()
    createJournal("CLEAN k1 1 1 1")
    createNewCache()
    assertGarbageFilesAllDeleted()
    Truth.assertThat(cache!!["k1"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun keyWithSpaceNotPermitted() {
    try {
      cache!!.edit("my key")
      Assert.fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun keyWithNewlineNotPermitted() {
    try {
      cache!!.edit("my\nkey")
      Assert.fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun keyWithCarriageReturnNotPermitted() {
    try {
      cache!!.edit("my\rkey")
      Assert.fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun createNewEntryWithTooFewValuesFails() {
    val creator = cache!!.edit("k1")
    setString(creator, 1, "A")
    try {
      creator!!.commit()
      Assert.fail()
    } catch (expected: IllegalStateException) {
    }
    Truth.assertThat(fileSystem.exists(getCleanFile("k1", 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getCleanFile("k1", 1))).isFalse()
    Truth.assertThat(fileSystem.exists(getDirtyFile("k1", 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getDirtyFile("k1", 1))).isFalse()
    Truth.assertThat(cache!!["k1"]).isNull()
    val creator2 = cache!!.edit("k1")
    setString(creator2, 0, "B")
    setString(creator2, 1, "C")
    creator2!!.commit()
  }

  @Test
  @Throws(Exception::class)
  fun revertWithTooFewValues() {
    val creator = cache!!.edit("k1")
    setString(creator, 1, "A")
    creator!!.abort()
    Truth.assertThat(fileSystem.exists(getCleanFile("k1", 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getCleanFile("k1", 1))).isFalse()
    Truth.assertThat(fileSystem.exists(getDirtyFile("k1", 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getDirtyFile("k1", 1))).isFalse()
    Truth.assertThat(cache!!["k1"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun updateExistingEntryWithTooFewValuesReusesPreviousValues() {
    val creator = cache!!.edit("k1")
    setString(creator, 0, "A")
    setString(creator, 1, "B")
    creator!!.commit()
    val updater = cache!!.edit("k1")
    setString(updater, 0, "C")
    updater!!.commit()
    val snapshot = cache!!["k1"]
    assertSnapshotValue(snapshot, 0, "C")
    assertSnapshotValue(snapshot, 1, "B")
    snapshot!!.close()
  }

  @Test
  @Throws(Exception::class)
  fun growMaxSize() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "a", "aaa") // size 4
    set("b", "bb", "bbbb") // size 6
    cache!!.setMaxSize(20)
    set("c", "c", "c") // size 12
    Truth.assertThat(cache!!.size()).isEqualTo(12)
  }

  @Test
  @Throws(Exception::class)
  fun shrinkMaxSizeEvicts() {
    cache!!.close()
    createNewCacheWithSize(20)
    set("a", "a", "aaa") // size 4
    set("b", "bb", "bbbb") // size 6
    set("c", "c", "c") // size 12
    cache!!.setMaxSize(10)
    Truth.assertThat(executor.jobs.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun evictOnInsert() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "a", "aaa") // size 4
    set("b", "bb", "bbbb") // size 6
    Truth.assertThat(cache!!.size()).isEqualTo(10)

    // Cause the size to grow to 12 should evict 'A'.
    set("c", "c", "c")
    cache!!.flush()
    Truth.assertThat(cache!!.size()).isEqualTo(8)
    assertAbsent("a")
    assertValue("b", "bb", "bbbb")
    assertValue("c", "c", "c")

    // Causing the size to grow to 10 should evict nothing.
    set("d", "d", "d")
    cache!!.flush()
    Truth.assertThat(cache!!.size()).isEqualTo(10)
    assertAbsent("a")
    assertValue("b", "bb", "bbbb")
    assertValue("c", "c", "c")
    assertValue("d", "d", "d")

    // Causing the size to grow to 18 should evict 'B' and 'C'.
    set("e", "eeee", "eeee")
    cache!!.flush()
    Truth.assertThat(cache!!.size()).isEqualTo(10)
    assertAbsent("a")
    assertAbsent("b")
    assertAbsent("c")
    assertValue("d", "d", "d")
    assertValue("e", "eeee", "eeee")
  }

  @Test
  @Throws(Exception::class)
  fun evictOnUpdate() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "a", "aa") // size 3
    set("b", "b", "bb") // size 3
    set("c", "c", "cc") // size 3
    Truth.assertThat(cache!!.size()).isEqualTo(9)

    // Causing the size to grow to 11 should evict 'A'.
    set("b", "b", "bbbb")
    cache!!.flush()
    Truth.assertThat(cache!!.size()).isEqualTo(8)
    assertAbsent("a")
    assertValue("b", "b", "bbbb")
    assertValue("c", "c", "cc")
  }

  @Test
  @Throws(Exception::class)
  fun evictionHonorsLruFromCurrentSession() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "a", "a")
    set("b", "b", "b")
    set("c", "c", "c")
    set("d", "d", "d")
    set("e", "e", "e")
    cache!!["b"]!!.close() // 'B' is now least recently used.

    // Causing the size to grow to 12 should evict 'A'.
    set("f", "f", "f")
    // Causing the size to grow to 12 should evict 'C'.
    set("g", "g", "g")
    cache!!.flush()
    Truth.assertThat(cache!!.size()).isEqualTo(10)
    assertAbsent("a")
    assertValue("b", "b", "b")
    assertAbsent("c")
    assertValue("d", "d", "d")
    assertValue("e", "e", "e")
    assertValue("f", "f", "f")
  }

  @Test
  @Throws(Exception::class)
  fun evictionHonorsLruFromPreviousSession() {
    set("a", "a", "a")
    set("b", "b", "b")
    set("c", "c", "c")
    set("d", "d", "d")
    set("e", "e", "e")
    set("f", "f", "f")
    cache!!["b"]!!.close() // 'B' is now least recently used.
    Truth.assertThat(cache!!.size()).isEqualTo(12)
    cache!!.close()
    createNewCacheWithSize(10)
    set("g", "g", "g")
    cache!!.flush()
    Truth.assertThat(cache!!.size()).isEqualTo(10)
    assertAbsent("a")
    assertValue("b", "b", "b")
    assertAbsent("c")
    assertValue("d", "d", "d")
    assertValue("e", "e", "e")
    assertValue("f", "f", "f")
    assertValue("g", "g", "g")
  }

  @Test
  @Throws(Exception::class)
  fun cacheSingleEntryOfSizeGreaterThanMaxSize() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "aaaaa", "aaaaaa") // size=11
    cache!!.flush()
    assertAbsent("a")
  }

  @Test
  @Throws(Exception::class)
  fun cacheSingleValueOfSizeGreaterThanMaxSize() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "aaaaaaaaaaa", "a") // size=12
    cache!!.flush()
    assertAbsent("a")
  }

  @Test
  @Throws(Exception::class)
  fun constructorDoesNotAllowZeroCacheSize() {
    try {
      create(fileSystem, cacheDir!!, appVersion, 2, 0)
      Assert.fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun constructorDoesNotAllowZeroValuesPerEntry() {
    try {
      create(fileSystem, cacheDir!!, appVersion, 0, 10)
      Assert.fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun removeAbsentElement() {
    cache!!.remove("a")
  }

  @Test
  @Throws(Exception::class)
  fun readingTheSameStreamMultipleTimes() {
    set("a", "a", "b")
    val snapshot = cache!!["a"]
    Truth.assertThat(snapshot!!.getSource(0)).isEqualTo(snapshot.getSource(0))
    snapshot.close()
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalOnRepeatedReads() {
    set("a", "a", "a")
    set("b", "b", "b")
    while (executor.jobs.isEmpty()) {
      assertValue("a", "a", "a")
      assertValue("b", "b", "b")
    }
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalOnRepeatedEdits() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }
    executor.jobs.removeFirst().run()

    // Sanity check that a rebuilt journal behaves normally.
    assertValue("a", "a", "a")
    assertValue("b", "b", "b")
  }

  /** @see [Issue .28](https://github.com/JakeWharton/DiskLruCache/issues/28)
   */
  @Test
  @Throws(Exception::class)
  fun rebuildJournalOnRepeatedReadsWithOpenAndClose() {
    set("a", "a", "a")
    set("b", "b", "b")
    while (executor.jobs.isEmpty()) {
      assertValue("a", "a", "a")
      assertValue("b", "b", "b")
      cache!!.close()
      createNewCache()
    }
  }

  /** @see [Issue .28](https://github.com/JakeWharton/DiskLruCache/issues/28)
   */
  @Test
  @Throws(Exception::class)
  fun rebuildJournalOnRepeatedEditsWithOpenAndClose() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
      cache!!.close()
      createNewCache()
    }
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailurePreventsEditors() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()

    // Don't allow edits under any circumstances.
    Truth.assertThat(cache!!.edit("a")).isNull()
    Truth.assertThat(cache!!.edit("c")).isNull()
    val snapshot = cache!!["a"]
    Truth.assertThat(snapshot!!.edit()).isNull()
    snapshot.close()
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureIsRetried() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()

    // The rebuild is retried on cache hits and on cache edits.
    val snapshot = cache!!["b"]
    snapshot!!.close()
    Truth.assertThat(cache!!.edit("d")).isNull()
    Truth.assertThat(executor.jobs.size).isEqualTo(2)

    // On cache misses, no retry job is queued.
    Truth.assertThat(cache!!["c"]).isNull()
    Truth.assertThat(executor.jobs.size).isEqualTo(2)

    // Let the rebuild complete successfully.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), false)
    executor.jobs.removeFirst().run()
    assertJournalEquals("CLEAN a 1 1", "CLEAN b 1 1")
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureWithInFlightEditors() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }
    val commitEditor = cache!!.edit("c")
    val abortEditor = cache!!.edit("d")
    cache!!.edit("e") // Grab an editor, but don't do anything with it.

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()

    // In-flight editors can commit and have their values retained.
    setString(commitEditor, 0, "c")
    setString(commitEditor, 1, "c")
    commitEditor!!.commit()
    assertValue("c", "c", "c")
    abortEditor!!.abort()

    // Let the rebuild complete successfully.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), false)
    executor.jobs.removeFirst().run()
    assertJournalEquals("CLEAN a 1 1", "CLEAN b 1 1", "DIRTY e", "CLEAN c 1 1")
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureWithEditorsInFlightThenClose() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }
    val commitEditor = cache!!.edit("c")
    val abortEditor = cache!!.edit("d")
    cache!!.edit("e") // Grab an editor, but don't do anything with it.

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()
    setString(commitEditor, 0, "c")
    setString(commitEditor, 1, "c")
    commitEditor!!.commit()
    assertValue("c", "c", "c")
    abortEditor!!.abort()
    cache!!.close()
    createNewCache()

    // Although 'c' successfully committed above, the journal wasn't available to issue a CLEAN op.
    // Because the last state of 'c' was DIRTY before the journal failed, it should be removed
    // entirely on a subsequent open.
    Truth.assertThat(cache!!.size()).isEqualTo(4)
    assertAbsent("c")
    assertAbsent("d")
    assertAbsent("e")
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureAllowsRemovals() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()
    Truth.assertThat(cache!!.remove("a")).isTrue()
    assertAbsent("a")

    // Let the rebuild complete successfully.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), false)
    executor.jobs.removeFirst().run()
    assertJournalEquals("CLEAN b 1 1")
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureWithRemovalThenClose() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()
    Truth.assertThat(cache!!.remove("a")).isTrue()
    assertAbsent("a")
    cache!!.close()
    createNewCache()

    // The journal will have no record that 'a' was removed. It will have an entry for 'a', but when
    // it tries to read the cache files, it will find they were deleted. Once it encounters an entry
    // with missing cache files, it should remove it from the cache entirely.
    Truth.assertThat(cache!!.size()).isEqualTo(4)
    Truth.assertThat(cache!!["a"]).isNull()
    Truth.assertThat(cache!!.size()).isEqualTo(2)
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureAllowsEvictAll() {
    while (executor.jobs.isEmpty()) {
      set("a", "a", "a")
      set("b", "b", "b")
    }

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()
    cache!!.evictAll()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    assertAbsent("a")
    assertAbsent("b")
    cache!!.close()
    createNewCache()

    // The journal has no record that 'a' and 'b' were removed. It will have an entry for both, but
    // when it tries to read the cache files for either entry, it will discover the cache files are
    // missing and remove the entries from the cache.
    Truth.assertThat(cache!!.size()).isEqualTo(4)
    Truth.assertThat(cache!!["a"]).isNull()
    Truth.assertThat(cache!!["b"]).isNull()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun rebuildJournalFailureWithCacheTrim() {
    while (executor.jobs.isEmpty()) {
      set("a", "aa", "aa")
      set("b", "bb", "bb")
    }

    // Cause the rebuild action to fail.
    fileSystem.setFaultyRename(File(cacheDir, DiskLruCache.JOURNAL_FILE_BACKUP), true)
    executor.jobs.removeFirst().run()

    // Trigger a job to trim the cache.
    cache!!.setMaxSize(4)
    executor.jobs.removeFirst().run()
    assertAbsent("a")
    assertValue("b", "bb", "bb")
  }

  @Test
  @Throws(Exception::class)
  fun restoreBackupFile() {
    val creator = cache!!.edit("k1")
    setString(creator, 0, "ABC")
    setString(creator, 1, "DE")
    creator!!.commit()
    cache!!.close()
    fileSystem.rename(journalFile!!, journalBkpFile!!)
    Truth.assertThat(fileSystem.exists(journalFile!!)).isFalse()
    createNewCache()
    val snapshot = cache!!["k1"]
    assertSnapshotValue(snapshot, 0, "ABC")
    assertSnapshotValue(snapshot, 1, "DE")
    Truth.assertThat(fileSystem.exists(journalBkpFile!!)).isFalse()
    Truth.assertThat(fileSystem.exists(journalFile!!)).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun journalFileIsPreferredOverBackupFile() {
    var creator = cache!!.edit("k1")
    setString(creator, 0, "ABC")
    setString(creator, 1, "DE")
    creator!!.commit()
    cache!!.flush()
    copyFile(journalFile, journalBkpFile)
    creator = cache!!.edit("k2")
    setString(creator, 0, "F")
    setString(creator, 1, "GH")
    creator!!.commit()
    cache!!.close()
    Truth.assertThat(fileSystem.exists(journalFile!!)).isTrue()
    Truth.assertThat(fileSystem.exists(journalBkpFile!!)).isTrue()
    createNewCache()
    val snapshotA = cache!!["k1"]
    assertSnapshotValue(snapshotA, 0, "ABC")
    assertSnapshotValue(snapshotA, 1, "DE")
    val snapshotB = cache!!["k2"]
    assertSnapshotValue(snapshotB, 0, "F")
    assertSnapshotValue(snapshotB, 1, "GH")
    Truth.assertThat(fileSystem.exists(journalBkpFile!!)).isFalse()
    Truth.assertThat(fileSystem.exists(journalFile!!)).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun openCreatesDirectoryIfNecessary() {
    cache!!.close()
    val dir = tempDir.newFolder("testOpenCreatesDirectoryIfNecessary")
    cache = create(fileSystem, dir, appVersion, 2, Int.MAX_VALUE.toLong())
    set("a", "a", "a")
    Truth.assertThat(fileSystem.exists(File(dir, "a.0"))).isTrue()
    Truth.assertThat(fileSystem.exists(File(dir, "a.1"))).isTrue()
    Truth.assertThat(fileSystem.exists(File(dir, "journal"))).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun fileDeletedExternally() {
    set("a", "a", "a")
    fileSystem.delete(getCleanFile("a", 1))
    Truth.assertThat(cache!!["a"]).isNull()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun editSameVersion() {
    set("a", "a", "a")
    val snapshot = cache!!["a"]
    val editor = snapshot!!.edit()
    setString(editor, 1, "a2")
    editor!!.commit()
    assertValue("a", "a", "a2")
  }

  @Test
  @Throws(Exception::class)
  fun editSnapshotAfterChangeAborted() {
    set("a", "a", "a")
    val snapshot = cache!!["a"]
    val toAbort = snapshot!!.edit()
    setString(toAbort, 0, "b")
    toAbort!!.abort()
    val editor = snapshot.edit()
    setString(editor, 1, "a2")
    editor!!.commit()
    assertValue("a", "a", "a2")
  }

  @Test
  @Throws(Exception::class)
  fun editSnapshotAfterChangeCommitted() {
    set("a", "a", "a")
    val snapshot = cache!!["a"]
    val toAbort = snapshot!!.edit()
    setString(toAbort, 0, "b")
    toAbort!!.commit()
    Truth.assertThat(snapshot.edit()).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun editSinceEvicted() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "aa", "aaa") // size 5
    val snapshot = cache!!["a"]
    set("b", "bb", "bbb") // size 5
    set("c", "cc", "ccc") // size 5; will evict 'A'
    cache!!.flush()
    Truth.assertThat(snapshot!!.edit()).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun editSinceEvictedAndRecreated() {
    cache!!.close()
    createNewCacheWithSize(10)
    set("a", "aa", "aaa") // size 5
    val snapshot = cache!!["a"]
    set("b", "bb", "bbb") // size 5
    set("c", "cc", "ccc") // size 5; will evict 'A'
    set("a", "a", "aaaa") // size 5; will evict 'B'
    cache!!.flush()
    Truth.assertThat(snapshot!!.edit()).isNull()
  }

  /** @see [Issue .2](https://github.com/JakeWharton/DiskLruCache/issues/2)
   */
  @Test
  @Throws(Exception::class)
  fun aggressiveClearingHandlesWrite() {
    fileSystem.deleteRecursively(tempDir.root)
    set("a", "a", "a")
    assertValue("a", "a", "a")
  }

  /** @see [Issue .2](https://github.com/JakeWharton/DiskLruCache/issues/2)
   */
  @Test
  @Throws(Exception::class)
  fun aggressiveClearingHandlesEdit() {
    set("a", "a", "a")
    val a = cache!!["a"]!!.edit()
    fileSystem.deleteRecursively(tempDir.root)
    setString(a, 1, "a2")
    a!!.commit()
  }

  @Test
  @Throws(Exception::class)
  fun removeHandlesMissingFile() {
    set("a", "a", "a")
    getCleanFile("a", 0).delete()
    cache!!.remove("a")
  }

  /** @see [Issue .2](https://github.com/JakeWharton/DiskLruCache/issues/2)
   */
  @Test
  @Throws(Exception::class)
  fun aggressiveClearingHandlesPartialEdit() {
    set("a", "a", "a")
    set("b", "b", "b")
    val a = cache!!["a"]!!.edit()
    setString(a, 0, "a1")
    fileSystem.deleteRecursively(tempDir.root)
    setString(a, 1, "a2")
    a!!.commit()
    Truth.assertThat(cache!!["a"]).isNull()
  }

  /** @see [Issue .2](https://github.com/JakeWharton/DiskLruCache/issues/2)
   */
  @Test
  @Throws(Exception::class)
  fun aggressiveClearingHandlesRead() {
    fileSystem.deleteRecursively(tempDir.root)
    Truth.assertThat(cache!!["a"]).isNull()
  }

  /**
   * We had a long-lived bug where [DiskLruCache.trimToSize] could infinite loop if entries
   * being edited required deletion for the operation to complete.
   */
  @Test
  @Throws(Exception::class)
  fun trimToSizeWithActiveEdit() {
    set("a", "a1234", "a1234")
    val a = cache!!.edit("a")
    setString(a, 0, "a123")
    cache!!.setMaxSize(8) // Smaller than the sum of active edits!
    cache!!.flush() // Force trimToSize().
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    Truth.assertThat(cache!!["a"]).isNull()

    // After the edit is completed, its entry is still gone.
    setString(a, 1, "a1")
    a!!.commit()
    assertAbsent("a")
    Truth.assertThat(cache!!.size()).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun evictAll() {
    set("a", "a", "a")
    set("b", "b", "b")
    cache!!.evictAll()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    assertAbsent("a")
    assertAbsent("b")
  }

  @Test
  @Throws(Exception::class)
  fun evictAllWithPartialCreate() {
    val a = cache!!.edit("a")
    setString(a, 0, "a1")
    setString(a, 1, "a2")
    cache!!.evictAll()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    a!!.commit()
    assertAbsent("a")
  }

  @Test
  @Throws(Exception::class)
  fun evictAllWithPartialEditDoesNotStoreAValue() {
    set("a", "a", "a")
    val a = cache!!.edit("a")
    setString(a, 0, "a1")
    setString(a, 1, "a2")
    cache!!.evictAll()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    a!!.commit()
    assertAbsent("a")
  }

  @Test
  @Throws(Exception::class)
  fun evictAllDoesntInterruptPartialRead() {
    set("a", "a", "a")
    val a = cache!!["a"]
    assertSnapshotValue(a, 0, "a")
    cache!!.evictAll()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    assertAbsent("a")
    assertSnapshotValue(a, 1, "a")
    a!!.close()
  }

  @Test
  @Throws(Exception::class)
  fun editSnapshotAfterEvictAllReturnsNullDueToStaleValue() {
    set("a", "a", "a")
    val a = cache!!["a"]
    cache!!.evictAll()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    assertAbsent("a")
    Truth.assertThat(a!!.edit()).isNull()
    a.close()
  }

  @Test
  @Throws(Exception::class)
  operator fun iterator() {
    set("a", "a1", "a2")
    set("b", "b1", "b2")
    set("c", "c1", "c2")
    val iterator = cache!!.snapshots()
    Truth.assertThat(iterator.hasNext()).isTrue()
    val a = iterator.next()
    Truth.assertThat(a.key()).isEqualTo("a")
    assertSnapshotValue(a, 0, "a1")
    assertSnapshotValue(a, 1, "a2")
    a.close()
    Truth.assertThat(iterator.hasNext()).isTrue()
    val b = iterator.next()
    Truth.assertThat(b.key()).isEqualTo("b")
    assertSnapshotValue(b, 0, "b1")
    assertSnapshotValue(b, 1, "b2")
    b.close()
    Truth.assertThat(iterator.hasNext()).isTrue()
    val c = iterator.next()
    Truth.assertThat(c.key()).isEqualTo("c")
    assertSnapshotValue(c, 0, "c1")
    assertSnapshotValue(c, 1, "c2")
    c.close()
    Truth.assertThat(iterator.hasNext()).isFalse()
    try {
      iterator.next()
      Assert.fail()
    } catch (expected: NoSuchElementException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun iteratorElementsAddedDuringIterationAreOmitted() {
    set("a", "a1", "a2")
    set("b", "b1", "b2")
    val iterator = cache!!.snapshots()
    val a = iterator.next()
    Truth.assertThat(a.key()).isEqualTo("a")
    a.close()
    set("c", "c1", "c2")
    val b = iterator.next()
    Truth.assertThat(b.key()).isEqualTo("b")
    b.close()
    Truth.assertThat(iterator.hasNext()).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun iteratorElementsUpdatedDuringIterationAreUpdated() {
    set("a", "a1", "a2")
    set("b", "b1", "b2")
    val iterator = cache!!.snapshots()
    val a = iterator.next()
    Truth.assertThat(a.key()).isEqualTo("a")
    a.close()
    set("b", "b3", "b4")
    val b = iterator.next()
    Truth.assertThat(b.key()).isEqualTo("b")
    assertSnapshotValue(b, 0, "b3")
    assertSnapshotValue(b, 1, "b4")
    b.close()
  }

  @Test
  @Throws(Exception::class)
  fun iteratorElementsRemovedDuringIterationAreOmitted() {
    set("a", "a1", "a2")
    set("b", "b1", "b2")
    val iterator = cache!!.snapshots()
    cache!!.remove("b")
    val a = iterator.next()
    Truth.assertThat(a.key()).isEqualTo("a")
    a.close()
    Truth.assertThat(iterator.hasNext()).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun iteratorRemove() {
    set("a", "a1", "a2")
    val iterator = cache!!.snapshots()
    val a = iterator.next()
    a.close()
    iterator.remove()
    Truth.assertThat(cache!!["a"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun iteratorRemoveBeforeNext() {
    set("a", "a1", "a2")
    val iterator = cache!!.snapshots()
    try {
      iterator.remove()
      Assert.fail()
    } catch (expected: IllegalStateException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun iteratorRemoveOncePerCallToNext() {
    set("a", "a1", "a2")
    val iterator = cache!!.snapshots()
    val a = iterator.next()
    iterator.remove()
    a.close()
    try {
      iterator.remove()
      Assert.fail()
    } catch (expected: IllegalStateException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun cacheClosedTruncatesIterator() {
    set("a", "a1", "a2")
    val iterator = cache!!.snapshots()
    cache!!.close()
    Truth.assertThat(iterator.hasNext()).isFalse()
  }

  // Create an uninitialized cache.
  @Throws(Exception::class)
  @Test
  fun isClosedUninitializedCache() {
    // Create an uninitialized cache.
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, Int.MAX_VALUE.toLong(), executor)
    toClose.add(cache)
    Truth.assertThat(cache!!.isClosed).isFalse()
    cache!!.close()
    Truth.assertThat(cache!!.isClosed).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun journalWriteFailsDuringEdit() {
    set("a", "a", "a")
    set("b", "b", "b")

    // We can't begin the edit if writing 'DIRTY' fails.
    fileSystem.setFaultyWrite(journalFile!!, true)
    Truth.assertThat(cache!!.edit("c")).isNull()

    // Once the journal has a failure, subsequent writes aren't permitted.
    fileSystem.setFaultyWrite(journalFile!!, false)
    Truth.assertThat(cache!!.edit("d")).isNull()

    // Confirm that the fault didn't corrupt entries stored before the fault was introduced.
    cache!!.close()
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, Int.MAX_VALUE.toLong(), executor)
    assertValue("a", "a", "a")
    assertValue("b", "b", "b")
    assertAbsent("c")
    assertAbsent("d")
  }

  /**
   * We had a bug where the cache was left in an inconsistent state after a journal write failed.
   * https://github.com/square/okhttp/issues/1211
   */
  @Test
  @Throws(Exception::class)
  fun journalWriteFailsDuringEditorCommit() {
    set("a", "a", "a")
    set("b", "b", "b")

    // Create an entry that fails to write to the journal during commit.
    val editor = cache!!.edit("c")
    setString(editor, 0, "c")
    setString(editor, 1, "c")
    fileSystem.setFaultyWrite(journalFile!!, true)
    editor!!.commit()

    // Once the journal has a failure, subsequent writes aren't permitted.
    fileSystem.setFaultyWrite(journalFile!!, false)
    Truth.assertThat(cache!!.edit("d")).isNull()

    // Confirm that the fault didn't corrupt entries stored before the fault was introduced.
    cache!!.close()
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, Int.MAX_VALUE.toLong(), executor)
    assertValue("a", "a", "a")
    assertValue("b", "b", "b")
    assertAbsent("c")
    assertAbsent("d")
  }

  @Test
  @Throws(Exception::class)
  fun journalWriteFailsDuringEditorAbort() {
    set("a", "a", "a")
    set("b", "b", "b")

    // Create an entry that fails to write to the journal during abort.
    val editor = cache!!.edit("c")
    setString(editor, 0, "c")
    setString(editor, 1, "c")
    fileSystem.setFaultyWrite(journalFile!!, true)
    editor!!.abort()

    // Once the journal has a failure, subsequent writes aren't permitted.
    fileSystem.setFaultyWrite(journalFile!!, false)
    Truth.assertThat(cache!!.edit("d")).isNull()

    // Confirm that the fault didn't corrupt entries stored before the fault was introduced.
    cache!!.close()
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, Int.MAX_VALUE.toLong(), executor)
    assertValue("a", "a", "a")
    assertValue("b", "b", "b")
    assertAbsent("c")
    assertAbsent("d")
  }

  @Test
  @Throws(Exception::class)
  fun journalWriteFailsDuringRemove() {
    set("a", "a", "a")
    set("b", "b", "b")

    // Remove, but the journal write will fail.
    fileSystem.setFaultyWrite(journalFile!!, true)
    Truth.assertThat(cache!!.remove("a")).isTrue()

    // Confirm that the entry was still removed.
    fileSystem.setFaultyWrite(journalFile!!, false)
    cache!!.close()
    cache = DiskLruCache(fileSystem, cacheDir!!, appVersion, 2, Int.MAX_VALUE.toLong(), executor)
    assertAbsent("a")
    assertValue("b", "b", "b")
  }

  @Test
  @Throws(Exception::class)
  fun cleanupTrimFailurePreventsNewEditors() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // Confirm that edits are prevented after a cache trim failure.
    Truth.assertThat(cache!!.edit("a")).isNull()
    Truth.assertThat(cache!!.edit("b")).isNull()
    Truth.assertThat(cache!!.edit("c")).isNull()

    // Allow the test to clean up.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
  }

  @Test
  @Throws(Exception::class)
  fun cleanupTrimFailureRetriedOnEditors() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // An edit should now add a job to clean up if the most recent trim failed.
    Truth.assertThat(cache!!.edit("b")).isNull()
    executor.jobs.pop().run()

    // Confirm a successful cache trim now allows edits.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
    Truth.assertThat(cache!!.edit("c")).isNull()
    executor.jobs.pop().run()
    set("c", "cc", "cc")
    assertValue("c", "cc", "cc")
  }

  @Test
  @Throws(Exception::class)
  fun cleanupTrimFailureWithInFlightEditor() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aaa")
    set("b", "bb", "bb")
    val inFlightEditor = cache!!.edit("c")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // The in-flight editor can still write after a trim failure.
    setString(inFlightEditor, 0, "cc")
    setString(inFlightEditor, 1, "cc")
    inFlightEditor!!.commit()

    // Confirm the committed values are present after a successful cache trim.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
    executor.jobs.pop().run()
    assertValue("c", "cc", "cc")
  }

  @Test
  @Throws(Exception::class)
  fun cleanupTrimFailureAllowsSnapshotReads() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // Confirm we still allow snapshot reads after a trim failure.
    assertValue("a", "aa", "aa")
    assertValue("b", "bb", "bbb")

    // Allow the test to clean up.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
  }

  @Test
  @Throws(Exception::class)
  fun cleanupTrimFailurePreventsSnapshotWrites() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // Confirm snapshot writes are prevented after a trim failure.
    val snapshot1 = cache!!["a"]
    Truth.assertThat(snapshot1!!.edit()).isNull()
    snapshot1.close()
    val snapshot2 = cache!!["b"]
    Truth.assertThat(snapshot2!!.edit()).isNull()
    snapshot2.close()

    // Allow the test to clean up.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
  }

  @Test
  @Throws(Exception::class)
  fun evictAllAfterCleanupTrimFailure() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // Confirm we prevent edits after a trim failure.
    Truth.assertThat(cache!!.edit("c")).isNull()

    // A successful eviction should allow new writes.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
    cache!!.evictAll()
    set("c", "cc", "cc")
    assertValue("c", "cc", "cc")
  }

  @Test
  @Throws(Exception::class)
  fun manualRemovalAfterCleanupTrimFailure() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // Confirm we prevent edits after a trim failure.
    Truth.assertThat(cache!!.edit("c")).isNull()

    // A successful removal which trims the cache should allow new writes.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
    cache!!.remove("a")
    set("c", "cc", "cc")
    assertValue("c", "cc", "cc")
  }

  @Test
  @Throws(Exception::class)
  fun flushingAfterCleanupTrimFailure() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim job to fail.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), true)
    executor.jobs.pop().run()

    // Confirm we prevent edits after a trim failure.
    Truth.assertThat(cache!!.edit("c")).isNull()

    // A successful flush trims the cache and should allow new writes.
    fileSystem.setFaultyDelete(File(cacheDir, "a.0"), false)
    cache!!.flush()
    set("c", "cc", "cc")
    assertValue("c", "cc", "cc")
  }

  @Test
  @Throws(Exception::class)
  fun cleanupTrimFailureWithPartialSnapshot() {
    cache!!.setMaxSize(8)
    executor.jobs.pop()
    set("a", "aa", "aa")
    set("b", "bb", "bbb")

    // Cause the cache trim to fail on the second value leaving a partial snapshot.
    fileSystem.setFaultyDelete(File(cacheDir, "a.1"), true)
    executor.jobs.pop().run()

    // Confirm the partial snapshot is not returned.
    Truth.assertThat(cache!!["a"]).isNull()

    // Confirm we prevent edits after a trim failure.
    Truth.assertThat(cache!!.edit("a")).isNull()

    // Confirm the partial snapshot is not returned after a successful trim.
    fileSystem.setFaultyDelete(File(cacheDir, "a.1"), false)
    executor.jobs.pop().run()
    Truth.assertThat(cache!!["a"]).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun noSizeCorruptionAfterCreatorDetached() {
    // Create an editor for k1. Detach it by clearing the cache.
    val editor = cache!!.edit("k1")
    setString(editor, 0, "a")
    setString(editor, 1, "a")
    cache!!.evictAll()

    // Create a new value in its place.
    set("k1", "bb", "bb")
    Truth.assertThat(cache!!.size()).isEqualTo(4)

    // Committing the detached editor should not change the cache's size.
    editor!!.commit()
    Truth.assertThat(cache!!.size()).isEqualTo(4)
    assertValue("k1", "bb", "bb")
  }

  @Test
  @Throws(Exception::class)
  fun noSizeCorruptionAfterEditorDetached() {
    set("k1", "a", "a")

    // Create an editor for k1. Detach it by clearing the cache.
    val editor = cache!!.edit("k1")
    setString(editor, 0, "bb")
    setString(editor, 1, "bb")
    cache!!.evictAll()

    // Create a new value in its place.
    set("k1", "ccc", "ccc")
    Truth.assertThat(cache!!.size()).isEqualTo(6)

    // Committing the detached editor should not change the cache's size.
    editor!!.commit()
    Truth.assertThat(cache!!.size()).isEqualTo(6)
    assertValue("k1", "ccc", "ccc")
  }

  @Test
  @Throws(Exception::class)
  fun noNewSourceAfterEditorDetached() {
    set("k1", "a", "a")
    val editor = cache!!.edit("k1")
    cache!!.evictAll()
    Truth.assertThat(editor!!.newSource(0)).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun editsDiscardedAfterEditorDetached() {
    set("k1", "a", "a")

    // Create an editor, then detach it.
    val editor = cache!!.edit("k1")
    val sink = editor!!.newSink(0).buffer()
    cache!!.evictAll()

    // Create another value in its place.
    set("k1", "ccc", "ccc")

    // Complete the original edit. It goes into a black hole.
    sink.writeUtf8("bb")
    sink.close()
    assertValue("k1", "ccc", "ccc")
  }

  @Test
  @Throws(Exception::class)
  fun abortAfterDetach() {
    set("k1", "a", "a")
    val editor = cache!!.edit("k1")
    cache!!.evictAll()
    editor!!.abort()
    Truth.assertThat(cache!!.size()).isEqualTo(0)
    assertAbsent("k1")
  }

  @Throws(Exception::class)
  private fun assertJournalEquals(vararg expectedBodyLines: String) {
    val expectedLines: MutableList<String> = ArrayList()
    expectedLines.add(DiskLruCache.MAGIC)
    expectedLines.add(DiskLruCache.VERSION_1)
    expectedLines.add("100")
    expectedLines.add("2")
    expectedLines.add("")
    expectedLines.addAll(listOf(*expectedBodyLines))
    Truth.assertThat(readJournalLines()).isEqualTo(expectedLines)
  }

  @Throws(Exception::class)
  private fun createJournal(vararg bodyLines: String) {
    createJournalWithHeader(DiskLruCache.MAGIC, DiskLruCache.VERSION_1, "100", "2", "", *bodyLines)
  }

  @Throws(Exception::class)
  private fun createJournalWithHeader(
      magic: String, version: String, appVersion: String,
      valueCount: String, blank: String, vararg bodyLines: String,
  ) {
    val sink = fileSystem.sink(journalFile!!).buffer()
    sink.writeUtf8("""
  $magic
  
  """.trimIndent())
    sink.writeUtf8("""
  $version
  
  """.trimIndent())
    sink.writeUtf8("""
  $appVersion
  
  """.trimIndent())
    sink.writeUtf8("""
  $valueCount
  
  """.trimIndent())
    sink.writeUtf8("""
  $blank
  
  """.trimIndent())
    for (line in bodyLines) {
      sink.writeUtf8(line)
      sink.writeUtf8("\n")
    }
    sink.close()
  }

  @Throws(Exception::class)
  private fun readJournalLines(): List<String?> {
    val result: MutableList<String?> = ArrayList()
    val source = fileSystem.source(journalFile!!).buffer()
    var line: String?
    while (source.readUtf8Line().also { line = it } != null) {
      result.add(line)
    }
    source.close()
    return result
  }

  private fun getCleanFile(key: String, index: Int): File {
    return File(cacheDir, "$key.$index")
  }

  private fun getDirtyFile(key: String, index: Int): File {
    return File(cacheDir, "$key.$index.tmp")
  }

  @Throws(Exception::class)
  private fun readFile(file: File): String {
    val source = fileSystem.source(file).buffer()
    val result = source.readUtf8()
    source.close()
    return result
  }

  @Throws(Exception::class)
  fun writeFile(file: File?, content: String?) {
    val sink = fileSystem.sink(file!!).buffer()
    sink.writeUtf8(content!!)
    sink.close()
  }

  @Throws(Exception::class)
  private fun generateSomeGarbageFiles() {
    val dir1 = File(cacheDir, "dir1")
    val dir2 = File(dir1, "dir2")
    writeFile(getCleanFile("g1", 0), "A")
    writeFile(getCleanFile("g1", 1), "B")
    writeFile(getCleanFile("g2", 0), "C")
    writeFile(getCleanFile("g2", 1), "D")
    writeFile(getCleanFile("g2", 1), "D")
    writeFile(File(cacheDir, "otherFile0"), "E")
    writeFile(File(dir2, "otherFile1"), "F")
  }

  private fun assertGarbageFilesAllDeleted() {
    Truth.assertThat(fileSystem.exists(getCleanFile("g1", 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getCleanFile("g1", 1))).isFalse()
    Truth.assertThat(fileSystem.exists(getCleanFile("g2", 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getCleanFile("g2", 1))).isFalse()
    Truth.assertThat(fileSystem.exists(File(cacheDir, "otherFile0"))).isFalse()
    Truth.assertThat(fileSystem.exists(File(cacheDir, "dir1"))).isFalse()
  }

  @Throws(Exception::class)
  private operator fun set(key: String, value0: String, value1: String) {
    val editor = cache!!.edit(key)
    setString(editor, 0, value0)
    setString(editor, 1, value1)
    editor!!.commit()
  }

  @Throws(Exception::class)
  private fun assertAbsent(key: String) {
    val snapshot = cache!![key]
    if (snapshot != null) {
      snapshot.close()
      Assert.fail()
    }
    Truth.assertThat(fileSystem.exists(getCleanFile(key, 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getCleanFile(key, 1))).isFalse()
    Truth.assertThat(fileSystem.exists(getDirtyFile(key, 0))).isFalse()
    Truth.assertThat(fileSystem.exists(getDirtyFile(key, 1))).isFalse()
  }

  @Throws(Exception::class)
  private fun assertValue(key: String, value0: String, value1: String) {
    val snapshot = cache!![key]
    assertSnapshotValue(snapshot, 0, value0)
    assertSnapshotValue(snapshot, 1, value1)
    Truth.assertThat(fileSystem.exists(getCleanFile(key, 0))).isTrue()
    Truth.assertThat(fileSystem.exists(getCleanFile(key, 1))).isTrue()
    snapshot!!.close()
  }

  @Throws(IOException::class)
  private fun assertSnapshotValue(snapshot: DiskLruCache.Snapshot?, index: Int, value: String) {
    Truth.assertThat(sourceAsString(snapshot!!.getSource(index))).isEqualTo(value)
    Truth.assertThat(snapshot.getLength(index)).isEqualTo(value.length)
  }

  @Throws(IOException::class)
  private fun sourceAsString(source: Source?): String? {
    return source?.buffer()?.readUtf8()
  }

  @Throws(IOException::class)
  private fun copyFile(from: File?, to: File?) {
    val source = fileSystem.source(from!!)
    val sink = fileSystem.sink(to!!).buffer()
    sink.writeAll(source)
    source.close()
    sink.close()
  }

  private class TestExecutor : Executor {
    val jobs: Deque<Runnable> = ArrayDeque()
    override fun execute(command: Runnable) {
      jobs.addLast(command)
    }
  }

  companion object {
    @Throws(Exception::class)
    private fun assertInoperable(editor: DiskLruCache.Editor?) {
      try {
        setString(editor, 0, "A")
        Assert.fail()
      } catch (expected: IllegalStateException) {
      }
      try {
        editor!!.newSource(0)
        Assert.fail()
      } catch (expected: IllegalStateException) {
      }
      try {
        editor!!.newSink(0)
        Assert.fail()
      } catch (expected: IllegalStateException) {
      }
      try {
        editor!!.commit()
        Assert.fail()
      } catch (expected: IllegalStateException) {
      }
      try {
        editor!!.abort()
        Assert.fail()
      } catch (expected: IllegalStateException) {
      }
    }

    @Throws(IOException::class)
    private fun setString(editor: DiskLruCache.Editor?, index: Int, value: String?) {
      val writer = editor!!.newSink(index).buffer()
      writer.writeUtf8(value!!)
      writer.close()
    }
  }
}

private fun FaultyFileSystem.setFaultyDelete(file: File, faulty: Boolean) = setFaultyDelete(file.toOkioPath(), faulty)
private fun FaultyFileSystem.setFaultyRename(file: File, faulty: Boolean) = setFaultyRename(file.toOkioPath(), faulty)
private fun FaultyFileSystem.setFaultyWrite(file: File, faulty: Boolean) = setFaultyWrite(file.toOkioPath(), faulty)
private fun FaultyFileSystem.exists(file: File) = exists(file.toOkioPath())
private fun FaultyFileSystem.sink(file: File): Sink {
  if (!file.exists()) {
    file.parentFile.mkdirs()
    file.createNewFile()
  }
  return sink(file.toOkioPath())
}

private fun FaultyFileSystem.rename(from: File, to: File) = atomicMove(from.toOkioPath(), to.toOkioPath())
private fun FaultyFileSystem.delete(file: File) = delete(file.toOkioPath())
private fun FaultyFileSystem.deleteRecursively(file: File) = deleteRecursively(file.toOkioPath())
private fun FaultyFileSystem.source(file: File) = source(file.toOkioPath())
