@file:Suppress("DEPRECATION")

package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.PackageNameGenerator
import com.apollographql.apollo.compiler.allTypes
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.decapitalizeFirstLetter
import com.apollographql.apollo.compiler.defaultDecapitalizeFields
import com.apollographql.apollo.compiler.defaultGeneratedSchemaName
import com.apollographql.apollo.compiler.defaultUseSemanticNaming
import com.apollographql.apollo.compiler.internal.singularize
import com.apollographql.apollo.compiler.ir.IrFieldInfo
import com.apollographql.apollo.compiler.ir.IrListType
import com.apollographql.apollo.compiler.ir.IrOperation
import com.apollographql.apollo.compiler.ir.IrType
import com.apollographql.apollo.compiler.ir.TypeSet
import com.apollographql.apollo.compiler.maybeAddSuffix
import com.apollographql.apollo.compiler.uniqueName
import com.apollographql.apollo.compiler.upperCamelCaseIgnoringNonLetters
import com.apollographql.apollo.compiler.withUnderscorePrefix

/**
 * The central place where the names/packages of the different classes are decided and escape rules done.
 *
 * Inputs should always be GraphQL identifiers and outputs are valid Kotlin/Java identifiers.
 */
internal class LayoutImpl(
    codegenSchema: CodegenSchema,
    private val packageNameGenerator: PackageNameGenerator,
    useSemanticNaming: Boolean?,
    decapitalizeFields: Boolean?,
    generatedSchemaName: String?
) : SchemaAndOperationsLayout {
  private val schemaPackageName = executableDocumentPackageName(codegenSchema.normalizedPath)
  private val useSemanticNaming: Boolean = useSemanticNaming ?: defaultUseSemanticNaming
  private val decapitalizeFields: Boolean = decapitalizeFields ?: defaultDecapitalizeFields
  private val generatedSchemaName: String = generatedSchemaName ?: defaultGeneratedSchemaName

  private val schemaTypeToClassName: Map<String, String> = mutableMapOf<String, String>().apply {
    val usedNames = mutableSetOf<String>()
    val allTypes = codegenSchema.allTypes()

    /**
     * Make it possible to support several types with different cases. Example:
     *
     * type URL @targetName(newName: "Url1")
     * type Url
     * type url
     *
     * Because we capitalize the first letter, we need to escape the name because else `Url` and `url` clash
     */
    // 1. Compute a unique name for types without a targetName
    for (type in allTypes.filter { it.targetName == null }) {
      val uniqueName = uniqueName(type.name, usedNames)
      val className = uniqueName.capitalizeFirstLetter()

      usedNames.add(className.lowercase())
      this[type.name] = className
    }

    // 2. Use targetName verbatim for types that define it
    for (type in allTypes.filter { it.targetName != null }) {
      this[type.name] = type.targetName!!
    }
  }

  override fun schemaPackageName(): String = schemaPackageName

  override fun executableDocumentPackageName(filePath: String?): String = packageNameGenerator.packageName(filePath ?: "")

  override fun schemaTypeName(schemaTypeName: String): String {
    return schemaTypeToClassName[schemaTypeName]?.let {
      className(it)
    } ?: error("unknown schema type: $schemaTypeName")
  }

  override fun schemaName(): String {
    return generatedSchemaName
  }

  override fun assertionsName(): String {
    return "Assertions"
  }

  override fun paginationName(): String {
    return "Pagination"
  }

  override fun operationName(name: String, capitalizedOperationType: String): String {
    return className(name).let {
      if (useSemanticNaming) {
        it.maybeAddSuffix(capitalizedOperationType)
      } else {
        it
      }
    }
  }

  override fun fragmentName(name: String): String {
    return className(name)
  }

  override fun className(name: String): String {
    return name.capitalizeFirstLetter()
  }

  override fun propertyName(name: String) = if (decapitalizeFields) name.decapitalizeFirstLetter() else name
}

private fun IrType.isList(): Boolean {
  return when (this) {
    is IrListType -> true
    else -> false
  }
}

/**
 * Build a name for a model
 *
 * @param info the field info, including responseName and whether this field is of list type. If the field is of
 * list type, the model name will be singularized
 * @param typeSet the different type conditions for this model
 * @param rawTypename the type of the field. Because it is always satisfied, it is not included in the model name
 * @param isOther whether this is a fallback type
 */
