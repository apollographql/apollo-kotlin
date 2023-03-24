package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.CodegenType
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
internal abstract class CodegenLayout(
    allTypes: List<CodegenType>,
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    private val useSemanticNaming: Boolean,
    private val decapitalizeFields: Boolean,
) {
  private val schemaTypeToClassName: Map<String, String> = mutableMapOf<String, String>().apply {
    val usedNames = mutableSetOf<String>()

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
      val className = capitalizedIdentifier(uniqueName)

      usedNames.add(className.lowercase())
      this[type.name] = className
    }

    // 2. Use targetName verbatim for types that define it
    for (type in allTypes.filter { it.targetName != null }) {
      this[type.name] = type.targetName!!
    }
  }

  private fun className(schemaTypeName: String): String = schemaTypeToClassName[schemaTypeName]
      ?: error("unknown schema type: $schemaTypeName")

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

  fun fragmentPackageName(filePath: String) = "${packageNameGenerator.packageName(filePath)}.fragment"

  fun fragmentAdapterPackageName(filePath: String) = "${fragmentPackageName(filePath)}.adapter".stripDots()
  fun fragmentResponseFieldsPackageName(filePath: String) = "${fragmentPackageName(filePath)}.selections".stripDots()

  fun paginationPackageName() = "$schemaPackageName.pagination"

  fun schemaPackageName() = "$schemaPackageName.schema"

  private fun String.stripDots() = this.removePrefix(".").removeSuffix(".")

  // ------------------------ Names ---------------------------------

  internal fun compiledTypeName(name: String) = className(name)

  internal fun enumName(name: String) = className(name)

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

  internal fun operationResponseAdapterWrapperName(operation: IrOperation) = operationName(operation) + "_ResponseAdapter"
  internal fun operationTestBuildersWrapperName(operation: IrOperation) = operationName(operation) + "_TestBuilder"
  internal fun operationVariablesAdapterName(operation: IrOperation) = operationName(operation) + "_VariablesAdapter"
  internal fun operationSelectionsName(operation: IrOperation) = operationName(operation) + "Selections"

  internal fun paginationName() = "Pagination"

  internal fun fragmentName(name: String) = capitalizedIdentifier(name) + "Impl"
  internal fun fragmentResponseAdapterWrapperName(name: String) = fragmentName(name) + "_ResponseAdapter"
  internal fun fragmentVariablesAdapterName(name: String) = fragmentName(name) + "_VariablesAdapter"
  internal fun fragmentSelectionsName(name: String) = regularIdentifier(name) + "Selections"

  internal fun inputObjectName(name: String) = className(name)
  internal fun inputObjectAdapterName(name: String) = inputObjectName(name) + "_InputAdapter"

  // Variables are escaped to avoid a clash with the model name if they are capitalized
  internal fun variableName(name: String) = if (name == "__typename") name else regularIdentifier("_$name")
  internal fun propertyName(name: String) = regularIdentifier(name).let { if (decapitalizeFields) it.decapitalizeFirstLetter() else it }

  internal fun compiledSelectionsName(name: String) = regularIdentifier("__$name")

  // ------------------------ Helpers ---------------------------------

  abstract fun escapeReservedWord(word: String): String

  protected fun regularIdentifier(name: String) = escapeReservedWord(name)

  private fun capitalizedIdentifier(name: String): String {
    return escapeReservedWord(name.capitalizeFirstLetter())
  }

  fun builderName(name: String): String {
    return "${name.capitalizeFirstLetter()}Builder"
  }

  fun otherBuilderName(name: String): String {
    return "Other${name.capitalizeFirstLetter()}Builder"
  }

  fun mapName(name: String): String {
    return "${name.capitalizeFirstLetter()}Map"
  }

  fun otherMapName(name: String): String {
    return "Other${name.capitalizeFirstLetter()}Map"
  }

  fun buildFunName(name: String): String {
    return "build${name.capitalizeFirstLetter()}"
  }

  fun buildOtherFunName(name: String): String {
    return "buildOther${name.capitalizeFirstLetter()}"
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
    internal fun uniqueName(name: String, usedNames: Set<String>): String {
      var i = 1
      var uniqueName = name
      while (uniqueName.lowercase() in usedNames) {
        uniqueName = "${name}$i"
        i++
      }
      return uniqueName
    }
  }
}
