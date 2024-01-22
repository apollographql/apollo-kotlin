@file:JvmName("FileUtils")

package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.CodegenSymbols
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.toOperationOutput
import com.apollographql.apollo3.compiler.pqm.PersistedQueryManifest
import com.apollographql.apollo3.compiler.pqm.toPersistedQueryManifest
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

private inline fun <reified T> T.encodeToJson(file: File) {
  // XXX: use a streaming API when they are not experimental anymore
  file.writeText(json.encodeToString(this))
}

private inline fun <reified T> File.parseFromJson(): T {
  // XXX: use a streaming API when they are not experimental anymore
  return json.decodeFromString<T>(readText())
}

fun IrOperations.writeManifestTo(file: File?, format: String?) {
  if (file == null || format == null) {
    return
  }

  this as DefaultIrOperations

  when(format) {
    MANIFEST_NONE -> {}
    MANIFEST_PERSISTED_QUERY -> toPersistedQueryManifest().writeTo(file)
    MANIFEST_OPERATION_OUTPUT -> toOperationOutput().writeTo(file)
    else -> error("Apollo: unknown operationManifestFormat: $format")
  }
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
 * Writing options
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
fun File.toIrOperations(): IrOperations = parseFromJson<DefaultIrOperations>()

@JvmName("readIrSchema")
fun File.toIrSchema(): IrSchema = parseFromJson<DefaultIrSchema>()

@JvmName("readCodegenSymbols")
fun File.toCodegenSymbols(): CodegenSymbols = parseFromJson<CodegenSymbols>()

@JvmName("readOperationOutput")
fun File.toOperationOutput(): OperationOutput = parseFromJson<Map<String, OperationDescriptor>>()

@JvmName("readPersistedQueryManifest")
fun File.toPersistedQueryManifest(): PersistedQueryManifest = parseFromJson()

/**
 * Writing compiler outputs
 */
@JvmName("writeCodegenSchema")
fun CodegenSchema.writeTo(file: File) = encodeToJson(file)

@JvmName("writeCodegenSchema")
fun IrOperations.writeTo(file: File) = (this as DefaultIrOperations).encodeToJson(file)

@JvmName("writeCodegenSchema")
fun IrSchema.writeTo(file: File) = (this as DefaultIrSchema).encodeToJson(file)

@JvmName("writeCodegenSymbols")
fun CodegenSymbols.writeTo(file: File) = (this as CodegenSymbols).encodeToJson(file)

@JvmName("writeCodegenSchema")
fun PersistedQueryManifest.writeTo(file: File) = encodeToJson(file)

@JvmName("writeCodegenSchema")
fun OperationOutput.writeTo(file: File) = this.encodeToJson(file)