internal fun modelName(info: IrFieldInfo, typeSet: TypeSet, rawTypename: String, isOther: Boolean): String {
  val responseName = if (info.type.isList()) {
    info.responseName.singularize()
  } else {
    info.responseName
  }
  val name = upperCamelCaseIgnoringNonLetters((typeSet - rawTypename).sorted() + responseName)

  return (if (isOther) "Other" else "") + name
}

/**
 * Build a simple model name. See also [modelName] for a more complex version that can use typeSets and fallback types
 */
internal fun modelName(info: IrFieldInfo): String {
  val responseName = if (info.type.isList()) {
    info.responseName.singularize()
  } else {
    info.responseName
  }
  return upperCamelCaseIgnoringNonLetters(setOf(responseName))
}


internal fun SchemaLayout.typePackageName() = "${schemaPackageName()}.type"
internal fun SchemaLayout.typeBuilderPackageName() = "${schemaPackageName()}.type.builder"
internal fun SchemaLayout.typeAdapterPackageName() = "${schemaPackageName()}.type.adapter"
internal fun SchemaLayout.typeUtilPackageName() = "${schemaPackageName()}.type.util"

internal fun SchemaLayout.paginationPackageName() = "${schemaPackageName()}.pagination"
internal fun SchemaLayout.schemaSubPackageName() = "${schemaPackageName()}.schema"

internal fun SchemaLayout.javaOptionalAdapterClassName() = "OptionalAdapter"
internal fun SchemaLayout.javaOptionalAdaptersClassName() = "OptionalAdapters"

internal fun OperationsLayout.operationAdapterPackageName(filePath: String) = "${executableDocumentPackageName(filePath)}.adapter"
internal fun OperationsLayout.operationResponseFieldsPackageName(filePath: String) = "${executableDocumentPackageName(filePath)}.selections"

internal fun OperationsLayout.fragmentPackageName(filePath: String) = "${executableDocumentPackageName(filePath)}.fragment"
internal fun OperationsLayout.fragmentAdapterPackageName(filePath: String) = "${executableDocumentPackageName(filePath)}.fragment.adapter"
internal fun OperationsLayout.fragmentResponseFieldsPackageName(filePath: String) = "${executableDocumentPackageName(filePath)}.fragment.selections"

internal fun OperationsLayout.operationName(operation: IrOperation) = operationName(operation.name, operation.operationType.name)

internal fun String.responseAdapter(): String = "${this}_ResponseAdapter"
internal fun String.inputAdapter(): String = "${this}_InputAdapter"
internal fun String.variablesAdapter(): String = "${this}_VariablesAdapter"
internal fun String.impl(): String = "${this}Impl"
internal fun String.selections(): String = "${this}Selections"
/**
 * when used in function bodies, prefixing with '_' prevents clashing with parent classes
 */
internal fun String.variableName(): String = this.withUnderscorePrefix()

fun SchemaAndOperationsLayout(
    codegenSchema: CodegenSchema,
    packageName: String?,
    rootPackageName: String?,
    useSemanticNaming: Boolean?,
    decapitalizeFields: Boolean?,
    generatedSchemaName: String?,
): SchemaAndOperationsLayout {
  val packageNameGenerator = when {
    packageName != null -> PackageNameGenerator.Flat(packageName)
    rootPackageName != null -> PackageNameGenerator.NormalizedPathAware(rootPackageName)
    else -> error("One of packageName or rootPackageName is required")
  }
  return LayoutImpl(codegenSchema, packageNameGenerator, useSemanticNaming, decapitalizeFields, generatedSchemaName)
}

@ApolloInternal
fun SchemaAndOperationsLayout(
    codegenSchema: CodegenSchema,
    packageNameGenerator: PackageNameGenerator,
    useSemanticNaming: Boolean?,
    decapitalizeFields: Boolean?,
    generatedSchemaName: String?,
): SchemaAndOperationsLayout {
  return LayoutImpl(codegenSchema, packageNameGenerator, useSemanticNaming, decapitalizeFields, generatedSchemaName )
}

