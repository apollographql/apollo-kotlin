package com.apollographql.apollo.compiler.codegen.java

import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.CODEGEN_METADATA_VERSION
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.type
import com.apollographql.apollo.compiler.codegen.ResolverClassName
import com.apollographql.apollo.compiler.codegen.ResolverEntry
import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.java.helpers.parseType
import com.apollographql.apollo.compiler.codegen.java.helpers.singletonAdapterInitializer
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
import com.apollographql.apollo.compiler.ir.isComposite
import com.apollographql.apollo.compiler.ir.isCompositeOrWrappedComposite
import com.apollographql.apollo.compiler.ir.nullable
import com.apollographql.apollo.compiler.ir.optional
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName


internal class JavaResolver(
    upstreamCodegenMetadata: CodegenMetadata,
    private val generatePrimitiveTypes: Boolean,
    private val nullableFieldStyle: JavaNullable,
) {
  private val upstreamEntries = upstreamCodegenMetadata.entries.associate { it.key to it.className.toJavaPoetClassName() }
  private val upstreamScalarAdapters = upstreamCodegenMetadata.scalarAdapters
  private val upstreamScalarTargets = upstreamCodegenMetadata.scalarTargets
  private val upstreamScalarIsUserDefined = upstreamCodegenMetadata.scalarIsUserDefined

  private val classNames = mutableMapOf<ResolverKey, ClassName>()

  private val scalarAdapters = mutableMapOf<String, String>()
  private val scalarTargets = mutableMapOf<String, String>()
  private val scalarIsUserDefined = mutableMapOf<String, Boolean>()
  private val mapTypes = mutableMapOf<String, ClassName>()

  private val optionalClassName: ClassName = when (nullableFieldStyle) {
    JavaNullable.JAVA_OPTIONAL -> JavaClassNames.JavaOptional
    JavaNullable.GUAVA_OPTIONAL -> JavaClassNames.GuavaOptional
    else -> JavaClassNames.Optional
  }

  private fun getOptionalAdapterClassName(): ClassName = when (nullableFieldStyle) {
    JavaNullable.JAVA_OPTIONAL, JavaNullable.GUAVA_OPTIONAL -> resolveJavaOptionalAdapter()
    else -> JavaClassNames.ApolloOptionalAdapter
  }

  private fun getOptionalOrNullableAdapterClassName(): ClassName = when (nullableFieldStyle) {
    JavaNullable.APOLLO_OPTIONAL -> JavaClassNames.ApolloOptionalAdapter
    JavaNullable.JAVA_OPTIONAL, JavaNullable.GUAVA_OPTIONAL -> resolveJavaOptionalAdapter()
    else -> JavaClassNames.NullableAdapter
  }

  private val wrapNullableFieldsInOptional = nullableFieldStyle in setOf(
      JavaNullable.APOLLO_OPTIONAL,
      JavaNullable.JAVA_OPTIONAL,
      JavaNullable.GUAVA_OPTIONAL,
  )

  private val nullableAnnotationClassName: ClassName? = when (nullableFieldStyle) {
    JavaNullable.JETBRAINS_ANNOTATIONS -> JavaClassNames.JetBrainsNullable
    JavaNullable.ANDROID_ANNOTATIONS -> JavaClassNames.AndroidNullable
    JavaNullable.JSR_305_ANNOTATIONS -> JavaClassNames.Jsr305Nullable
    else -> null
  }

  val notNullAnnotationClassName: ClassName? = when (nullableFieldStyle) {
    JavaNullable.JETBRAINS_ANNOTATIONS -> JavaClassNames.JetBrainsNonNull
    JavaNullable.ANDROID_ANNOTATIONS -> JavaClassNames.AndroidNonNull
    JavaNullable.JSR_305_ANNOTATIONS -> JavaClassNames.Jsr305NonNull
    else -> null
  }

  fun resolve(key: ResolverKey): ClassName? {
    return classNames[key] ?: upstreamEntries[key]
  }

  private fun resolve(kind: ResolverKeyKind, id: String) = resolve(ResolverKey(kind, id))
  private fun resolveAndAssert(kind: ResolverKeyKind, id: String): ClassName {
    val result = resolve(ResolverKey(kind, id))

    check(result != null) {
      "Cannot resolve $kind($id). Have you set up an 'opposite link' on the downstream project to the schema module as a isADependencyOf(..)?"
    }
    return result
  }

  private fun register(kind: ResolverKeyKind, id: String, className: ClassName) = classNames.put(ResolverKey(kind, id), className)

  fun resolveIrType(type: IrType): TypeName {
    return if (type.optional) {
      resolveIrType(type.optional(false)).wrapInOptional().addNonNullableAnnotation()
    } else if (type.catchTo != IrCatchTo.NoCatch) {
      error("Java codegen does not support @catch")
    } else if (type.nullable) {
      resolveRawIrType(type).boxIfPrimitiveType().let {
        if (wrapNullableFieldsInOptional) it.wrapInOptional() else it.addNullableAnnotation()
      }
    } else {
      resolveRawIrType(type).addNonNullableAnnotation()
    }
  }

  private fun TypeName.wrapInList(): TypeName = ParameterizedTypeName.get(JavaClassNames.List, this.boxIfPrimitiveType())

  private fun TypeName.wrapInOptional(): TypeName {
    return ParameterizedTypeName.get(optionalClassName, this.filterTypeUseAnnotations().boxIfPrimitiveType())
  }

  private fun resolveRawIrType(type: IrType): TypeName {
    return when (type) {
      is IrListType -> resolveIrType(type.ofType).filterTypeUseAnnotations().wrapInList()
      is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      is IrScalarType -> resolveIrScalarTargetOrPrimitive(type, asPrimitiveType = generatePrimitiveTypes)
      is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
    }
  }

  internal fun unwrapFromOptional(typeName: TypeName): TypeName {
    return if (typeName !is ParameterizedTypeName || typeName.rawType != optionalClassName) typeName else typeName.typeArguments.first()
  }

  internal fun isOptional(typeName: TypeName): Boolean {
    return typeName is ParameterizedTypeName && typeName.rawType == optionalClassName
  }

  private fun TypeName.addNullableAnnotation(): TypeName {
    return if (nullableAnnotationClassName == null) {
      this
    } else {
      annotated(AnnotationSpec.builder(nullableAnnotationClassName).build())
    }
  }

  private fun TypeName.addNonNullableAnnotation(): TypeName {
    return if (this in primitiveTypeNames || notNullAnnotationClassName == null) {
      this
    } else {
      annotated(AnnotationSpec.builder(notNullAnnotationClassName).build())
    }
  }

  /**
   * Only keep the annotations that support TYPE_USE targets for use
   * in generics like `List<@NotNull String>` for an example
   */
  private fun TypeName.filterTypeUseAnnotations(): TypeName {
    // Only the JetBrains nullability annotations have a target including ElementType.TYPE_USE
    return if (annotations.isEmpty()) {
      this
    } else {
      withoutAnnotations().annotated(annotations.filter { it.type == JavaClassNames.JetBrainsNullable || it.type == JavaClassNames.JetBrainsNonNull })
    }
  }

  private fun resolveIrScalarTargetOrPrimitive(type: IrScalarType, asPrimitiveType: Boolean): TypeName {
    val target = resolveScalarTarget(type.name)

    if(asPrimitiveType) {
      when(target.toString()) {
        "java.lang.Integer" -> return TypeName.INT
        "java.lang.Boolean" -> return TypeName.BOOLEAN
        "java.lang.Float" -> return TypeName.FLOAT
        "java.lang.Double" -> return TypeName.DOUBLE
      }
    }

    return target
  }

  fun adapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    return if (type.optional) {
      return CodeBlock.of("new $T<>($L)", getOptionalAdapterClassName(), adapterInitializer(type.optional(false), requiresBuffering))
    } else if (type.catchTo != IrCatchTo.NoCatch) {
      error("Java codegen does not support @catch")
    } else if (type.nullable) {
      val initializer = adapterInitializer(type.nullable(false), requiresBuffering)

      val match = Regex("com\\.apollographql\\.apollo\\.api\\.Adapters\\.([a-zA-Z]*)Adapter").matchEntire(initializer.toString())
      if (match != null) {
        nullableAdapterCodeBlock(match.groupValues[1])
      } else {
        CodeBlock.of("new $T<>($L)", getOptionalOrNullableAdapterClassName(), adapterInitializer(type.nullable(false), requiresBuffering))
      }
    } else {
      when (type) {
        is IrListType -> {
          adapterInitializer(type.ofType, requiresBuffering).listAdapter(isComposite = type.ofType.rawType().isComposite())
        }

        is IrScalarType -> {
          scalarAdapterInitializer(type.name)
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

        is IrObjectType -> error("IrObjectType cannot be adapted")
      }
    }
  }

  fun resolveScalarTarget(name: String): TypeName {
    val custom = scalarTargets[name] ?: upstreamScalarTargets[name]
    check(custom != null) {
      "Cannot resolve scalar target for '$name'"
    }

    return parseType(custom) ?: error("Cannot parse type '$custom'")
  }

  fun registerScalarTarget(name: String, target: String) {
    scalarTargets.put(name, target)
  }

  fun registerScalarIsUserDefined(name: String) {
    scalarIsUserDefined.put(name, true)
  }

  fun isScalarUserDefined(name: String): Boolean {
    val custom = scalarIsUserDefined[name] ?: upstreamScalarIsUserDefined[name]
    if(custom != null) {
      return custom
    }

    return false
  }

  fun resolveCompiledType(name: String): CodeBlock {
    return CodeBlock.of("$T.$type", resolveAndAssert(ResolverKeyKind.SchemaType, name))
  }

  private fun scalarAdapterInitializer(name: String): CodeBlock {
    val adapterInitializer = resolveScalarAdapterInitializer(name)
    return if (adapterInitializer == null) {
      val target = resolveScalarTarget(name)
      CodeBlock.of(
          "($customScalarAdapters.<$T>responseAdapterFor($L))",
          target,
          resolveCompiledType(name)
      )
    } else {
      adapterInitializer
    }
  }

  private fun nullableAdapterCodeBlock(typeName: String): CodeBlock {
    val className: ClassName
    val adapterNamePrefix: String
    when (nullableFieldStyle) {
      JavaNullable.APOLLO_OPTIONAL -> {
        // Ex: Adapters.ApolloOptionalStringAdapter
        className = JavaClassNames.Adapters
        adapterNamePrefix = "ApolloOptional"
      }

      JavaNullable.JAVA_OPTIONAL, JavaNullable.GUAVA_OPTIONAL -> {
        // Ex: OptionalAdapters.OptionalStringAdapter
        className = resolveJavaOptionalAdapters()
        adapterNamePrefix = "Optional"
      }

      else -> {
        // Ex: Adapters.NullableStringAdapter
        className = JavaClassNames.Adapters
        adapterNamePrefix = "Nullable"
      }
    }
    val adapterName = "$adapterNamePrefix${typeName}Adapter"
    return CodeBlock.of("$T.$L", className, adapterName)
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

  fun entries() = classNames.map { ResolverEntry(it.key, ResolverClassName(it.value.packageName(), it.value.simpleNames())) }
  fun resolveSchemaType(name: String) = resolveAndAssert(ResolverKeyKind.SchemaType, name)
  fun registerSchemaType(name: String, className: ClassName) = register(ResolverKeyKind.SchemaType, name, className)
  fun registerModel(path: String, className: ClassName) = register(ResolverKeyKind.Model, path, className)

  internal fun resolveIrType2(type: IrType2): TypeName {
    return when (type) {
      is IrNonNullType2 -> resolveIrType2(type.ofType)
      is IrListType2 -> ParameterizedTypeName.get(JavaClassNames.List, resolveIrType2(type.ofType))
      is IrCompositeType2 -> mapTypes.get(type.name) ?: error("Cannot find map type for ${type.name}")
      is IrEnumType2 -> resolveIrType(IrEnumType(type.name, nullable = true))
      is IrScalarType2 -> resolveIrType(IrScalarType(type.name, nullable = true))
    }
  }

  internal fun registerMapType(name: String, className: ClassName) {
    mapTypes.put(name, className)
  }

  internal fun adapterInitializer2(type: IrType2): CodeBlock? {
    if (type !is IrNonNullType2) {
      return adapterInitializer2(IrNonNullType2(type))
    }
    return nonNullableAdapterInitializer2(type.ofType)
  }

  private fun nonNullableAdapterInitializer2(type: IrType2): CodeBlock? {
    return when (type) {
      is IrNonNullType2 -> error("")
      is IrListType2 -> adapterInitializer2(type.ofType)?.listAdapter(isComposite = type.ofType.isCompositeOrWrappedComposite())
      is IrScalarType2 -> {
        if (isScalarUserDefined(type.name)) {
          scalarAdapterInitializer(type.name)
        } else {
          null
        }
      }

      is IrEnumType2 -> {
        adapterInitializer(IrEnumType(type.name), false)
      }

      is IrCompositeType2 -> null
    }
  }

  private fun CodeBlock.listAdapter(isComposite: Boolean): CodeBlock {
    return CodeBlock.of(
        "new $T<>($L)",
        if (!isComposite) {
          JavaClassNames.ListAdapter
        } else {
          JavaClassNames.ListAdapter
        },
        this
    )
  }

  fun registerSchema(className: ClassName) = register(ResolverKeyKind.Schema, "", className)
  fun resolveSchema(): ClassName = resolveAndAssert(ResolverKeyKind.Schema, "")

  fun registerArgumentDefinition(id: String, className: ClassName) = register(ResolverKeyKind.ArgumentDefinition, id, className)
  fun resolveArgumentDefinition(id: String): ClassName = resolveAndAssert(ResolverKeyKind.ArgumentDefinition, id)

  fun registerJavaOptionalAdapter(className: ClassName) = register(ResolverKeyKind.JavaOptionalAdapter, "", className)
  fun resolveJavaOptionalAdapter() = resolveAndAssert(ResolverKeyKind.JavaOptionalAdapter, "")

  fun registerJavaOptionalAdapters(className: ClassName) = register(ResolverKeyKind.JavaOptionalAdapters, "", className)
  fun resolveJavaOptionalAdapters() = resolveAndAssert(ResolverKeyKind.JavaOptionalAdapters, "")
  fun registerScalarAdapter(name: String, adapter: String) {
    this@JavaResolver.scalarAdapters[name] = adapter
  }
  fun resolveScalarAdapterInitializer(name: String): CodeBlock? {
    val customAdapter = this@JavaResolver.scalarAdapters[name] ?: upstreamScalarAdapters[name]
    if (customAdapter != null) {
      val match = Regex("com\\.apollographql\\.apollo\\.api\\.Adapters\\.([a-zA-Z]*)Adapter").matchEntire(customAdapter)
      if (match != null) {
        // Make the generated code look a bit nicer in case this is one of the built-in adapters
        return CodeBlock.of("$T.${match.groupValues[1]}Adapter", JavaClassNames.Adapters)
      }

      return CodeBlock.of(L, customAdapter)
    }

    return null
  }

  fun toCodegenMetadata(targetLanguage: TargetLanguage): CodegenMetadata {
    val entries = classNames.map { ResolverEntry(it.key, ResolverClassName(it.value.packageName(), it.value.simpleNames())) }
    return CodegenMetadata(
        entries = entries,
        version = CODEGEN_METADATA_VERSION,
        targetLanguage = targetLanguage,
        inlineProperties = emptyMap(),
        scalarTargets = scalarTargets,
        scalarAdapters = this@JavaResolver.scalarAdapters,
        scalarIsUserDefined = this@JavaResolver.scalarIsUserDefined
    )
  }
}


internal fun ResolverClassName.toJavaPoetClassName(): ClassName =
  ClassName.get(packageName, simpleNames[0], *simpleNames.drop(1).toTypedArray())


private val primitiveTypeNames = setOf(TypeName.DOUBLE, TypeName.INT, TypeName.BOOLEAN)

internal fun TypeName.boxIfPrimitiveType(): TypeName {
  return if (this in primitiveTypeNames) this.box() else this
}
