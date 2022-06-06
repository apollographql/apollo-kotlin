package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.obj
import com.apollographql.apollo3.compiler.ir.IrEnumType
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNamedType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.IrType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName


class KotlinResolver(
    entries: List<ResolverEntry>,
    val next: KotlinResolver?,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val requiresOptInAnnotation: String?
) {
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

  fun resolveIrType(type: IrType, override: (IrType) -> TypeName? = { null }): TypeName {
    if (type is IrNonNullType) {
      return resolveIrType(type.ofType, override).copy(nullable = false)
    }

    override(type)?.let {
      return it
    }

    return when {
      type is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      type is IrOptionalType -> KotlinSymbols.Optional.parameterizedBy(resolveIrType(type.ofType, override))
      type is IrListType -> KotlinSymbols.List.parameterizedBy(resolveIrType(type.ofType, override))
      type is IrScalarType -> resolveIrScalarType(type)
      type is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      type is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
      else -> error("$type is not a schema type")
    }.copy(nullable = true)
  }

  private fun resolveIrScalarType(type: IrScalarType): ClassName {
    // Try mapping first, then built-ins, then fallback to Any
    return resolveScalarTarget(type.name) ?: when (type.name) {
      "String" -> KotlinSymbols.String
      "Float" -> KotlinSymbols.Double
      "Int" -> KotlinSymbols.Int
      "Boolean" -> KotlinSymbols.Boolean
      "ID" -> KotlinSymbols.String
      else -> KotlinSymbols.Any
    }
  }

  private fun resolveScalarTarget(name: String): ClassName? {
    return scalarMapping[name]?.targetName?.let {
      ClassName.bestGuess(it)
    }
  }

  fun adapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    if (type !is IrNonNullType) {
      // Don't hardcode the adapter when the scalar is mapped to a user-defined type
      val scalarWithoutCustomMapping = type is IrScalarType && !scalarMapping.containsKey(type.name)
      return when {
        type is IrScalarType && type.name == "ID" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableStringAdapter)
        type is IrScalarType && type.name == "Boolean" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableBooleanAdapter)
        type is IrScalarType && type.name == "String" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableStringAdapter)
        type is IrScalarType && type.name == "Int" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableIntAdapter)
        type is IrScalarType && type.name == "Float" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableDoubleAdapter)
        type is IrScalarType && resolveScalarTarget(type.name) == null -> {
          CodeBlock.of("%M", KotlinSymbols.NullableAnyAdapter)
        }
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
    return when {
      type is IrNonNullType -> error("")
      type is IrListType -> {
        val listFun = MemberName("com.apollographql.apollo3.api", "list")
        CodeBlock.of("%L.%M()", adapterInitializer(type.ofType, requiresBuffering), listFun)
      }
      type is IrScalarType -> {
        nonNullableScalarAdapterInitializer(type)
      }
      type is IrEnumType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name))
      }
      type is IrInputObjectType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name)).obj(requiresBuffering)
      }
      type is IrModelType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.ModelAdapter, type.path)).obj(requiresBuffering)
      }
      type is IrOptionalType -> {
        val optionalFun = MemberName("com.apollographql.apollo3.api", "optional")
        CodeBlock.of("%L.%M()", adapterInitializer(type.ofType, requiresBuffering), optionalFun)
      }
      else -> error("Cannot create an adapter for $type")
    }
  }

  private fun nonNullableScalarAdapterInitializer(type: IrScalarType): CodeBlock {
    return when (val adapterInitializer = scalarMapping[type.name]?.adapterInitializer) {
      is ExpressionAdapterInitializer -> {
        CodeBlock.of(adapterInitializer.expression)
      }
      is RuntimeAdapterInitializer -> {
        val target = resolveScalarTarget(type.name)
        CodeBlock.of(
            "$customScalarAdapters.responseAdapterFor<%T>(%L)",
            target,
            resolveCompiledType(type.name)
        )
      }
      else -> {
        when (type.name) {
          "Boolean" -> CodeBlock.of("%M", KotlinSymbols.BooleanAdapter)
          "ID" -> CodeBlock.of("%M", KotlinSymbols.StringAdapter)
          "String" -> CodeBlock.of("%M", KotlinSymbols.StringAdapter)
          "Int" -> CodeBlock.of("%M", KotlinSymbols.IntAdapter)
          "Float" -> CodeBlock.of("%M", KotlinSymbols.DoubleAdapter)
          else -> {
            val target = resolveScalarTarget(type.name)
            if (target == null) {
              CodeBlock.of("%M", KotlinSymbols.AnyAdapter)
            } else {
              CodeBlock.of(
                  "$customScalarAdapters.responseAdapterFor<%T>(%L)",
                  target,
                  resolveCompiledType(type.name)
              )
            }
          }
        }
      }
    }
  }

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

  fun registerTestBuilder(path: String, className: ClassName) = register(ResolverKeyKind.TestBuilder, path, className)
  fun resolveTestBuilder(path: String) = resolveAndAssert(ResolverKeyKind.TestBuilder, path)
  fun resolveRequiresOptInAnnotation(): ClassName? {
    if (requiresOptInAnnotation == "none") {
      return null
    }
    return requiresOptInAnnotation?.let { ClassName.bestGuess(it) }
  }
}
