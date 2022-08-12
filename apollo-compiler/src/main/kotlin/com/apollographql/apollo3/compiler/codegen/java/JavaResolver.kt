package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.java.adapter.singletonAdapterInitializer
import com.apollographql.apollo3.compiler.ir.IrCompositeType2
import com.apollographql.apollo3.compiler.ir.IrEnumType
import com.apollographql.apollo3.compiler.ir.IrEnumType2
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrListType2
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNamedType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrNonNullType2
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.IrScalarType2
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.IrType2
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.kotlinpoet.MemberName


internal class JavaResolver(entries: List<ResolverEntry>, val next: JavaResolver?, private val scalarMapping: Map<String, ScalarInfo>) {
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
      is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      is IrScalarType -> resolveIrScalarType(type)
      is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
    }
  }

  private fun resolveIrScalarType(type: IrScalarType): ClassName {
    // Try mapping first, then built-ins, then fallback to Object
    return resolveScalarTarget(type.name) ?: when (type.name) {
      "String" -> JavaClassNames.String
      "ID" -> JavaClassNames.String
      "Float" -> JavaClassNames.Double
      "Int" -> JavaClassNames.Integer
      "Boolean" -> JavaClassNames.Boolean
      else -> JavaClassNames.Object
    }
  }

  fun adapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    if (type !is IrNonNullType) {
      // Don't hardcode the adapter when the scalar is mapped to a user-defined type
      val scalarWithoutCustomMapping = type is IrScalarType && !scalarMapping.containsKey(type.name)
      return when {
        type is IrScalarType && type.name == "String" && scalarWithoutCustomMapping -> adapterCodeBlock("NullableStringAdapter")
        type is IrScalarType && type.name == "ID" && scalarWithoutCustomMapping -> adapterCodeBlock("NullableStringAdapter")
        type is IrScalarType && type.name == "Boolean" && scalarWithoutCustomMapping -> adapterCodeBlock("NullableBooleanAdapter")
        type is IrScalarType && type.name == "Int" && scalarWithoutCustomMapping -> adapterCodeBlock("NullableIntAdapter")
        type is IrScalarType && type.name == "Float" && scalarWithoutCustomMapping -> adapterCodeBlock("NullableDoubleAdapter")
        type is IrScalarType && resolveScalarTarget(type.name) == null -> {
          adapterCodeBlock("NullableAnyAdapter")
        }
        else -> {
          CodeBlock.of("new $T<>($L)", JavaClassNames.NullableAdapter, adapterInitializer(IrNonNullType(type), requiresBuffering))
        }
      }
    }
    return nonNullableAdapterInitializer(type.ofType, requiresBuffering)
  }

  private fun resolveScalarTarget(name: String): ClassName? {
    return scalarMapping.get(name)?.targetName?.let {
      ClassName.bestGuess(it)
    }
  }

  fun resolveCompiledType(name: String): CodeBlock {
    val builtin = when (name) {
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
        adapterInitializer(type.ofType, requiresBuffering).listAdapter()
      }

      is IrScalarType -> {
        nonNullableScalarAdapterInitializer(type)
      }

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

      is IrModelType -> {
        singletonAdapterInitializer(
            resolveAndAssert(ResolverKeyKind.ModelAdapter, type.path),
            resolveAndAssert(ResolverKeyKind.Model, type.path),
            requiresBuffering
        )
      }

      is IrOptionalType -> {
        CodeBlock.of("new $T<>($L)", JavaClassNames.OptionalAdapter, adapterInitializer(type.ofType, requiresBuffering))
      }
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
            "($customScalarAdapters.<$T>responseAdapterFor($L))",
            target,
            resolveCompiledType(type.name)
        )
      }
      else -> {
        when (type.name) {
          "Boolean" -> adapterCodeBlock("BooleanAdapter")
          "ID" -> adapterCodeBlock("StringAdapter")
          "String" -> adapterCodeBlock("StringAdapter")
          "Int" -> adapterCodeBlock("IntAdapter")
          "Float" -> adapterCodeBlock("DoubleAdapter")
          else -> {
            val target = resolveScalarTarget(type.name)
            if (target == null) {
              adapterCodeBlock("AnyAdapter")
            } else {
              CodeBlock.of(
                  "($customScalarAdapters.<$T>responseAdapterFor($L))",
                  target,
                  resolveCompiledType(type.name)
              )
            }
          }
        }
      }
    }
  }

  /**
   * Nullable adapters are @JvmField properties
   */
  private fun adapterCodeBlock(name: String) = CodeBlock.of("$T.$L", JavaClassNames.Adapters, name)

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

  internal fun resolveIrType2(type: IrType2): TypeName {
    return when (type) {
      is IrNonNullType2 -> resolveIrType2(type.ofType)
      is IrListType2 -> ParameterizedTypeName.get(JavaClassNames.List, resolveIrType2(type.ofType))
      is IrCompositeType2 -> resolveAndAssert(ResolverKeyKind.MapType, type.name)
      is IrEnumType2 -> resolveIrType(IrEnumType(type.name))
      is IrScalarType2 -> resolveIrType(IrScalarType(type.name))
    }
  }

  internal fun adapterInitializer2(type: IrType2): CodeBlock? {
    if (type !is IrNonNullType2) {
      return adapterInitializer2(IrNonNullType2(type))
    }
    return nonNullableAdapterInitializer2(type.ofType)
  }

  fun registerMapType(name: String, className: ClassName) = register(ResolverKeyKind.MapType, name, className)

  private fun nonNullableAdapterInitializer2(type: IrType2): CodeBlock? {
    return when (type) {
      is IrNonNullType2 -> error("")
      is IrListType2 -> adapterInitializer2(type.ofType)?.listAdapter()
      is IrScalarType2 -> {
        if (scalarMapping.containsKey(type.name)) {
          nonNullableScalarAdapterInitializer(IrScalarType(type.name))
        } else {
          null
        }
      }
      is IrEnumType2 -> {
        nonNullableAdapterInitializer(IrEnumType(type.name), false)
      }
      is IrCompositeType2 -> null
    }
  }

  private fun CodeBlock.listAdapter(): CodeBlock {
    return CodeBlock.of("new $T<>($L)", JavaClassNames.ListAdapter, this)
  }

  fun registerSchema(className: ClassName) = register(ResolverKeyKind.Schema, "", className)
  fun resolveSchema():ClassName = resolveAndAssert(ResolverKeyKind.Schema, "")
}

fun ResolverClassName.toJavaPoetClassName(): ClassName = ClassName.get(packageName, simpleNames[0], *simpleNames.drop(1).toTypedArray())
