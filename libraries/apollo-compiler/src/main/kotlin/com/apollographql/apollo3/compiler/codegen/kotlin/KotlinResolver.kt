package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.scalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.kotlin.adapter.obj
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName


internal class KotlinResolver(
    entries: List<ResolverEntry>,
    val next: KotlinResolver?,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val requiresOptInAnnotation: String?,
    private val hooks: ApolloCompilerKotlinHooks,
) {
  fun resolve(key: ResolverKey): ClassName? = hooks.overrideResolvedType(key, classNames[key] ?: next?.resolve(key))

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

  private fun resolveMemberNameAndAssert(kind: ResolverKeyKind, id: String): MemberName {
    val className = resolveAndAssert(kind, id)

    return MemberName(className.packageName, className.simpleName)
  }

  internal fun register(kind: ResolverKeyKind, id: String, className: ClassName) = classNames.put(ResolverKey(kind, id), className)

  private fun register(kind: ResolverKeyKind, id: String, memberName: MemberName): Unit {
    check(memberName.enclosingClassName == null) {
      "enclosingClassName is not supported"
    }
    classNames.put(ResolverKey(kind, id), ClassName(memberName.packageName, memberName.simpleName))
  }

  internal fun resolveIrType(type: IrType, jsExport: Boolean, isInterface: Boolean = false, override: (IrType) -> TypeName? = { null }): TypeName {
    if (type is IrNonNullType) {
      return resolveIrType(type.ofType, jsExport, isInterface, override = override).copy(nullable = false)
    }

    override(type)?.let {
      return it
    }

    return when {
      type is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      type is IrOptionalType -> KotlinSymbols.Optional.parameterizedBy(resolveIrType(type.ofType, jsExport, isInterface, override = override))
      type is IrListType -> toListType(resolveIrType(type.ofType, jsExport, isInterface, override), jsExport, isInterface)
      type is IrScalarType -> resolveIrScalarType(type)
      type is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      type is IrEnumType -> if (jsExport) {
        KotlinSymbols.String
      } else {
        resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
      }
      type is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
      else -> error("$type is not a schema type")
    }.copy(nullable = true)
  }

  private fun toListType(ofType: TypeName, jsExport: Boolean, isInterface: Boolean): TypeName {
    val listType = if (jsExport) {
      KotlinSymbols.Array
    } else {
      KotlinSymbols.List
    }
    val param = if (jsExport && isInterface) {
      WildcardTypeName.producerOf(ofType)
    } else {
      ofType
    }
    return listType.parameterizedBy(param)
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

  internal fun resolveIrType2(type: IrType2): TypeName {
    return when (type) {
      is IrNonNullType2 -> resolveIrType2(type.ofType).copy(nullable = false)
      is IrListType2 -> KotlinSymbols.List.parameterizedBy(resolveIrType2(type.ofType)).copy(nullable = true)
      is IrCompositeType2 -> resolveAndAssert(ResolverKeyKind.MapType, type.name).copy(nullable = true)
      is IrEnumType2 -> resolveIrType(IrEnumType(type.name), false).copy(nullable = true)
      is IrScalarType2 -> resolveIrType(IrScalarType(type.name), false).copy(nullable = true)
    }
  }

  private fun CodeBlock.nullable(): CodeBlock {
    val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
    return CodeBlock.of("%L.%M()", this, nullableFun)
  }


  private fun CodeBlock.list(jsExport: Boolean): CodeBlock {
    val listFun = if (jsExport) {
      MemberName("com.apollographql.apollo3.api", "array")
    } else {
      MemberName("com.apollographql.apollo3.api", "list")
    }
    return CodeBlock.of("%L.%M()", this, listFun)
  }

  internal fun adapterInitializer2(type: IrType2, jsExport: Boolean): CodeBlock? {
    if (type !is IrNonNullType2) {
      return adapterInitializer2(IrNonNullType2(type), jsExport)?.nullable()
    }
    return nonNullableAdapterInitializer2(type.ofType, jsExport)
  }

  private fun nonNullableAdapterInitializer2(type: IrType2, jsExport: Boolean): CodeBlock? {
    return when (type) {
      is IrNonNullType2 -> error("")
      is IrListType2 -> adapterInitializer2(type.ofType, jsExport)?.list(jsExport)
      is IrScalarType2 -> {
        if (scalarMapping.containsKey(type.name)) {
          nonNullableScalarAdapterInitializer(IrScalarType(type.name), scalarAdapters)
        } else {
          null
        }
      }

      is IrEnumType2 -> {
        nonNullableAdapterInitializer(IrEnumType(type.name), false, jsExport)
      }

      is IrCompositeType2 -> null
    }
  }

  internal fun adapterInitializer(type: IrType, requiresBuffering: Boolean, jsExport: Boolean): CodeBlock {
    if (type !is IrNonNullType) {
      // Don't hardcode the adapter when the scalar is mapped to a user-defined type
      val scalarWithoutCustomMapping = type is IrScalarType && !scalarMapping.containsKey(type.name)
      return when {
        type is IrScalarType && type.name == "ID" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableStringDataAdapter)
        type is IrScalarType && type.name == "Boolean" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableBooleanDataAdapter)
        type is IrScalarType && type.name == "String" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableStringDataAdapter)
        type is IrScalarType && type.name == "Int" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableIntDataAdapter)
        type is IrScalarType && type.name == "Float" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableDoubleDataAdapter)
        type is IrScalarType && resolveScalarTarget(type.name) == null -> {
          CodeBlock.of("%M", KotlinSymbols.NullableAnyDataAdapter)
        }

        else -> {
          val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
          CodeBlock.of("%L.%M()", adapterInitializer(IrNonNullType(type), requiresBuffering, jsExport), nullableFun)
        }
      }
    }
    return nonNullableAdapterInitializer(type.ofType, requiresBuffering, jsExport)
  }

  fun resolveCompiledType(name: String): CodeBlock {
    return CodeBlock.of("%T.$type", resolveAndAssert(ResolverKeyKind.SchemaType, name))
  }

  private fun nonNullableAdapterInitializer(type: IrType, requiresBuffering: Boolean, jsExport: Boolean): CodeBlock {
    return when {
      type is IrNonNullType -> error("")
      type is IrListType -> {
        adapterInitializer(type.ofType, requiresBuffering, jsExport).list(jsExport)
      }

      type is IrScalarType -> {
        nonNullableScalarAdapterInitializer(type, "${Identifier.context}.$scalarAdapters")
      }

      type is IrEnumType -> {
        if (jsExport) {
          nonNullableScalarAdapterInitializer(IrScalarType("String"))
        } else {
          CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name))
        }
      }

      type is IrInputObjectType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name)).obj(requiresBuffering)
      }

      type is IrModelType -> {
        CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.ModelAdapter, type.path)).obj(requiresBuffering)
      }

      type is IrOptionalType -> {
        val presentFun = MemberName("com.apollographql.apollo3.api", "present")
        CodeBlock.of("%L.%M()", adapterInitializer(type.ofType, requiresBuffering, jsExport), presentFun)
      }

      else -> error("Cannot create an adapter for $type")
    }
  }

  private fun nonNullableScalarAdapterInitializer(type: IrScalarType, scalarAdapters: String): CodeBlock {
    return when (val adapterInitializer = scalarMapping[type.name]?.adapterInitializer) {
      is ExpressionAdapterInitializer -> {
        CodeBlock.of("%T(%L)",
            KotlinSymbols.AdapterToDataAdapter,
            adapterInitializer.expression
        )
      }

      is RuntimeAdapterInitializer -> {
        val target = resolveScalarTarget(type.name)
        CodeBlock.of(
            "$scalarAdapters.responseAdapterFor<%T>(%L)",
            target,
            resolveCompiledType(type.name)
        )
      }

      else -> {
        when (type.name) {
          "Boolean" -> CodeBlock.of("%M", KotlinSymbols.BooleanDataAdapter)
          "ID" -> CodeBlock.of("%M", KotlinSymbols.StringDataAdapter)
          "String" -> CodeBlock.of("%M", KotlinSymbols.StringDataAdapter)
          "Int" -> CodeBlock.of("%M", KotlinSymbols.IntDataAdapter)
          "Float" -> CodeBlock.of("%M", KotlinSymbols.DoubleDataAdapter)
          else -> {
            val target = resolveScalarTarget(type.name)
            if (target == null) {
              CodeBlock.of("%M", KotlinSymbols.AnyDataAdapter)
            } else {
              CodeBlock.of(
                  "$scalarAdapters.responseAdapterFor<%T>(%L)",
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
  fun registerMapType(name: String, className: ClassName) = register(ResolverKeyKind.MapType, name, className)
  fun resolveMapType(name: String) = resolveAndAssert(ResolverKeyKind.MapType, name)

  fun registerModel(path: String, className: ClassName) = register(ResolverKeyKind.Model, path, className)

  fun registerBuilderType(name: String, className: ClassName) = register(ResolverKeyKind.BuilderType, name, className)
  fun resolveBuilderType(name: String) = resolveAndAssert(ResolverKeyKind.BuilderType, name)

  fun registerBuilderFun(name: String, memberName: MemberName) = register(ResolverKeyKind.BuilderFun, name, memberName)
  fun resolveBuilderFun(name: String) = resolveMemberNameAndAssert(ResolverKeyKind.BuilderFun, name)

  fun resolveRequiresOptInAnnotation(): ClassName? {
    if (requiresOptInAnnotation == "none") {
      return null
    }
    return requiresOptInAnnotation?.let { ClassName.bestGuess(it) }
  }

  fun registerSchema(className: ClassName) {
    register(ResolverKeyKind.Schema, "", className)
  }

  fun resolveSchema(): ClassName = resolveAndAssert(ResolverKeyKind.Schema, "")

  fun registerScalarAdapters(className: ClassName) {
    register(ResolverKeyKind.ScalarAdapters, "", className)
  }

  fun resolveScalarAdapters(): ClassName = resolveAndAssert(ResolverKeyKind.ScalarAdapters, "")
}
