package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.unified.codegen.kotlinTypeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

/**
 * The central place where the names of the different classes are decided and escape rules done
 *
 * Inputs should always be GraphQL identifiers and outputs are valid Kotlin identifiers
 */
class ClassLayout(
    private val operations: List<IrOperation>,
    private val fragments: List<IrNamedFragment>,
    private val schemaPackageName: String,
    private val rootPackageName: String,
    private val useSemanticNaming: Boolean
) {
  // ------------------------ ClassNames ---------------------------------

  fun customScalarsClassName(): ClassName {
    return ClassName(
        packageName = typePackageName(),
        customScalarsName()
    )
  }


  fun enumClassName(name: String): ClassName {
    return ClassName(
        packageName = typePackageName(),
        enumName(name)
    )
  }

  fun enumAdapterClassName(name: String): ClassName {
    return ClassName(
        packageName = typeAdapterPackageName(),
        enumResponseAdapterName(name)
    )
  }

  fun inputObjectClassName(name: String): ClassName {
    return ClassName(
        packageName = typePackageName(),
        inputObjectName(name)
    )
  }

  fun operationClassName(operation: IrOperation): ClassName {
    return ClassName(
        packageName = operationPackageName(operation.packageName),
        operationName(operation)
    )
  }
  fun operationResponseFieldsClassName(operation: IrOperation): ClassName {
    return ClassName(
        packageName = operationResponseFieldsPackageName(operation.packageName),
        operationResponseFieldsName(operation)
    )
  }

  fun operationResponseAdapterClassName(operation: IrOperation): ClassName {
    return ClassName(
        packageName = operationAdapterPackageName(operation.packageName),
        operationResponseAdapterName(operation)
    )
  }

  fun operationVariablesAdapterClassName(operation: IrOperation): ClassName {
    return ClassName(
        packageName = operationAdapterPackageName(operation.packageName),
        operationVariablesAdapterName(operation)
    )
  }

  fun fragmentResponseFieldsClassName(name: String): ClassName {
    return ClassName(
        packageName = fragmentResponseFieldsPackageName(),
        fragmentResponseFieldsName(name)
    )
  }

  fun fragmentImplementationClassName(name: String): ClassName {
    return ClassName(
        packageName = fragmentPackageName(),
        fragmentName(name)
    )
  }

  fun typeTypename(type: IrType, fieldSet: IrFieldSet? = null): TypeName {
    if (type is IrNonNullType) {
      return typeTypename(type.ofType, fieldSet).copy(nullable = false)
    }

    return when (type) {
      is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      is IrListType -> List::class.asClassName().parameterizedBy(typeTypename(type.ofType, fieldSet))
      is IrStringType -> String::class.asTypeName()
      is IrFloatType -> Double::class.asTypeName()
      is IrIntType -> Int::class.asTypeName()
      is IrBooleanType -> Boolean::class.asTypeName()
      is IrIdType -> String::class.asTypeName()
      is IrAnyType -> Any::class.asTypeName()
      is IrCustomScalarType -> type.customScalar.kotlinTypeName()
      is IrEnumType -> enumClassName(type.enum.name)
      is IrInputObjectType -> inputObjectClassName(type.inputObject().name)
      is IrCompoundType -> fieldSetClassName(fieldSet ?: error("IrField.typeName() instead"))
    }.copy(nullable = true)
  }

  fun fieldTypeName(field: IrField): TypeName {
    return typeTypename(field.type, field.typeFieldSet)
  }

  fun fieldSetClassName(fieldSet: IrFieldSet): ClassName {
    return modelPathClassName(fieldSet.fullPath)
  }

  fun fieldSetAdapterClassName(fieldSet: IrFieldSet): ClassName {
    return modelPathAdapterClassName(fieldSet.fullPath)
  }

  fun modelPathClassName(path: ModelPath): ClassName {
    with (path) {
      val (packageName, elements) = when (root) {
        is ModelPath.Root.Operation -> {
          val operation = operations.firstOrNull { it.name == root.name } ?: error("Cannot find operation ${root.name}")
          operationPackageName(operation.packageName) to listOf(operationName(operation))
        }
        is ModelPath.Root.FragmentInterface -> {
          fragmentPackageName() to emptyList()
        }
        is ModelPath.Root.FragmentImplementation -> {
          fragmentPackageName() to listOf(fragmentName(root.name))
        }
      }

      return ClassName(
          packageName = packageName,
          simpleNames = elements
      )
    }
  }

  fun modelPathAdapterClassName(path: ModelPath): ClassName {
    with (path) {
      val (packageName, elements) = when (root) {
        is ModelPath.Root.Operation -> {
          val operation = operations.firstOrNull { it.name == root.name } ?: error("Cannot find operation ${root.name}")
          operationPackageName(operation.packageName) to listOf(operationResponseAdapterName(operation))
        }
        is ModelPath.Root.FragmentInterface -> error("Fragment interfaces cannot have an adapter")
        is ModelPath.Root.FragmentImplementation -> {
          fragmentPackageName() to listOf(fragmentResponseAdapterName(root.name))
        }
      }

      return ClassName(
          packageName = packageName,
          simpleNames = elements
      )
    }
  }

  // ------------------------ MemberNames ---------------------------------

  internal fun customScalarMemberName(customScalar: IrCustomScalar) = MemberName(
      customScalarsClassName(),
      customScalarName(customScalar.name)
  )

  // ------------------------ FileNames ---------------------------------

  internal fun fragmentInterfaceFileName(name: String) = capitalizedIdentifier(name)

  // ------------------------ PackageNames ---------------------------------

  fun typePackageName() = "$schemaPackageName.type"
  fun typeAdapterPackageName() = "$schemaPackageName.type.adapter"

  fun operationPackageName(packageName: String) = "$rootPackageName.$packageName".removePrefix(".")
  fun operationAdapterPackageName(packageName: String) = "$rootPackageName.$packageName.adapter".removePrefix(".")
  fun operationResponseFieldsPackageName(packageName: String) = "$rootPackageName.$packageName.responsefields".removePrefix(".")

  fun fragmentPackageName() = "$schemaPackageName.fragment"
  fun fragmentAdapterPackageName() = "$schemaPackageName.fragment.adapter"
  fun fragmentResponseFieldsPackageName() = "$schemaPackageName.fragment.responsefields"

  // ------------------------ Names ---------------------------------

  internal fun customScalarName(name: String) = capitalizedIdentifier(name)
  internal fun enumName(name: String) = regularIdentifier(name)
  internal fun enumValueName(name: String) = upperCaseIdentifier(name)
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
  fun operationResponseAdapterName(operation: IrOperation) = operationName(operation) + "_ResponseAdapter"
  fun operationVariablesAdapterName(operation: IrOperation) = operationName(operation) + "_VariablesAdapter"
  fun operationResponseFieldsName(operation: IrOperation) = operationName(operation) + "_ResponseFields"

  internal fun fragmentName(name: String) = capitalizedIdentifier(name) + "Impl"
  internal fun fragmentResponseAdapterName(name: String) = fragmentName(name) + "_ResponseAdapter"
  internal fun fragmentVariablesAdapterName(name: String) = fragmentName(name) + "_VariablesAdapter"
  internal fun fragmentResponseFieldsName(name: String) = fragmentName(name) + "_ResponseFields"

  internal fun inputObjectName(name: String) = capitalizedIdentifier(name)
  internal fun inputObjectAdapterName(name: String) = capitalizedIdentifier(name) + "_InputAdapter"

  // variables keep the same case as their declared name
  internal fun variableName(name: String) = regularIdentifier(name)
  internal fun propertyName(name: String) = regularIdentifier(name)
  internal fun customScalarsName() = "CustomScalars"

  // ------------------------ Helpers ---------------------------------
  private fun regularIdentifier(name: String) = name.escapeKotlinReservedWord()
  private fun upperCaseIdentifier(name: String) = name.toUpperCase().escapeKotlinReservedWord()
  private fun capitalizedIdentifier(name: String): String {
    return capitalizeFirstLetter(name).escapeKotlinReservedWord()
  }

  /**
   * A variation of [String.capitalize] that skips initial underscore, especially found in introspection queries
   *
   * There can still be name clashes if a property starts with an upper case letter
   */
  private fun capitalizeFirstLetter(name: String): String {
    val builder = StringBuilder(name.length)
    var isCapitalized = false
    name.forEach {
      builder.append(if (!isCapitalized && it.isLetter()) {
        isCapitalized = true
        it.toUpperCase()
      } else {
        it
      })
    }
    return builder.toString()
  }

  private fun decapitalizeFirstLetter(name: String): String {
    val builder = StringBuilder(name.length)
    var isDecapitalized = false
    name.forEach {
      builder.append(if (!isDecapitalized && it.isLetter()) {
        isDecapitalized = true
        it.toLowerCase()
      } else {
        it
      })
    }
    return builder.toString()
  }

  private fun isFirstLetterUpperCase(name: String): Boolean {
    return name.firstOrNull { it.isLetter() }?.isUpperCase() ?: true
  }
}