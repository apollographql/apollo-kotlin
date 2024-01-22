package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.allTypes
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.internal.singularize
import com.apollographql.apollo3.compiler.ir.IrFieldInfo
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.TypeSet

/**
 * The central place where the names/packages of the different classes are decided and escape rules done.
 *
 * Inputs should always be GraphQL identifiers and outputs are valid Kotlin/Java identifiers.
 */
internal class CodegenLayout(
    codegenSchema: CodegenSchema,
    private val packageNameGenerator: PackageNameGenerator,
    private val useSemanticNaming: Boolean,
    private val decapitalizeFields: Boolean,
) {
  private val schemaPackageName = filePackageName(codegenSchema.filePath ?: "")
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

  internal fun schemaTypeName(schemaTypeName: String): String = schemaTypeToClassName[schemaTypeName]
      ?: error("unknown schema type: $schemaTypeName")

  fun basePackageName() = schemaPackageName
  fun filePackageName(filePath: String) = packageNameGenerator.packageName(filePath)

  internal fun operationName(operation: IrOperation): String {
    val str = operation.name.capitalizeFirstLetter()

    if (!useSemanticNaming) {
      return str
    }

    return if (str.endsWith(operation.operationType.name)) {
      str
    } else {
      "$str${operation.operationType.name}"
    }
  }

  internal fun propertyName(name: String) = if (decapitalizeFields) name.decapitalizeFirstLetter() else name

  companion object {
    internal fun upperCamelCaseIgnoringNonLetters(strings: Collection<String>): String {
      return strings.map {
        it.capitalizeFirstLetter()
      }.joinToString("")
    }

    internal fun lowerCamelCaseIgnoringNonLetters(strings: Collection<String>): String {
      return strings.map {
        it.decapitalizeFirstLetter()
      }.joinToString("")
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

internal fun CodegenLayout.typeBuilderPackageName() = "${basePackageName()}.type.builder"
internal fun CodegenLayout.typeAdapterPackageName() = "${basePackageName()}.type.adapter"
internal fun CodegenLayout.typeUtilPackageName() = "${basePackageName()}.type.util"

internal fun CodegenLayout.paginationPackageName() = "${basePackageName()}.pagination"
internal fun CodegenLayout.schemaPackageName() = "${basePackageName()}.schema"
internal fun CodegenLayout.executionPackageName() = "${basePackageName()}.execution"

internal fun CodegenLayout.operationAdapterPackageName(filePath: String) = "${filePackageName(filePath)}.adapter"
internal fun CodegenLayout.operationResponseFieldsPackageName(filePath: String) = "${filePackageName(filePath)}.selections"

internal fun CodegenLayout.fragmentPackageName(filePath: String) = "${filePackageName(filePath)}.fragment"
internal fun CodegenLayout.fragmentAdapterPackageName(filePath: String) = "${fragmentPackageName(filePath)}.adapter"
internal fun CodegenLayout.fragmentResponseFieldsPackageName(filePath: String) = "${fragmentPackageName(filePath)}.selections"
