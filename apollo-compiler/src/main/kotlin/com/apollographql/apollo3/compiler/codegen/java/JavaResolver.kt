package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.list
import com.apollographql.apollo3.compiler.codegen.Identifier.nullable
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.java.adapter.singletonAdapterInitializer
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
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName


class JavaResolver(entries: List<ResolverEntry>, val next: JavaResolver?) {
  fun resolve(key: ResolverKey): ClassName? = classNames[key] ?: next?.resolve(key)

  private var classNames = entries.associateBy(
      keySelector = { it.key },
      valueTransform = { it.className.toJavaPoetClassName() }
  ).toMutableMap()
  
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
      return resolveIrType(type.ofType)
    }

    return when (type) {
      is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      is IrOptionalType -> ParameterizedTypeName.get(JavaClassNames.Optional, resolveIrType(type.ofType))
      is IrListType -> ParameterizedTypeName.get(JavaClassNames.List, resolveIrType(type.ofType))
      is IrStringType -> JavaClassNames.String
      is IrFloatType -> JavaClassNames.Double
      is IrIntType -> JavaClassNames.Integer
      is IrBooleanType -> JavaClassNames.Boolean
      is IrIdType -> JavaClassNames.String
      is IrAnyType -> JavaClassNames.Object
      is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      is IrCustomScalarType -> resolveAndAssert(ResolverKeyKind.CustomScalarTarget, type.name)
      is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
      else -> error("$type is not a schema type")
    }
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
          CodeBlock.of("$T.$nullable($L)", JavaClassNames.Adapters, adapterInitializer(IrNonNullType(type), requiresBuffering))
        }
      }
    }
    return nonNullableAdapterInitializer(type.ofType, requiresBuffering)
  }

  fun resolveCompiledType(name: String): CodeBlock {
    val builtin = when (name) {
      "String" -> "CompiledStringType"
      "Int" -> "CompiledIntType"
      "Float" -> "CompiledFloatType"
      "Boolean" -> "CompiledBooleanType"
      "ID" -> "CompiledIDType"
      "__Schema" -> "CompiledSchemaType"
      "__Type" -> "CompiledTypeType"
      "__Field" -> "CompiledFieldType"
      "__InputValue" -> "CompiledInputValueType"
      "__EnumValue" -> "CompiledEnumValueType"
      "__Directive" -> "CompiledDirectiveType"
      else -> null
    }

    if (builtin != null) {
      return CodeBlock.of("$T.$L", JavaClassNames.CompiledGraphQL, builtin)
    }

    return CodeBlock.of("$T.$type", resolveAndAssert(ResolverKeyKind.SchemaType, name))
  }

  private fun nonNullableAdapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    return when (type) {
      is IrNonNullType -> error("")
      is IrListType -> {
        CodeBlock.of("$T.$list($L)", JavaClassNames.Adapters, adapterInitializer(type.ofType, requiresBuffering))
      }
      is IrBooleanType -> nonNullableScalarAdapter(JavaClassNames.BooleanAdapter)
      is IrIdType -> nonNullableScalarAdapter(JavaClassNames.StringAdapter)
      is IrStringType -> nonNullableScalarAdapter(JavaClassNames.StringAdapter)
      is IrIntType -> nonNullableScalarAdapter(JavaClassNames.IntAdapter)
      is IrFloatType -> nonNullableScalarAdapter(JavaClassNames.DoubleAdapter)
      is IrAnyType -> nonNullableScalarAdapter(JavaClassNames.AnyAdapter)
      is IrEnumType -> {
        CodeBlock.of("$T.INSTANCE", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name))
      }
      is IrInputObjectType -> {
        singletonAdapterInitializer(
            resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name),
            resolveAndAssert(ResolverKeyKind.SchemaType, type.name),
            requiresBuffering
        )
      }
      is IrCustomScalarType -> {
        CodeBlock.of(
            "($customScalarAdapters.<$T>responseAdapterFor($L))",
            resolveIrType(IrNonNullType(type)),
            resolveCompiledType(type.name)
        )
      }
      is IrModelType -> {
        singletonAdapterInitializer(
            resolveAndAssert(ResolverKeyKind.ModelAdapter, type.path),
            resolveAndAssert(ResolverKeyKind.Model, type.path),
            requiresBuffering
        )
      }
      is IrOptionalType -> {
        CodeBlock.of("$T.${Identifier.optional}($L)", JavaClassNames.Adapters, adapterInitializer(type.ofType, requiresBuffering))
      }
      else -> error("Cannot create an adapter for $type")
    }
  }

  /**
   * Nullable adapters are @JvmField properties
   */
  private fun nullableScalarAdapter(name: String) = CodeBlock.of("$T.$L", JavaClassNames.Adapters, name)
  /**
   * Non Nullable adapters are object INSTANCES
   */
  private fun nonNullableScalarAdapter(className: ClassName) = CodeBlock.of("$T.INSTANCE", className)


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

  fun entries() = classNames.map { ResolverEntry(it.key, ResolverClassName(it.value.packageName(), it.value.simpleNames())) }
  fun resolveSchemaType(name: String) = resolveAndAssert(ResolverKeyKind.SchemaType, name)
  fun registerSchemaType(name: String, className: ClassName) = register(ResolverKeyKind.SchemaType, name, className)
  fun registerModel(path: String, className: ClassName) = register(ResolverKeyKind.Model, path, className)
  fun registerCustomScalar(name: String, className: ClassName) = register(ResolverKeyKind.CustomScalarTarget, name, className)
}

fun ResolverClassName.toJavaPoetClassName(): ClassName = ClassName.get(packageName, simpleNames[0], *simpleNames.drop(1).toTypedArray() )
