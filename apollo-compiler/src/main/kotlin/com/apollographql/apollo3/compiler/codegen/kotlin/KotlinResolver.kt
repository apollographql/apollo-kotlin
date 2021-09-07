package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.BooleanAdapter
import com.apollographql.apollo3.api.DoubleAdapter
import com.apollographql.apollo3.api.IntAdapter
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.StringAdapter
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.obj
import com.apollographql.apollo3.compiler.ir.IrAnyType
import com.apollographql.apollo3.compiler.ir.IrBooleanType
import com.apollographql.apollo3.compiler.ir.IrCustomScalarType
import com.apollographql.apollo3.compiler.ir.IrEnumType
import com.apollographql.apollo3.compiler.ir.IrFloatType
import com.apollographql.apollo3.compiler.ir.IrIdType
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrIntType
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNamedType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrStringType
import com.apollographql.apollo3.compiler.ir.IrType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName


class KotlinResolver(entries: List<ResolverEntry>, val next: KotlinResolver?) {
  fun resolve(key: ResolverKey): ClassName? = classNames[key] ?: next?.resolve(key)

  private var classNames = entries.associateBy(
      keySelector = { it.key },
      valueTransform = { it.className.toKotlinPoetClassName() }
  ).toMutableMap()

  private fun ResolverClassName.toKotlinPoetClassName() = ClassName(packageName, simpleNames)

  private fun resolve(kind: ResolverKeyKind, id: String) = resolve(ResolverKey(kind, id))
  private fun resolveAndAssert(kind: ResolverKeyKind, id: String): ClassName {
    val result = resolve(ResolverKey(kind, id))

    check(result != null) {
      "Cannot resolve $kind($id)"
    }
    return result
  }
  fun canResolveSchemaType(name: String) = resolve(ResolverKey(ResolverKeyKind.SchemaType, name)) != null

  private fun register(kind: ResolverKeyKind, id: String, className: ClassName) = classNames.put(ResolverKey(kind, id), className)

  fun resolveIrType(type: IrType): TypeName {
    if (type is IrNonNullType) {
      return resolveIrType(type.ofType).copy(nullable = false)
    }

    return when (type) {
      is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      is IrOptionalType -> Optional::class.asClassName().parameterizedBy(resolveIrType(type.ofType))
      is IrListType -> List::class.asClassName().parameterizedBy(resolveIrType(type.ofType))
      is IrStringType -> String::class.asTypeName()
      is IrFloatType -> Double::class.asTypeName()
      is IrIntType -> Int::class.asTypeName()
      is IrBooleanType -> Boolean::class.asTypeName()
      is IrIdType -> String::class.asTypeName()
      is IrAnyType -> Any::class.asTypeName()
      is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      is IrCustomScalarType -> resolveAndAssert(ResolverKeyKind.CustomScalarTarget, type.name)
      is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
      else -> error("$type is not a schema type")
    }.copy(nullable = true)
  }

