package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo.compiler.ScalarInfo
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.type
import com.apollographql.apollo.compiler.codegen.ResolverClassName
import com.apollographql.apollo.compiler.codegen.ResolverEntry
import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.bestGuess
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.obj
import com.apollographql.apollo.compiler.ir.IrCatchTo
import com.apollographql.apollo.compiler.ir.IrCompositeType2
import com.apollographql.apollo.compiler.ir.IrEnumType
import com.apollographql.apollo.compiler.ir.IrEnumType2
import com.apollographql.apollo.compiler.ir.IrInputObjectType
import com.apollographql.apollo.compiler.ir.IrListType
import com.apollographql.apollo.compiler.ir.IrListType2
import com.apollographql.apollo.compiler.ir.IrModelType
import com.apollographql.apollo.compiler.ir.IrNamedType
import com.apollographql.apollo.compiler.ir.IrNonNullType2
import com.apollographql.apollo.compiler.ir.IrObjectType
import com.apollographql.apollo.compiler.ir.IrScalarType
import com.apollographql.apollo.compiler.ir.IrScalarType2
import com.apollographql.apollo.compiler.ir.IrType
import com.apollographql.apollo.compiler.ir.IrType2
import com.apollographql.apollo.compiler.ir.catchTo
import com.apollographql.apollo.compiler.ir.maybeError
import com.apollographql.apollo.compiler.ir.nullable
import com.apollographql.apollo.compiler.ir.optional
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
) {
  fun resolve(key: ResolverKey): ClassName? {
    return classNames[key] ?: next?.resolve(key)
  }

  private var classNames = entries.associateBy(
      keySelector = { it.key },
      valueTransform = { it.className.toKotlinPoetClassName() }
  ).toMutableMap()

  private fun ResolverClassName.toKotlinPoetClassName() = ClassName(packageName, simpleNames)

  private fun resolve(kind: ResolverKeyKind, id: String) = resolve(ResolverKey(kind, id))
  private fun resolveAndAssert(kind: ResolverKeyKind, id: String): ClassName {
    val result = resolve(ResolverKey(kind, id))

    check(result != null) {
      "Cannot resolve $kind($id). " +
          "Have you set up an 'opposite link' on the downstream project to the schema module as a isADependencyOf(..)?"
    }
    return result
  }

  private fun resolveMemberNameAndAssert(kind: ResolverKeyKind, id: String): MemberName {
    val className = resolveAndAssert(kind, id)

    return MemberName(className.packageName, className.simpleName)
  }

  internal fun register(kind: ResolverKeyKind, id: String, className: ClassName) = classNames.put(ResolverKey(kind, id), className)

  private fun register(kind: ResolverKeyKind, id: String, memberName: MemberName) {
    check(memberName.enclosingClassName == null) {
      "enclosingClassName is not supported"
    }
    classNames.put(ResolverKey(kind, id), ClassName(memberName.packageName, memberName.simpleName))
  }

  internal fun resolveIrType(type: IrType, jsExport: Boolean, isInterface: Boolean = false): TypeName {
    return when {
      type.optional -> {
        KotlinSymbols.Optional.parameterizedBy(resolveIrType(type.optional(false), jsExport, isInterface))
      }

      type.catchTo != IrCatchTo.NoCatch -> {
        resolveIrType(type.catchTo(IrCatchTo.NoCatch), jsExport, isInterface).let {
          when (type.catchTo) {
            IrCatchTo.Null -> it.copy(nullable = true)
            IrCatchTo.Result -> KotlinSymbols.FieldResult.parameterizedBy(it)
            IrCatchTo.NoCatch -> error("") // keep the compiler happy
          }
        }
      }

      type.nullable -> {
        resolveIrType(type.nullable(false), jsExport, isInterface).copy(nullable = true)
      }

      else -> {
        when (type) {
          is IrListType -> resolveIrType(type.ofType, jsExport, isInterface).wrapInList(jsExport, isInterface)
          is IrScalarType -> {
            // Try mapping first, then built-ins, then fallback to Any
            resolveScalarTarget(type.name) ?: when (type.name) {
              "String" -> KotlinSymbols.String
              "Float" -> KotlinSymbols.Double
              "Int" -> KotlinSymbols.Int
              "Boolean" -> KotlinSymbols.Boolean
              "ID" -> KotlinSymbols.String
              else -> KotlinSymbols.Any
            }
          }

          is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
          is IrEnumType -> if (jsExport) {
            KotlinSymbols.String
          } else {
            resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
          }

          is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
        }
      }
    }
  }

  private fun TypeName.wrapInList(jsExport: Boolean, isInterface: Boolean): TypeName {
    val listType = if (jsExport) {
      KotlinSymbols.Array
    } else {
      KotlinSymbols.List
    }
    val param = if (jsExport && isInterface) {
      WildcardTypeName.producerOf(this)
    } else {
      this
    }
    return listType.parameterizedBy(param)
  }

  private fun resolveScalarTarget(name: String): TypeName? {
    return scalarMapping[name]?.targetName?.let {
      bestGuess(it)
    }
  }

  internal fun resolveIrType2(type: IrType2): TypeName {
    return when (type) {
      is IrNonNullType2 -> resolveIrType2(type.ofType).copy(nullable = false)
      is IrListType2 -> KotlinSymbols.List.parameterizedBy(resolveIrType2(type.ofType)).copy(nullable = true)
      is IrCompositeType2 -> resolveAndAssert(ResolverKeyKind.MapType, type.name).copy(nullable = true)
      is IrEnumType2 -> resolveIrType(IrEnumType(type.name, nullable = true), false).copy(nullable = true)
      is IrScalarType2 -> resolveIrType(IrScalarType(type.name, nullable = true), false).copy(nullable = true)
    }
  }

  private fun CodeBlock.nullable(): CodeBlock {
    val nullableFun = MemberName("com.apollographql.apollo.api", "nullable")
    return CodeBlock.of("%L.%M()", this, nullableFun)
  }


  private fun CodeBlock.list(jsExport: Boolean): CodeBlock {
    val listFun = if (jsExport) {
      MemberName("com.apollographql.apollo.api", "array")
    } else {
      MemberName("com.apollographql.apollo.api", "list")
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
          scalarAdapterInitializer(type.name)
        } else {
          null
        }
      }

      is IrEnumType2 -> {
        adapterInitializer(IrEnumType(type.name), false, jsExport)
      }

      is IrCompositeType2 -> null
    }
  }

  internal fun adapterInitializer(type: IrType, requiresBuffering: Boolean, jsExport: Boolean): CodeBlock {
    return when {
      type.optional -> {
        val presentFun = MemberName("com.apollographql.apollo.api", "present")
        CodeBlock.of("%L.%M()", adapterInitializer(type.optional(false), requiresBuffering, jsExport), presentFun)
      }

      type.catchTo != IrCatchTo.NoCatch -> {
        adapterInitializer(type.catchTo(IrCatchTo.NoCatch), requiresBuffering, jsExport).let {
          val member = when (type.catchTo) {
            IrCatchTo.Null -> KotlinSymbols.catchToNull
            IrCatchTo.Result -> KotlinSymbols.catchToResult
            IrCatchTo.NoCatch -> error("") // happy compiler
          }
          CodeBlock.of("%L.%M()", it, member)
        }
      }

      type.maybeError -> {
        adapterInitializer(type.maybeError(false), requiresBuffering, jsExport).let {
          CodeBlock.of("%L.%M()", it, KotlinSymbols.errorAware)
        }
      }

      type.nullable -> {
        // Don't hardcode the adapter when the scalar is mapped to a user-defined type
        val scalarWithoutCustomMapping = type is IrScalarType && !scalarMapping.containsKey(type.name)
        when {
          type is IrScalarType && type.name == "ID" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableStringAdapter)
          type is IrScalarType && type.name == "Boolean" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableBooleanAdapter)
          type is IrScalarType && type.name == "String" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableStringAdapter)
          type is IrScalarType && type.name == "Int" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableIntAdapter)
          type is IrScalarType && type.name == "Float" && scalarWithoutCustomMapping -> CodeBlock.of("%M", KotlinSymbols.NullableDoubleAdapter)
          type is IrScalarType && resolveScalarTarget(type.name) == null -> {
            CodeBlock.of("%M", KotlinSymbols.NullableAnyAdapter)
          }

          else -> {
            val nullableFun = MemberName("com.apollographql.apollo.api", "nullable")
            CodeBlock.of("%L.%M()", adapterInitializer(type.nullable(false), requiresBuffering, jsExport), nullableFun)
          }
        }
      }

      else -> {
        when (type) {
          is IrListType -> {
            adapterInitializer(type.ofType, requiresBuffering, jsExport).list(jsExport)
          }

          is IrScalarType -> {
            scalarAdapterInitializer(type.name)
          }

          is IrEnumType -> {
            if (jsExport) {
              scalarAdapterInitializer("String")
            } else {
              CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name))
            }
          }

          is IrInputObjectType -> {
            CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.SchemaTypeAdapter, type.name)).obj(requiresBuffering)
          }

          is IrModelType -> {
            CodeBlock.of("%T", resolveAndAssert(ResolverKeyKind.ModelAdapter, type.path)).obj(requiresBuffering)
          }

          is IrObjectType -> error("IrObjectType cannot be adapted")
        }
      }
    }
  }

  fun resolveCompiledType(name: String): CodeBlock {
    return CodeBlock.of("%T.$type", resolveAndAssert(ResolverKeyKind.SchemaType, name))
  }

  private fun scalarAdapterInitializer(name: String): CodeBlock {
    return when (val adapterInitializer = scalarMapping[name]?.adapterInitializer) {
      is ExpressionAdapterInitializer -> {
        CodeBlock.of(adapterInitializer.expression)
      }

      is RuntimeAdapterInitializer -> {
        val target = resolveScalarTarget(name)
        CodeBlock.of(
            "$customScalarAdapters.responseAdapterFor<%T>(%L)",
            target,
            resolveCompiledType(name)
        )
      }

      else -> {
        when (name) {
          "Boolean" -> CodeBlock.of("%M", KotlinSymbols.BooleanAdapter)
          "ID" -> CodeBlock.of("%M", KotlinSymbols.StringAdapter)
          "String" -> CodeBlock.of("%M", KotlinSymbols.StringAdapter)
          "Int" -> CodeBlock.of("%M", KotlinSymbols.IntAdapter)
          "Float" -> CodeBlock.of("%M", KotlinSymbols.DoubleAdapter)
          else -> {
            val target = resolveScalarTarget(name)
            if (target == null) {
              CodeBlock.of("%M", KotlinSymbols.AnyAdapter)
            } else {
              CodeBlock.of(
                  "$customScalarAdapters.responseAdapterFor<%T>(%L)",
                  target,
                  resolveCompiledType(name)
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

  fun registerCustomScalarAdapters(className: ClassName) {
    register(ResolverKeyKind.CustomScalarAdapters, "", className)
  }

  fun resolveCustomScalarAdapters(): ClassName = resolveAndAssert(ResolverKeyKind.CustomScalarAdapters, "")

  fun registerArgumentDefinition(id: String, className: ClassName) = register(ResolverKeyKind.ArgumentDefinition, id, className)
  fun resolveArgumentDefinition(id: String) = resolveAndAssert(ResolverKeyKind.ArgumentDefinition, id)
}
