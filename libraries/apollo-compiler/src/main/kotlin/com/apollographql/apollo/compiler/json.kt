@file:JvmName("FileUtils")

package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.DefaultIrSchema
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.ir.IrSchema
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.pqm.PersistedQueryManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json {
  /**
   * The default class discriminator is 'type', which clashes
   * with IrField.type
   */
  classDiscriminator = "#class"
}

private val prettyPrintJson = Json { prettyPrint = true }

private inline fun <reified T> T.encodeToJson(file: File) {
  // XXX: use a streaming API when they are not experimental anymore
  file.writeText(json.encodeToString(this))
}

private inline fun <reified T> File.parseFromJson(): T {
  // XXX: use a streaming API when they are not experimental anymore
  return json.decodeFromString<T>(readText())
}

/**
 * Reading options
 */
@JvmName("readCodegenSchemaOptions")
fun File.toCodegenSchemaOptions(): CodegenSchemaOptions = parseFromJson()

@JvmName("readIrOptions")
fun File.toIrOptions(): IrOptions = parseFromJson()

@JvmName("readCodegenOptions")
fun File.toCodegenOptions(): CodegenOptions = parseFromJson()

/**
 * Writing options to files need to be public to start
 */
@JvmName("writeCodegenSchemaOptions")
fun CodegenSchemaOptions.writeTo(file: File) = encodeToJson(file)

@JvmName("writeIrOptions")
fun IrOptions.writeTo(file: File) = encodeToJson(file)

@JvmName("writeCodegenOptions")
fun CodegenOptions.writeTo(file: File) = encodeToJson(file)

/**
 * Reading compiler outputs
 */
@JvmName("readCodegenSchema")
fun File.toCodegenSchema(): CodegenSchema = parseFromJson()

@JvmName("readIrOperations")
fun File.toIrOperations(): IrOperations = parseFromJson<IrOperations>()

@JvmName("readIrSchema")
fun File.toIrSchema(): IrSchema = parseFromJson<DefaultIrSchema>()

fun File.toUsedCoordinates(): UsedCoordinates {
  return parseFromJson()
}

/**
 * A minimal class that is only used to read a version
 */
@Serializable
internal class MinimalCodegen(
    val version: String? = null,
)

@JvmName("readCodegenMetadata")
fun File.toCodegenMetadata(): CodegenMetadata {
  val json = Json {
    ignoreUnknownKeys = true
  }
  // XXX: use a streaming API when they are not experimental anymore
  val version = json.decodeFromString<MinimalCodegen>(readText()).version

  check(version == CODEGEN_METADATA_VERSION) {
    "Apollo: unsupported metadata version '$version' (expected '$CODEGEN_METADATA_VERSION')"
  }
  return parseFromJson()
}

@JvmName("readOperationOutput")
fun File.toOperationOutput(): OperationOutput = parseFromJson<Map<String, OperationDescriptor>>()

@JvmName("readPersistedQueryManifest")
fun File.toPersistedQueryManifest(): PersistedQueryManifest = parseFromJson()

/**
 * Writing compiler outputs
 */
@JvmName("writeCodegenSchema")
fun CodegenSchema.writeTo(file: File) = encodeToJson(file)

@JvmName("writeIrOperations")
fun IrOperations.writeTo(file: File) = this.encodeToJson(file)

@JvmName("writeIrSchema")
fun IrSchema.writeTo(file: File) = (this as DefaultIrSchema).encodeToJson(file)

@JvmName("writeCodegenMetadata")
fun CodegenMetadata.writeTo(file: File) = encodeToJson(file)

@JvmName("writePersistedQueryManifest")
fun PersistedQueryManifest.writeTo(file: File) = encodeToJson(file)

@JvmName("writeOperationOutput")
fun OperationOutput.writeTo(file: File) = this.encodeToJson(file)

fun UsedCoordinates.writeTo(file: File) = file.writeText(prettyPrintJson.encodeToString(this))
