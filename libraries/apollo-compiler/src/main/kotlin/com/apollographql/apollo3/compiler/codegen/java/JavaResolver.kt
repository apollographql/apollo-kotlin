package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.scalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.java.adapter.singletonAdapterInitializer
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
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
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName


internal class JavaResolver(
    entries: List<ResolverEntry>,
    val next: JavaResolver?,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val generatePrimitiveTypes: Boolean,
    private val nullableFieldStyle: JavaNullable,
    private val hooks: ApolloCompilerJavaHooks,
) {

  private val optionalClassName: ClassName = when (nullableFieldStyle) {
    JavaNullable.JAVA_OPTIONAL -> JavaClassNames.JavaOptional
    JavaNullable.GUAVA_OPTIONAL -> JavaClassNames.GuavaOptional
    else -> JavaClassNames.Optional
  }

  private val optionalAdapterClassName: ClassName = when (nullableFieldStyle) {
    JavaNullable.JAVA_OPTIONAL -> JavaClassNames.JavaOptionalAdapter
    JavaNullable.GUAVA_OPTIONAL -> JavaClassNames.GuavaOptionalAdapter
    else -> JavaClassNames.ApolloOptionalAdapter
  }

  private val optionalOrNullableAdapterClassName: ClassName = when (nullableFieldStyle) {
    JavaNullable.APOLLO_OPTIONAL -> JavaClassNames.ApolloOptionalAdapter
    JavaNullable.JAVA_OPTIONAL -> JavaClassNames.JavaOptionalAdapter
    JavaNullable.GUAVA_OPTIONAL -> JavaClassNames.GuavaOptionalAdapter
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


  fun resolve(key: ResolverKey): ClassName? = hooks.overrideResolvedType(key, classNames[key] ?: next?.resolve(key))

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
      return if (generatePrimitiveTypes && type.ofType is IrScalarType) {
        resolveIrScalarType(type.ofType, asPrimitiveType = true)
      } else {
        resolveIrType(type.ofType).let { if (wrapNullableFieldsInOptional) unwrapFromOptional(it) else it.withoutAnnotations() }
      }.addNonNullableAnnotation()
    }

    return when (type) {
      is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
      is IrOptionalType -> resolveIrType(type.ofType).boxIfPrimitiveType().wrapInOptional()
      is IrListType -> ParameterizedTypeName.get(JavaClassNames.List, resolveIrType(type.ofType).filterTypeUseAnnotations().boxIfPrimitiveType())
      is IrModelType -> resolveAndAssert(ResolverKeyKind.Model, type.path)
      is IrScalarType -> resolveIrScalarType(type, asPrimitiveType = false)
      is IrNamedType -> resolveAndAssert(ResolverKeyKind.SchemaType, type.name)
    }.let { if (wrapNullableFieldsInOptional) it.wrapInOptional() else it.addNullableAnnotation() }
  }

  private fun TypeName.wrapInOptional(): TypeName {
    return ParameterizedTypeName.get(optionalClassName, this.filterTypeUseAnnotations())
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

  private fun resolveIrScalarType(type: IrScalarType, asPrimitiveType: Boolean): TypeName {
    // Try mapping first, then built-ins, then fallback to Object
    return resolveScalarTarget(type.name) ?: when (type.name) {
      "String" -> JavaClassNames.String
      "ID" -> JavaClassNames.String
      "Float" -> if (asPrimitiveType) TypeName.DOUBLE else JavaClassNames.Double
      "Int" -> if (asPrimitiveType) TypeName.INT else JavaClassNames.Integer
      "Boolean" -> if (asPrimitiveType) TypeName.BOOLEAN else JavaClassNames.Boolean
      else -> JavaClassNames.Object
    }
  }

  fun adapterInitializer(type: IrType, requiresBuffering: Boolean): CodeBlock {
    if (type !is IrNonNullType) {
      // Don't hardcode the adapter when the scalar is mapped to a user-defined type
      val scalarWithoutCustomMapping = type is IrScalarType && !scalarMapping.containsKey(type.name)
      return when {
        type is IrScalarType && type.name == "String" && scalarWithoutCustomMapping -> scalarAdapterCodeBlock("String")
        type is IrScalarType && type.name == "ID" && scalarWithoutCustomMapping -> scalarAdapterCodeBlock("String")
        type is IrScalarType && type.name == "Boolean" && scalarWithoutCustomMapping -> scalarAdapterCodeBlock("Boolean")
        type is IrScalarType && type.name == "Int" && scalarWithoutCustomMapping -> scalarAdapterCodeBlock("Int")
        type is IrScalarType && type.name == "Float" && scalarWithoutCustomMapping -> scalarAdapterCodeBlock("Double")
        type is IrScalarType && resolveScalarTarget(type.name) == null -> {
          adapterCodeBlock("NullableAnyApolloAdapter")
        }

        else -> {
          CodeBlock.of("new $T<>($L)", optionalOrNullableAdapterClassName, adapterInitializer(IrNonNullType(type), requiresBuffering))
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
        CodeBlock.of("new $T<>($L)", optionalAdapterClassName, adapterInitializer(type.ofType, requiresBuffering))
      }
    }
  }

  private fun nonNullableScalarAdapterInitializer(type: IrScalarType): CodeBlock {
    return when (val adapterInitializer = scalarMapping[type.name]?.adapterInitializer) {
      is ExpressionAdapterInitializer -> {
        CodeBlock.of(
            "new $T<>($L)",
            JavaClassNames.ScalarAdapterToApolloAdapter,
            CodeBlock.of(adapterInitializer.expression)
        )
      }

      is RuntimeAdapterInitializer -> {
        val target = resolveScalarTarget(type.name)
        CodeBlock.of(
            "(${Identifier.context}.$scalarAdapters.<$T>responseAdapterFor($L))",
            target,
            resolveCompiledType(type.name)
        )
      }

      else -> {
        when (type.name) {
          "Boolean" -> adapterCodeBlock("BooleanApolloAdapter")
          "ID" -> adapterCodeBlock("StringApolloAdapter")
          "String" -> adapterCodeBlock("StringApolloAdapter")
          "Int" -> adapterCodeBlock("IntApolloAdapter")
          "Float" -> adapterCodeBlock("DoubleApolloAdapter")
          else -> {
            val target = resolveScalarTarget(type.name)
            if (target == null) {
              adapterCodeBlock("AnyApolloAdapter")
            } else {
              CodeBlock.of(
                  "(${Identifier.context}.$scalarAdapters.<$T>responseAdapterFor($L))",
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
  private fun scalarAdapterCodeBlock(typeName: String): CodeBlock {
    val className: ClassName
    val adapterNamePrefix: String
    when (nullableFieldStyle) {
      JavaNullable.APOLLO_OPTIONAL -> {
        // Ex: Adapters.ApolloOptionalStringAdapter
        className = JavaClassNames.Adapters
        adapterNamePrefix = "ApolloOptional"
      }

      JavaNullable.JAVA_OPTIONAL -> {
        // Ex: JavaOptionalAdapters.JavaOptionalStringAdapter
        className = JavaClassNames.JavaOptionalAdapters
        adapterNamePrefix = "JavaOptional"
      }

      JavaNullable.GUAVA_OPTIONAL -> {
        // Ex: GuavaOptionalAdapters.GuavaOptionalStringAdapter
        className = JavaClassNames.GuavaOptionalAdapters
        adapterNamePrefix = "GuavaOptional"
      }

      else -> {
        // Ex: Adapters.NullableStringApolloAdapter
        className = JavaClassNames.Adapters
        adapterNamePrefix = "Nullable"
      }
    }
    val adapterName = "$adapterNamePrefix${typeName}ApolloAdapter"
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
  fun resolveSchema(): ClassName = resolveAndAssert(ResolverKeyKind.Schema, "")
}


internal fun ResolverClassName.toJavaPoetClassName(): ClassName = ClassName.get(packageName, simpleNames[0], *simpleNames.drop(1).toTypedArray())


private val primitiveTypeNames = setOf(TypeName.DOUBLE, TypeName.INT, TypeName.BOOLEAN)

internal fun TypeName.boxIfPrimitiveType(): TypeName {
  return if (this in primitiveTypeNames) this.box() else this
}
