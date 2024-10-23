package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.Builder
import com.apollographql.apollo.compiler.codegen.Identifier.__typename
import com.apollographql.apollo.compiler.codegen.Identifier.block
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.factory
import com.apollographql.apollo.compiler.codegen.Identifier.typename
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinResolver
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.newBuilderFunSpec
import com.apollographql.apollo.compiler.ir.IrCompositeType2
import com.apollographql.apollo.compiler.ir.IrMapProperty
import com.apollographql.apollo.compiler.ir.IrNonNullType2
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName

/**
 *     public fun Data(resolver: FakeResolver = DefaultFakeResolver(__Schema.all),
 *         block: QueryBuilder.() -> Unit = {}): Data = buildData(
 *       GetAliasesQuery_ResponseAdapter.Data,
 *       GetAliasesQuerySelections.__root,
 *       "Query",
 *       GlobalBuilder.buildQuery(block),
 *       resolver,
 *       __CustomScalarAdapters,
 *     )
 *
 *  @param builderFactoryParameterRequired pass true for abstract (unions & interfaces) types
 *  where the user needs to pass the concrete type as a parameter
 */
internal fun dataBuilderCtor(
    context: KotlinContext,
    modelId: String,
    selectionsClassName: ClassName,
    typename: String,
    builderFactoryParameterRequired: Boolean,
): FunSpec {
  val mapVariable = TypeVariableName(name = "M", bounds = listOf(context.resolver.resolveMapType(typename)))
  val builderBound = KotlinSymbols.ObjectBuilder.parameterizedBy(mapVariable)
  val builderTypeVariable = TypeVariableName(name = Builder, bounds = listOf(builderBound))
  val builderTypeName: TypeName = if (builderFactoryParameterRequired) {
    builderTypeVariable
  } else {
    context.resolver.resolveBuilderType(typename)
  }
  val builderFactoryClassName: TypeName = KotlinSymbols.BuilderFactory.parameterizedBy(builderTypeVariable)
  return FunSpec.builder(Identifier.Data)
      .apply {
        if (builderFactoryParameterRequired) {
          addTypeVariable(mapVariable)
          addTypeVariable(builderTypeVariable)
          addParameter(ParameterSpec.builder(factory, builderFactoryClassName).build())
        }
      }
      .addParameter(
          ParameterSpec.builder(
              Identifier.resolver,
              KotlinSymbols.FakeResolver
          ).defaultValue(
              CodeBlock.of("%T(%T.all)", KotlinSymbols.DefaultFakeResolver, context.resolver.resolveSchema())
          ).build()
      ).addParameter(
          ParameterSpec.builder(
              block,
              LambdaTypeName.get(
                  receiver = builderTypeName,
                  parameters = emptyArray<TypeName>(),
                  returnType = KotlinSymbols.Unit
              )
          ).defaultValue(CodeBlock.of("{}"))
              .build()
      )
      .addCode(
          CodeBlock.builder()
              .add("return %M(\n", KotlinSymbols.buildData)
              .indent()
              .apply {
                if (builderFactoryParameterRequired) {
                  add("$factory,\n")
                } else {
                  val typeClassName: ClassName = context.resolver.resolveSchemaType(typename)
                  add("%T,\n", typeClassName)
                }
              }
              .add("$block,\n")
              .add("%T,\n", context.resolver.resolveModelAdapter(modelId))
              .add("%T.${Identifier.root},\n", selectionsClassName)
              .add("%S,\n", typename)
              .add("${Identifier.resolver},\n")
              .add("%T,\n", context.resolver.resolveCustomScalarAdapters())
              .unindent()
              .add(")\n")
              .build()
      )
      .returns(context.resolver.resolveModel(modelId))
      .build()
}

internal fun TypeSpec.Builder.maybeImplementBuilderFactory(generateDataBuilders: Boolean, builderClassName: ClassName): TypeSpec.Builder = apply {
  if (!generateDataBuilders) return@apply

  addSuperinterface(KotlinSymbols.BuilderFactory.parameterizedBy(builderClassName))
  addFunction(newBuilderFunSpec(builderClassName))
}

