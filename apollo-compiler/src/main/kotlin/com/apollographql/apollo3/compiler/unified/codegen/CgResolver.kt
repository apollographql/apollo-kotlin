package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.BooleanResponseAdapter
import com.apollographql.apollo3.api.DoubleResponseAdapter
import com.apollographql.apollo3.api.IntResponseAdapter
import com.apollographql.apollo3.api.StringResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.apollographql.apollo3.compiler.unified.ir.IrAnyType
import com.apollographql.apollo3.compiler.unified.ir.IrBooleanType
import com.apollographql.apollo3.compiler.unified.ir.IrCustomScalarType
import com.apollographql.apollo3.compiler.unified.ir.IrEnumType
import com.apollographql.apollo3.compiler.unified.ir.IrFloatType
import com.apollographql.apollo3.compiler.unified.ir.IrIdType
import com.apollographql.apollo3.compiler.unified.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.ir.IrIntType
import com.apollographql.apollo3.compiler.unified.ir.IrListType
import com.apollographql.apollo3.compiler.unified.ir.IrModelId
import com.apollographql.apollo3.compiler.unified.ir.IrModelType
import com.apollographql.apollo3.compiler.unified.ir.IrNonNullType
import com.apollographql.apollo3.compiler.unified.ir.IrStringType
import com.apollographql.apollo3.compiler.unified.ir.IrType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName


class CgResolver {
  fun resolveType(type: IrType): TypeName {
    if (type is IrNonNullType) {
      return resolveType(type.ofType).copy(nullable = false)
    }

    return when (type) {
      is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      is IrListType -> List::class.asClassName().parameterizedBy(resolveType(type.ofType))
      is IrStringType -> String::class.asTypeName()
      is IrFloatType -> Double::class.asTypeName()
      is IrIntType -> Int::class.asTypeName()
      is IrBooleanType -> Boolean::class.asTypeName()
      is IrIdType -> String::class.asTypeName()
      is IrAnyType -> Any::class.asTypeName()
      is IrCustomScalarType -> customScalars.get(type.name)
      is IrEnumType -> enums.get(type.name)
      is IrInputObjectType -> inputObjects.get(type.name)
      is IrModelType -> {
        models.get(type.id)
      }
    }?.copy(nullable = true)?: error("Cannot resolve $type")
  }

  fun adapterInitializer(type: IrType): CodeBlock {
    if (type !is IrNonNullType) {
      return when (type) {
        is IrIdType -> nullableScalarAdapter("NullableStringResponseAdapter")
        is IrBooleanType -> nullableScalarAdapter("NullableBooleanResponseAdapter")
        is IrStringType -> nullableScalarAdapter("NullableStringResponseAdapter")
        is IrIntType -> nullableScalarAdapter("NullableIntResponseAdapter")
        is IrFloatType -> nullableScalarAdapter("NullableDoubleResponseAdapter")
        is IrAnyType -> nullableScalarAdapter("NullableAnyResponseAdapter")
        else -> {
          val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
          CodeBlock.of("%L.%M()", adapterInitializer(IrNonNullType(type)), nullableFun)
        }
      }
    }
    return nonNullableAdapterInitializer(type.ofType)
  }