  fun adapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    if (type !is IrNonNullType) {
      return when (type) {
        is IrIdType -> nullableScalarAdapter("NullableStringAdapter")
        is IrBooleanType -> nullableScalarAdapter("NullableBooleanAdapter")
        is IrStringType -> nullableScalarAdapter("NullableStringAdapter")
        is IrIntType -> nullableScalarAdapter("NullableIntAdapter")
        is IrFloatType -> nullableScalarAdapter("NullableDoubleAdapter")
        is IrAnyType -> nullableScalarAdapter("NullableAnyAdapter")
        else -> {
          val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
          CodeBlock.of("%L.%M()", adapterInitializer(IrNonNullType(type), requiresBuffering), nullableFun)
        }
      }
    }
    return nonNullableAdapterInitializer(type.ofType, requiresBuffering)
  }

  fun resolveCompiledType(name: String): CodeBlock {
    val builtin = when (name) {
      "String" -> MemberName("com.apollographql.apollo3.api", "CompiledStringType")
      "Int" -> MemberName("com.apollographql.apollo3.api", "CompiledIntType")
      "Float" -> MemberName("com.apollographql.apollo3.api", "CompiledFloatType")
      "Boolean" -> MemberName("com.apollographql.apollo3.api", "CompiledBooleanType")
      "ID" -> MemberName("com.apollographql.apollo3.api", "CompiledIDType")
      "__Schema" -> MemberName("com.apollographql.apollo3.api", "CompiledSchemaType")
      "__Type" -> MemberName("com.apollographql.apollo3.api", "CompiledTypeType")
      "__Field" -> MemberName("com.apollographql.apollo3.api", "CompiledFieldType")
      "__InputValue" -> MemberName("com.apollographql.apollo3.api", "CompiledInputValueType")
      "__EnumValue" -> MemberName("com.apollographql.apollo3.api", "CompiledEnumValueType")
      "__Directive" -> MemberName("com.apollographql.apollo3.api", "CompiledDirectiveType")
      else -> null
    }

    if (builtin != null) {
      return CodeBlock.of("%M", builtin)
    }

    return CodeBlock.of("%T.$type", resolveAndAssert(ResolverKeyKind.SchemaType, name))
  }

  private fun nonNullableAdapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    return when (type) {
      is IrNonNullType -> error("")
      is IrListType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", adapterInitializer(type.ofType, requiresBuffering), listFun)
      }
      is IrBooleanType -> CodeBlock.of("%T", BooleanAdapter::class)
      is IrIdType -> CodeBlock.of("%T", StringAdapter::class)
      is IrStringType -> CodeBlock.of("%T", StringAdapter::class)
      is IrIntType -> CodeBlock.of("%T", IntAdapter::class)
      is IrFloatType -> CodeBlock.of("%T", DoubleAdapter::class)
      is IrAnyType -> CodeBlock.of("%T", AnyAdapter::class)
      is IrEnumType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name))
      }
      is IrInputObjectType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name)).obj(requiresBuffering)
      }
      is IrCustomScalarType -> {
        CodeBlock.of(
            "$customScalarAdapters.responseAdapterFor<%T>(%L)",
            resolveIrType(IrNonNullType(type)),
            resolveCompiledType(type.name)
        )
      }
      is IrModelType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.ModelAdapter, type.path)).obj(requiresBuffering)
      }
      is IrOptionalType -> {
        val optionalFun = MemberName("com.apollographql.apollo3.api", "optional")
        CodeBlock.of("%L.%M()", adapterInitializer(type.ofType, requiresBuffering), optionalFun)
      }
      else -> error("Cannot create an adapter for $type")
    }
  }

  private fun nullableScalarAdapter(name: String) = CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api", name))


  fun resolveModel(path: String) = resolveAndAssert(ResolverKeyKind.Model, path)

  fun registerModelAdapter(path: String, className: ClassName) = register(ResolverKeyKind.ModelAdapter, path, className)
  fun resolveModelAdapter(path: String) = resolveAndAssert(ResolverKeyKind.ModelAdapter, path)

  fun registerEnumAdapter(name: String, className: ClassName) = register(ResolverKeyKind.SchemaTypeAdapter, name, className)
  fun registerInputObjectAdapter(name: String, className: ClassName) = register(ResolverKeyKind.SchemaTypeAdapter, name, className)

  fun registerOperation(name: String, className: ClassName) = register(ResolverKeyKind.Operation, name, className)
  fun resolveOperation(name: String) = resolveAndAssert(ResolverKeyKind.Operation, name)

  fun registerOperationVariablesAdapter(
      name: String,
      className: ClassName,
  ) = register(ResolverKeyKind.OperationVariablesAdapter, name, className)

  /**
   * Might be null if there are no variable
   */
  fun resolveOperationVariablesAdapter(name: String) = resolve(ResolverKeyKind.OperationVariablesAdapter, name)

  fun registerOperationSelections(name: String, className: ClassName) = register(ResolverKeyKind.OperationSelections, name, className)
  fun resolveOperationSelections(name: String) = resolveAndAssert(ResolverKeyKind.OperationSelections, name)

  fun registerFragment(name: String, className: ClassName) = register(ResolverKeyKind.Fragment, name, className)

  fun resolveFragment(name: String) = resolveAndAssert(ResolverKeyKind.Fragment, name)

  fun registerFragmentVariablesAdapter(
      name: String,
      className: ClassName,
  ) = register(ResolverKeyKind.FragmentVariablesAdapter, name, className)

  fun resolveFragmentVariablesAdapter(name: String) = resolve(ResolverKeyKind.FragmentVariablesAdapter, name)

  fun registerFragmentSelections(name: String, className: ClassName) = register(ResolverKeyKind.FragmentSelections, name, className)
  fun resolveFragmentSelections(name: String) = resolveAndAssert(ResolverKeyKind.FragmentSelections, name)

  fun entries() = classNames.map { ResolverEntry(it.key, ResolverClassName(it.value.packageName, it.value.simpleNames)) }
  fun resolveSchemaType(name: String) = resolveAndAssert(ResolverKeyKind.SchemaType, name)
  fun registerSchemaType(name: String, className: ClassName) = register(ResolverKeyKind.SchemaType, name, className)
  fun registerModel(path: String, className: ClassName) = register(ResolverKeyKind.Model, path, className)
  fun registerCustomScalar(name: String, className: ClassName) = register(ResolverKeyKind.CustomScalarTarget, name, className)
}