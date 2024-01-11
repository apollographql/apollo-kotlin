package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.pqm.PersistedQueryManifest
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

/**
 * Options
 */
fun File.toCodegenSchemaOptions(): CodegenSchemaOptions = parseFromJson()
fun File.toIrOptions(): IrOptions = parseFromJson()
fun File.toOperationOutput(): OperationOutput = parseFromJson<Map<String, OperationDescriptor>>()
fun File.toCodegenOptions(): CodegenOptions = parseFromJson()

fun CodegenSchemaOptions.writeTo(file: File) = encodeToJson(file)
fun IrOptions.writeTo(file: File) = encodeToJson(file)
@JvmName("writeOperationOutputTo") // fix a clash with UsedCoordinates
fun OperationOutput.writeTo(file: File) = (this as Map<String, OperationDescriptor>).encodeToJson(file)
fun CodegenOptions.writeTo(file: File) = encodeToJson(file)

/**
 * Compiler outputs
 */
fun File.toCodegenSchema(): CodegenSchema = parseFromJson()
fun File.toIrOperations(): IrOperations = parseFromJson<DefaultIrOperations>()
fun File.toIrSchema(): IrSchema = parseFromJson<DefaultIrSchema>()
fun File.toPersistedQueryManifest(): PersistedQueryManifest = parseFromJson()
fun File.toCodegenMetadata(): CodegenMetadata = parseFromJson()

fun CodegenSchema.writeTo(file: File) = encodeToJson(file)
fun IrOperations.writeTo(file: File) = (this as DefaultIrOperations).encodeToJson(file)
fun IrSchema.writeTo(file: File) = (this as DefaultIrSchema).encodeToJson(file)
fun PersistedQueryManifest.writeTo(file: File) = encodeToJson(file)
fun CodegenMetadata.writeTo(file: File) = encodeToJson(file)
