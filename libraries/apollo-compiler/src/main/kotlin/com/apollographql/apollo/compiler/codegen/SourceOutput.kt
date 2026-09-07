package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.writeTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class SourceOutput(
    val files: List<SourceFile>,
    val codegenMetadata: CodegenMetadata
) {
  operator fun plus(other: SourceOutput): SourceOutput{
    return SourceOutput(
        files + other.files,
        codegenMetadata + other.codegenMetadata
    )
  }
}

infix fun SourceOutput?.plus(other: SourceOutput): SourceOutput {
  if (this == null) {
    return other
  }

  return this + other
}

fun SourceOutput.writeTo(directory: File?, deleteDirectoryFirst: Boolean, codegenSymbolsFile: File?) {
  writeTo(directory, deleteDirectoryFirst, codegenSymbolsFile, 1)
}

internal fun SourceOutput.writeTo(
    directory: File?,
    deleteDirectoryFirst: Boolean,
    codegenSymbolsFile: File?,
    parallelism: Int,
) {
  if (directory == null) return
  if (deleteDirectoryFirst) directory.deleteRecursively()

  if (parallelism <= 1 || files.size < 16) {
    files.forEach { file ->
      file.outputFile(directory).outputStream().use { file.writeTo(it) }
    }
  } else {
    val executor = Executors.newFixedThreadPool(minOf(parallelism, files.size))
    val pending = ArrayDeque<Pair<SourceFile, Future<ByteArrayOutputStream>>>()
    val iterator = files.iterator()
    fun submitNext() {
      if (!iterator.hasNext()) return
      val file = iterator.next()
      pending.addLast(file to executor.submit<ByteArrayOutputStream> {
        ByteArrayOutputStream().apply { file.writeTo(this) }
      })
    }
    var interrupted = false
    try {
      repeat(parallelism * 2) { submitNext() }
      while (pending.isNotEmpty()) {
        val (file, future) = pending.removeFirst()
        val buffer = try {
          future.get()
        } catch (e: ExecutionException) {
          throw e.cause ?: e
        }
        file.outputFile(directory).outputStream().use { buffer.writeTo(it) }
        submitNext()
      }
    } catch (e: InterruptedException) {
      interrupted = true
      throw e
    } finally {
      executor.shutdownNow()
      while (!executor.isTerminated) {
        try {
          executor.awaitTermination(1, TimeUnit.DAYS)
        } catch (_: InterruptedException) {
          interrupted = true
        }
      }
      if (interrupted) Thread.currentThread().interrupt()
    }
  }
  if (codegenSymbolsFile != null) codegenMetadata.writeTo(codegenSymbolsFile)
}

private fun SourceFile.outputFile(directory: File): File {
  return packageName.split(".").plus(name).fold(directory) { acc, item -> acc.resolve(item) }.apply {
    parentFile.mkdirs()
  }
}

interface SourceFile {
  val packageName: String
  val name: String
  fun writeTo(outputStream: OutputStream)
}