  private fun nonNullableAdapterInitializer(type: IrType): CodeBlock {
    return when (type) {
      is IrNonNullType -> error("")
      is IrListType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", adapterInitializer(type.ofType), listFun)
      }
      is IrBooleanType -> CodeBlock.of("%T", BooleanResponseAdapter::class)
      is IrIdType -> CodeBlock.of("%T", StringResponseAdapter::class)
      is IrStringType -> CodeBlock.of("%T", StringResponseAdapter::class)
      is IrIntType -> CodeBlock.of("%T", IntResponseAdapter::class)
      is IrFloatType -> CodeBlock.of("%T", DoubleResponseAdapter::class)
      is IrAnyType -> CodeBlock.of("%T", AnyResponseAdapter::class)
      is IrEnumType -> {
        CodeBlock.of("%T", enumAdapters.get(type.name) ?: error("Cannot find enum '$type' adapter"))
      }
      is IrInputObjectType ->{
        CodeBlock.of("%T", inputObjectAdapters.get(type.name) ?: error("Cannot find inputObject '$type' adapter")).obj(false)
      }
      is IrCustomScalarType -> {
        CodeBlock.of(
            "responseAdapterCache.responseAdapterFor<%T>(%M)",
            customScalars.get(type.name) ?: "Cannot find custom scalar '$type'",
            customScalarConsts.get(type.name)
        )
      }
      is IrModelType -> {
        CodeBlock.of("%T", modelAdapters.get(type.id) ?: error("Cannot find model '$type' adapter")).obj(false)
      }
    }
  }

  private fun nullableScalarAdapter(name: String) = CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api", name))

  private val models = mutableMapOf<IrModelId, ClassName>()
  fun registerModel(type: IrModelId, className: ClassName) {
    models.put(type, className)
  }
  fun resolveModel(type: IrModelId): ClassName {
    return models.get(type) ?: error("Cannot resolve model '$type'")
  }
  private val modelAdapters = mutableMapOf<IrModelId, ClassName>()
  fun registerModelAdapter(id: IrModelId, className: ClassName) {
    modelAdapters.put(id, className)
  }
  fun resolveModelAdapter(id: IrModelId): ClassName {
    return modelAdapters.get(id) ?: error("Cannot resolve model adapter '$id'")
  }
  private val enumAdapters = mutableMapOf<String, ClassName>()
  fun registerEnumAdapter(name: String, className: ClassName) {
    enumAdapters.put(name, className)
  }
  private val inputObjectAdapters = mutableMapOf<String, ClassName>()
  fun registerInputObjectAdapter(name: String, className: ClassName) {
    inputObjectAdapters.put(name, className)
  }
  private val operations = mutableMapOf<String, ClassName>()
  fun registerOperation(name: String, className: ClassName) {
    operations.put(name, className)
  }
  fun resolveOperation(name: String): ClassName {
    return operations.get(name) ?: error("Cannot resolve operation '$name'")
  }

  private val operationsVariablesAdapter = mutableMapOf<String, ClassName>()
  fun registerOperationVariablesAdapter(name: String, className: ClassName) {
    operationsVariablesAdapter.put(name, className)
  }
  fun resolveOperationVariablesAdapter(name: String): ClassName? {
    return operationsVariablesAdapter.get(name)
  }

  private val operationsResponseFields = mutableMapOf<String, ClassName>()
  fun registerOperationResponseFields(name: String, className: ClassName) {
    operationsResponseFields.put(name, className)
  }
  fun resolveOperationResponseFields(name: String): ClassName {
    return operationsResponseFields.get(name) ?: error("Cannot resolve operation '$name' response fields")
  }

  private val fragments = mutableMapOf<String, ClassName>()
  fun registerFragment(name: String, className: ClassName) {
    fragments.put(name, className)
  }
  fun resolveFragment(name: String): ClassName {
    return fragments.get(name) ?: error("Cannot resolve fragment '$name'")
  }

  private val fragmentsVariablesAdapter = mutableMapOf<String, ClassName>()
  fun registerFragmentVariablesAdapter(name: String, className: ClassName) {
    fragmentsVariablesAdapter.put(name, className)
  }
  fun resolveFragmentVariablesAdapter(name: String): ClassName? {
    return fragmentsVariablesAdapter.get(name)
  }

  private val fragmentsResponseFields = mutableMapOf<String, ClassName>()
  fun registerFragmentResponseFields(name: String, className: ClassName) {
    fragmentsResponseFields.put(name, className)
  }
  fun resolveFragmentResponseFields(name: String): ClassName {
    return fragmentsResponseFields.get(name) ?: error("Cannot resolve fragment '$name' response fields")
  }

  private var customScalars = mutableMapOf<String, ClassName?>()
  fun registerCustomScalar(
      name: String,
      kotlinName: String?,
  ) {
    customScalars.put(name, kotlinName?.let { ClassName.bestGuess(it) })
  }

  private var customScalarConsts = mutableMapOf<String, MemberName>()
  fun registerCustomScalarConst(
      name: String,
      memberName: MemberName,
  ) {
    customScalarConsts.put(name, memberName)
  }

  private var enums = mutableMapOf<String, ClassName>()
  fun registerEnum(
      name: String,
      className: ClassName,
  ) {
    enums.put(name, className)
  }
  fun resolveEnum(name: String): ClassName {
    return enums.get(name) ?: error("cannot resolve enum '$name'")
  }

  private var inputObjects = mutableMapOf<String, ClassName>()
  fun registerInputObject(
      name: String,
      className: ClassName,
  ) {
    inputObjects.put(name, className)
  }

  fun resolveInputObject(name: String): ClassName {
    return inputObjects.get(name) ?: error("cannot resolve input object '$name'")
  }
}