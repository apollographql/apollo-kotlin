package com.apollographql.apollo.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CacheableTask
abstract class ApolloGenerateKspProcessorTask : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schema: RegularFileProperty

  @get:Input
  abstract val serviceName: Property<String>

  @get:Input
  abstract val packageName: Property<String>

  @get:OutputFile
  abstract val outputJar: RegularFileProperty

  private class Entry(
      val name: String,
      val contents: ByteArray
  )

  private fun metaInfResource(): Entry {
    return Entry(
        "META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider",
        "apollographql.generated.ApolloProcessorProvider".encodeToByteArray()
    )
  }

  private fun processorClassFile(serviceName: String, packageName: String): Entry {
    return Entry(
        "apollographql/generated/ApolloProcessorProvider.class",
        ApolloProcessorProviderDump(serviceName, packageName).dump()
    )
  }

  private fun schema(schema: File): Entry {
    return Entry(
        "schema.graphqls",
        schema.readBytes()
    )
  }
  private fun ZipOutputStream.putEntry(entry: Entry) {
    val zipEntry = ZipEntry(entry.name)
    zipEntry.size = entry.contents.size.toLong()
    putNextEntry(zipEntry)
    write(entry.contents)
  }

  @TaskAction
  fun taskAction() {
    outputJar.get().asFile.outputStream().let { ZipOutputStream(it) }.use {
      it.putEntry(metaInfResource())
      it.putEntry(processorClassFile(serviceName.get(), packageName.get()))
      it.putEntry(schema(schema.asFile.get()))
    }
  }
}
