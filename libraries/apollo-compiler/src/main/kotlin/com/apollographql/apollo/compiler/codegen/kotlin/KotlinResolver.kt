package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.CODEGEN_METADATA_VERSION
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.type
import com.apollographql.apollo.compiler.codegen.ResolverClassName
import com.apollographql.apollo.compiler.codegen.ResolverEntry
import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.obj
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.parseType
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
import com.squareup.kotlinpoet.buildCodeBlock

internal class KotlinResolver(
    upstreamCodegenMetadata: CodegenMetadata,
    private val requiresOptInAnnotation: String?,
) {
  private val upstreamEntries = upstreamCodegenMetadata.entries.associate { it.key to it.className.toKotlinPoetClassName() }
  private val upstreamScalarAdapters = upstreamCodegenMetadata.scalarAdapters
  private val upstreamScalarTargets = upstreamCodegenMetadata.scalarTargets
  private val upstreamInlineProperties = upstreamCodegenMetadata.inlineProperties
  private val upstreamScalarIsUserDefined = upstreamCodegenMetadata.scalarIsUserDefined

  fun resolve(key: ResolverKey): ClassName? {
    return classNames[key] ?: upstreamEntries[key]
  }

  private val classNames = mutableMapOf<ResolverKey, ClassName>()
  private val scalarAdapters = mutableMapOf<String, String>()
  private val scalarTargets = mutableMapOf<String, String>()
  private val inlineProperties = mutableMapOf<String, String>()
  private val scalarIsUserDefined = mutableMapOf<String, Boolean>()
  private val mapTypes = mutableMapOf<String, ClassName>()
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
          is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
          is IrEnumType -> if (jsExport) {
            KotlinSymbols.String
          } else {
            resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
          }
          is IrScalarType -> resolveScalarTarget(type.name)

          is IrNamedType -> resolveSchemaType(type.name)
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

  internal fun resolveIrType2(type: IrType2): TypeName {
    return when (type) {
      is IrNonNullType2 -> resolveIrType2(type.ofType).copy(nullable = false)
      is IrListType2 -> KotlinSymbols.List.parameterizedBy(resolveIrType2(type.ofType)).copy(nullable = true)
      is IrCompositeType2 -> mapTypes.get(type.name) ?: error("Cannot find map type for ${type.name}")
      is IrEnumType2 -> resolveIrType(IrEnumType(type.name, nullable = true), false).copy(nullable = true)
      is IrScalarType2 -> resolveIrType(IrScalarType(type.name, nullable = true), false).copy(nullable = true)
    }
  }

  private fun CodeBlock.nullable(): CodeBlock {
    val nullableFun = MemberName("com.apollographql.apollo.api", "nullable")
    return CodeBlock.of("%L.%M()", this, nullableFun)
  }

  internal fun registerMapType(name: String, className: ClassName) = mapTypes.put(name, className)

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
        if (isScalarUserDefined(type.name)) {
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
        val initializer = adapterInitializer(type.nullable(false), requiresBuffering, jsExport)

        val nonNullableBuiltin = when (initializer.toString()) {
          KotlinSymbols.StringAdapter.canonicalName -> KotlinSymbols.NullableStringAdapter
          KotlinSymbols.BooleanAdapter.canonicalName -> KotlinSymbols.NullableBooleanAdapter
          KotlinSymbols.IntAdapter.canonicalName -> KotlinSymbols.NullableIntAdapter
          KotlinSymbols.DoubleAdapter.canonicalName -> KotlinSymbols.NullableDoubleAdapter
          KotlinSymbols.AnyAdapter.canonicalName -> KotlinSymbols.NullableAnyAdapter
          else -> null
        }
        return if (nonNullableBuiltin != null) {
          CodeBlock.of("%M", nonNullableBuiltin)
        } else {
          val nullableFun = MemberName("com.apollographql.apollo.api", "nullable")
          CodeBlock.of("%L.%M()", initializer, nullableFun)
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

  internal fun unwrapInlineClass(type: IrType): CodeBlock {
    val inlineClassProperty = resolveScalarInlineProperty(type.rawType().name)
    if (inlineClassProperty == null) {
      return CodeBlock.of("")
    }

    return buildCodeBlock {
      when {
        type.optional -> {
          add(".%M { it%L }", KotlinSymbols.PresentMap, unwrapInlineClass(type.copyWith(optional = false)))
        }
        type is IrListType -> {
          if (type.nullable) {
            add("?")
          }
          add(".map { it%L }", unwrapInlineClass(type.ofType))
        }

        type is IrScalarType -> {
          if (type.nullable) {
            add("?")
          }
          add(".%L", inlineClassProperty)
        }
        else -> Unit
      }
    }
  }

  internal fun wrapInlineClass(expression: CodeBlock, type: IrType): CodeBlock {
    val inlineClassProperty = resolveScalarInlineProperty(type.rawType().name)

    return buildCodeBlock {
      when (type) {
        is IrListType -> {
          if (inlineClassProperty != null) {
            add(expression)
            if (type.nullable) {
              add("?")
            }
            add(".map { %L }", wrapInlineClass(CodeBlock.of("it"), type.ofType))
          } else {
            add(expression)
          }
        }

        is IrScalarType -> {
          if (inlineClassProperty == null) {
            add(expression)
          } else {
            val targetName = resolveScalarTarget(type.name)
            when {
              type.nullable -> add("%L?.let { %T(it) }", expression, targetName)
              else -> add("%T(%L)", targetName, expression)
            }
          }
        }

        else -> {
          add(expression)
        }
      }
    }
  }

  fun resolveCompiledType(name: String): CodeBlock {
    return CodeBlock.of("%T.$type", resolveAndAssert(ResolverKeyKind.SchemaType, name))
  }

  private fun scalarAdapterInitializer(name: String): CodeBlock {
    val adapterInitializer = resolveScalarAdapterInitializer(name)
    return if (adapterInitializer == null) {
      val target = resolveScalarTarget(name)
      CodeBlock.of(
          "$customScalarAdapters.responseAdapterFor<%T>(%L)",
          target,
          resolveCompiledType(name)
      )
    } else {
      adapterInitializer
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


  fun resolveSchemaType(name: String) = resolveAndAssert(ResolverKeyKind.SchemaType, name)
  fun registerSchemaType(name: String, className: ClassName) = register(ResolverKeyKind.SchemaType, name, className)

  fun registerModel(path: String, className: ClassName) = register(ResolverKeyKind.Model, path, className)

  fun resolveRequiresOptInAnnotation(): ClassName? {
    if (requiresOptInAnnotation == "none") {
      return null
    }
    return requiresOptInAnnotation?.let { ClassName.bestGuess(it) }
  }

  fun registerSchema(className: ClassName) {
    register(ResolverKeyKind.Schema, "", className)
  }

  fun registerCustomScalarAdapters(className: ClassName) {
    register(ResolverKeyKind.CustomScalarAdapters, "", className)
  }

  fun registerArgumentDefinition(id: String, className: ClassName) = register(ResolverKeyKind.ArgumentDefinition, id, className)
  fun resolveArgumentDefinition(id: String) = resolveAndAssert(ResolverKeyKind.ArgumentDefinition, id)
  fun toCodegenMetadata(targetLanguage: TargetLanguage): CodegenMetadata {
    val entries = classNames.map { ResolverEntry(it.key, ResolverClassName(it.value.packageName, it.value.simpleNames)) }
    return CodegenMetadata(
        entries = entries,
        version = CODEGEN_METADATA_VERSION,
        targetLanguage = targetLanguage,
        inlineProperties = inlineProperties,
        scalarAdapters = scalarAdapters,
        scalarTargets = scalarTargets,
        scalarIsUserDefined = scalarIsUserDefined
    )
  }

  fun resolveScalarTarget(name: String): TypeName {
    val custom = scalarTargets[name] ?: upstreamScalarTargets[name]
    check(custom != null) {
      "Cannot resolve scalar target for '$name'"
    }

    return parseType(custom)
  }

  fun registerScalarTarget(name: String, target: String) {
    scalarTargets[name] = target
  }

  fun registerScalarAdapter(id: String, expression: String) {
    scalarAdapters[id] = expression
  }

  internal fun resolveScalarAdapterInitializer(name: String): CodeBlock? {
    val customAdapter = scalarAdapters[name] ?: upstreamScalarAdapters[name]
    if (customAdapter != null) {
      if (customAdapter.matches(Regex("com\\.apollographql\\.apollo\\.api\\.[a-zA-Z]*Adapter"))) {
        // Make the generated code look a bit nicer in case this is one of the built-in adapters
        return CodeBlock.of("%T", ClassName.bestGuess(customAdapter))
      }
      return CodeBlock.of("%L", customAdapter)
    }

    return null
  }

  fun registerScalarInlineProperty(id: String, propertyName: String) {
    inlineProperties[id] = propertyName
  }

  private fun resolveScalarInlineProperty(id: String): String? {
    return inlineProperties[id] ?: upstreamInlineProperties.get(id)
  }

  fun registerScalarIsUserDefined(id: String) {
    scalarIsUserDefined[id] = true
  }

  fun isScalarUserDefined(id: String): Boolean {
    return scalarIsUserDefined[id] ?: upstreamScalarIsUserDefined.get(id) ?: false
  }
}
