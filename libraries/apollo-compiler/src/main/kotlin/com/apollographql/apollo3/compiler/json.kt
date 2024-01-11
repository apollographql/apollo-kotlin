@file:JvmName("FileUtils")

package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloInternal
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
 * Reading options
 */
@ApolloInternal // XXX: make internal
fun File.toCodegenSchemaOptions(): CodegenSchemaOptions = parseFromJson()
internal fun File.toIrOptions(): IrOptions = parseFromJson()
internal fun File.toCodegenOptions(): CodegenOptions = parseFromJson()

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
internal fun File.toCodegenSchema(): CodegenSchema = parseFromJson()
internal fun File.toIrOperations(): IrOperations = parseFromJson<DefaultIrOperations>()
internal fun File.toIrSchema(): IrSchema = parseFromJson<DefaultIrSchema>()
@ApolloInternal // XXX: make internal
fun File.toCodegenMetadata(): CodegenMetadata = parseFromJson()
// Public on purpose
@JvmName("readOperationOutput")
fun File.toOperationOutput(): OperationOutput = parseFromJson<Map<String, OperationDescriptor>>()
// Public on purpose
@JvmName("readPersistedQueryManifest")
fun File.toPersistedQueryManifest(): PersistedQueryManifest = parseFromJson()

/**
 * Writing compiler outputs
 */
internal fun CodegenSchema.writeTo(file: File) = encodeToJson(file)
internal fun IrOperations.writeTo(file: File) = (this as DefaultIrOperations).encodeToJson(file)
internal fun IrSchema.writeTo(file: File) = (this as DefaultIrSchema).encodeToJson(file)
internal fun CodegenMetadata.writeTo(file: File) = encodeToJson(file)
internal fun PersistedQueryManifest.writeTo(file: File) = encodeToJson(file)
internal fun OperationOutput.writeTo(file: File) = this.encodeToJson(file)