internal fun topLevelBuildFunSpec(
    funName: String,
    builderClassName: ClassName,
    mapClassName: ClassName,
    requiresTypename: Boolean
): FunSpec {
  return FunSpec.builder(funName)
      .returns(mapClassName)
      .apply {
        if (requiresTypename) {
          addParameter(typename, String::class)
        }
      }
      .addParameter(
          ParameterSpec.builder(
              block,
              LambdaTypeName.get(
                  receiver = builderClassName,
                  parameters = emptyArray<TypeName>(),
                  returnType = KotlinSymbols.Unit
              )
          ).build()
      )
      .receiver(KotlinSymbols.BuilderScope)
      .addCode(
          CodeBlock.builder()
              .add("return %T(${customScalarAdapters}).apply($block)", builderClassName)
              .apply {
                if (requiresTypename) {
                  add(".apply { __typename = typename }")
                }
              }
              .add(".build()")
              .build()
      )
      .build()
}

internal fun abstractMapTypeSpec(resolver: KotlinResolver, name: String, implements: List<String>): TypeSpec {
  return TypeSpec
      .interfaceBuilder(name)
      .addSuperinterfaces(implements.map { resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(it))) })
      .addSuperinterface(superinterface = KotlinSymbols.MapOfStringToNullableAny)
      .build()
}

internal fun concreteMapTypeSpec(
    resolver: KotlinResolver,
    mapName: String,
    implements: List<String>,
    extendsFromType: String?,
): TypeSpec {
  return TypeSpec
      .classBuilder(mapName)
      .primaryConstructor(
          FunSpec.constructorBuilder()
              .addParameter(
                  ParameterSpec.builder(
                      Identifier.__fields,
                      KotlinSymbols.MapOfStringToNullableAny
                  ).build()
              )
              .build()
      )
      .addSuperinterfaces(implements.map { resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(it))) })
      .apply {
        if (extendsFromType != null) {
          // We are an "other" concrete type
          addSuperinterface(resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(extendsFromType))))
        }
      }
      .addSuperinterface(
          superinterface = KotlinSymbols.MapOfStringToNullableAny,
          delegate = CodeBlock.of(Identifier.__fields)
      )
      .build()
}

internal fun concreteBuilderTypeSpec(
    context: KotlinContext,
    packageName: String,
    builderName: String,
    mapName: String,
    properties: List<IrMapProperty>,
    typename: String?,
): TypeSpec {
  return TypeSpec
      .classBuilder(builderName)
      .superclass(KotlinSymbols.ObjectBuilder.parameterizedBy(ClassName(packageName, mapName)))
      .addSuperclassConstructorParameter(CodeBlock.of(customScalarAdapters))
      .primaryConstructor(
          FunSpec.constructorBuilder()
              .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
              .build()
      )
      .addProperties(properties.map { it.toPropertySpec(context) })
      .addFunction(buildFunSpec(packageName, mapName))
      .apply {
        if (typename != null) {
          addInitializerBlock(CodeBlock.of("$__typename = %S", typename))
        }
      }
      .build()
}

private fun IrMapProperty.toPropertySpec(context: KotlinContext): PropertySpec {
  return PropertySpec.builder(context.layout.propertyName(name), context.resolver.resolveIrType2(type))
      .mutable(true)
      .apply {
        val initializer = context.resolver.adapterInitializer2(type, context.jsExport)
        if (initializer == null) {
          // Composite or no mapping registered (Int/Boolean/String/...)
          delegate(CodeBlock.of(Identifier.__fields))
        } else {
          delegate(CodeBlock.of("%T(%L)", KotlinSymbols.BuilderProperty, initializer))
        }
      }
      .build()
}

private fun buildFunSpec(packageName: String, mapName: String): FunSpec {
  val mapClassName = ClassName(packageName, mapName)
  return FunSpec.builder(Identifier.build)
      .returns(mapClassName)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(
          CodeBlock.builder()
              .addStatement("return %T(${Identifier.__fields})", mapClassName)
              .build()
      )
      .build()
}
