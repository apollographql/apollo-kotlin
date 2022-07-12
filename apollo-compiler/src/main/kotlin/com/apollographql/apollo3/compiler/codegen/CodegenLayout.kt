package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.ir.Ir
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.ir.IrFieldInfo
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.IrSchemaType
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.TypeSet
import com.apollographql.apollo3.compiler.singularize

/**
 * The central place where the names/packages of the different classes are decided and escape rules done.
 *
 * Inputs should always be GraphQL identifiers and outputs are valid Kotlin/Java identifiers.
 */
internal abstract class CodegenLayout(
   ir: Ir,
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    private val useSemanticNaming: Boolean,
    private val useSchemaPackageNameForFragments: Boolean,
) {
  private val schemaTypeToClassName: Map<IrSchemaType, String> = computeClassNames()

  private fun computeClassNames(): Map<IrSchemaType, String> {
    val schemaTypeToClassName = mutableMapOf<IrSchemaType, String>()
    val allTypes: List<IrSchemaType> = ir.customScalars + ir.objects + ir.enums + ir.interfaces + ir.inputObjects + ir.unions
    val usedNames = mutableSetOf<String>()
    for (type in allTypes) {
      val targetName = type.targetName
      val className = if (targetName != null) {
        // If a targetName was specified, honor it verbatim
        targetName
      } else {
        val uniqueName = uniqueName(type.name, usedNames)
        // Capitalize all types except enums
        if (type is IrEnum) {
          regularIdentifier(uniqueName)
        } else {
          capitalizedIdentifier(uniqueName)
        }
      }
      schemaTypeToClassName[type] = className
    }
    return schemaTypeToClassName
  }

  /**
   * On case-insensitive filesystems, we need to make sure two schema types with
   * different cases like 'Url' and 'URL' are not generated or their files will
   * overwrite each other.
   *
   * For Kotlin, we _could_ just change the file name (and not the class name) but
   * that only postpones the issue to later on when .class files are generated.
   *
   * In order to get predictable results independently of the system, we make the
   * case-insensitive checks no matter the actual filesystem.
   */
  private fun uniqueName(name: String, usedNames: MutableSet<String>): String {
    var i = 1
    var uniqueName = name
    while (uniqueName.lowercase() in usedNames) {
      uniqueName = "${name}$i"
      i++
    }
    usedNames.add(uniqueName.lowercase())
    return uniqueName
  }

  private fun className(schemaType: IrSchemaType) = schemaTypeToClassName[schemaType] ?: error("unknown schema type: $schemaType")

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

  internal fun compiledTypeName(schemaType: IrSchemaType) = className(schemaType)

  internal fun enumName(enum: IrEnum) = className(enum)

  internal fun enumResponseAdapterName(enum: IrEnum) = enumName(enum) + "_ResponseAdapter"

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

  internal fun operationResponseAdapterWrapperName(operation: IrOperation) = operationName(operation) + "_ResponseAdapter"
  internal fun operationTestBuildersWrapperName(operation: IrOperation) = operationName(operation) + "_TestBuilder"
  internal fun operationVariablesAdapterName(operation: IrOperation) = operationName(operation) + "_VariablesAdapter"
  internal fun operationSelectionsName(operation: IrOperation) = operationName(operation) + "Selections"

  internal fun fragmentName(name: String) = capitalizedIdentifier(name) + "Impl"
  internal fun fragmentResponseAdapterWrapperName(name: String) = fragmentName(name) + "_ResponseAdapter"
  internal fun fragmentVariablesAdapterName(name: String) = fragmentName(name) + "_VariablesAdapter"
  internal fun fragmentSelectionsName(name: String) = regularIdentifier(name) + "Selections"

  internal fun inputObjectName(inputObject: IrInputObject) = className(inputObject)
  internal fun inputObjectAdapterName(inputObject: IrInputObject) = inputObjectName(inputObject) + "_InputAdapter"

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

    internal fun modelName(info: IrFieldInfo, typeSet: TypeSet, rawTypename: String, isOther: Boolean): String {
      val responseName = if (info.type.isList()) {
        info.responseName.singularize()
      } else {
        info.responseName
      }
      val name = upperCamelCaseIgnoringNonLetters((typeSet - rawTypename).sorted() + responseName)

      return (if (isOther) "Other" else "") + name
    }

    internal fun modelName(info: IrFieldInfo): String {
      val responseName = if (info.type.isList()) {
        info.responseName.singularize()
      } else {
        info.responseName
      }
      return upperCamelCaseIgnoringNonLetters(setOf(responseName))
    }
  }
}
