package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.ir.IrFieldInfo
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.TypeSet
import com.apollographql.apollo3.compiler.singularize

/**
 * The central place where the names/packages of the different classes are decided and escape rules done.
 *
 * Inputs should always be GraphQL identifiers and outputs are valid Kotlin/Java identifiers.
 */
abstract class CodegenLayout(
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    private val useSemanticNaming: Boolean,
    private val useSchemaPackageNameForFragments: Boolean,
    private val nameToClassName: (String) -> String,
) {
  private val typePackageName = "$schemaPackageName.type"

  // ------------------------ FileNames ---------------------------------

  internal fun fragmentModelsFileName(name: String) = capitalizedIdentifier(name)

  // ------------------------ PackageNames ---------------------------------

  fun typePackageName() = typePackageName
  fun typeAdapterPackageName() = "$typePackageName.adapter".stripDots()

  fun operationPackageName(filePath: String) = packageNameGenerator.packageName(filePath)
  fun operationAdapterPackageName(filePath: String) = "${operationPackageName(filePath)}.adapter".stripDots()
  fun operationTestBuildersPackageName(filePath: String) = "${operationPackageName(filePath)}.test".stripDots()
  fun operationResponseFieldsPackageName(filePath: String) = "${operationPackageName(filePath)}.selections".stripDots()

  fun fragmentPackageName(filePath: String) = if (useSchemaPackageNameForFragments) {
    "$schemaPackageName.fragment"
  } else {
    "${packageNameGenerator.packageName(filePath)}.fragment"
  }

  fun fragmentAdapterPackageName(filePath: String) = "${fragmentPackageName(filePath)}.adapter".stripDots()
  fun fragmentResponseFieldsPackageName(filePath: String) = "${fragmentPackageName(filePath)}.selections".stripDots()

  private fun String.stripDots() = this.removePrefix(".").removeSuffix(".")

  // ------------------------ Names ---------------------------------

  internal fun compiledTypeName(name: String) = capitalizedIdentifier(nameToClassName(name))

  internal fun enumName(name: String) = regularIdentifier(nameToClassName(name))

  internal fun enumResponseAdapterName(name: String) = enumName(name) + "_ResponseAdapter"

  internal fun operationName(operation: IrOperation): String {
    val str = capitalizedIdentifier(operation.name)

    if (!useSemanticNaming) {
      return str
    }

    return if (str.endsWith(operation.operationType.name)) {
      str
    } else {
      "$str${operation.operationType.name}"
    }
  }

  fun operationResponseAdapterWrapperName(operation: IrOperation) = operationName(operation) + "_ResponseAdapter"
  fun operationTestBuildersWrapperName(operation: IrOperation) = operationName(operation) + "_TestBuilder"
  fun operationVariablesAdapterName(operation: IrOperation) = operationName(operation) + "_VariablesAdapter"
  fun operationSelectionsName(operation: IrOperation) = operationName(operation) + "Selections"

  internal fun fragmentName(name: String) = capitalizedIdentifier(name) + "Impl"
  internal fun fragmentResponseAdapterWrapperName(name: String) = fragmentName(name) + "_ResponseAdapter"
  internal fun fragmentVariablesAdapterName(name: String) = fragmentName(name) + "_VariablesAdapter"
  internal fun fragmentSelectionsName(name: String) = regularIdentifier(name) + "Selections"

  internal fun inputObjectName(name: String) = capitalizedIdentifier(nameToClassName(name))
  internal fun inputObjectAdapterName(name: String) = inputObjectName(name) + "_InputAdapter"

  // Variables are escaped to avoid a clash with the model name if they are capitalized
  internal fun variableName(name: String) = if (name == "__typename") name else regularIdentifier("_$name")
  internal fun propertyName(name: String) = regularIdentifier(name)

  internal fun compiledSelectionsName(name: String) = regularIdentifier("__$name")

  // ------------------------ Helpers ---------------------------------

  abstract fun escapeReservedWord(word: String): String

  protected fun regularIdentifier(name: String) = escapeReservedWord(name)

  private fun capitalizedIdentifier(name: String): String {
    return escapeReservedWord(name.capitalizeFirstLetter())
  }

  fun testBuilder(modelName: String): String {
    return "${modelName}Builder"
  }

  companion object {
    fun upperCamelCaseIgnoringNonLetters(strings: Collection<String>): String {
      return strings.map {
        it.capitalizeFirstLetter()
      }.joinToString("")
    }

    fun lowerCamelCaseIgnoringNonLetters(strings: Collection<String>): String {
      return strings.map {
        it.decapitalizeFirstLetter()
      }.joinToString("")
    }

    private fun IrType.isList(): Boolean {
      return when (this) {
        is IrListType -> true
        is IrNonNullType -> ofType.isList()
        else -> false
      }
    }

    fun modelName(info: IrFieldInfo, typeSet: TypeSet, rawTypename: String, isOther: Boolean): String {
      val responseName = if (info.type.isList()) {
        info.responseName.singularize()
      } else {
        info.responseName
      }
      val name = upperCamelCaseIgnoringNonLetters((typeSet - rawTypename).sorted() + responseName)

      return (if (isOther) "Other" else "") + name
    }

    fun modelName(info: IrFieldInfo): String {
      val responseName = if (info.type.isList()) {
        info.responseName.singularize()
      } else {
        info.responseName
      }
      return upperCamelCaseIgnoringNonLetters(setOf(responseName))
    }
  }
}
