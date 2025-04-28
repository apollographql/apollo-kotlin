package com.apollographql.apollo.compiler.codegen.kotlin.builders

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.ClassNames
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.Data
import com.apollographql.apollo.compiler.codegen.Identifier.__typename
import com.apollographql.apollo.compiler.codegen.Identifier.block
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.resolver
import com.apollographql.apollo.compiler.codegen.Identifier.typename
import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.dataBuilderName
import com.apollographql.apollo.compiler.codegen.dataMapName
import com.apollographql.apollo.compiler.codegen.dataName
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinDataBuilderContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.builderPackageName
import com.apollographql.apollo.compiler.ir.IrDataBuilder
import com.apollographql.apollo.compiler.ir.IrMapProperty
import com.squareup.kotlinpoet.AnnotationSpec
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
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.withIndent

internal class DataBuilderBuilder(
    private val context: KotlinDataBuilderContext,
    private val dataBuilder: IrDataBuilder,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val builderClassName = ClassName(packageName, dataBuilderName(dataBuilder.name, dataBuilder.isAbstract, layout))
  private val mapClassName = ClassName(packageName, dataMapName(dataBuilder.name, dataBuilder.isAbstract, layout))
  private val buildFunName = "build${dataName(dataBuilder.name, dataBuilder.isAbstract, layout)}"
  private val requiresTypename = dataBuilder.isAbstract

  override fun prepare() {}

  override fun build(): CgFile {
    val topLevelFunctions = buildList {
      add(topLevelBuildWithReceiverFunSpec())

      val schemaType = context.resolver.resolve(ResolverKey(ResolverKeyKind.SchemaType, dataBuilder.name))
      if (schemaType != null) {
        /**
         * Only add the extensions for types that are actually used in the fragments/operations.
         */
        val bound = if (dataBuilder.operationType != null) {
          ClassName(ClassNames.apolloApiPackageName, dataBuilder.operationType.capitalizeFirstLetter(), Data)
        } else {
          schemaType.nestedClass(Data)
        }
        add(topLevelDataFunSpec(bound, dataBuilder.isAbstract, false))
        add(topLevelDataFunSpec(bound, dataBuilder.isAbstract, true))
      }
    }
    return CgFile(
        packageName = packageName,
        fileName = builderClassName.simpleName,
        typeSpecs = listOf(typeSpec()),
        funSpecs = topLevelFunctions
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(builderClassName)
        .addAnnotation(AnnotationSpec.builder(KotlinSymbols.DataBuildersDsl).build())
        .superclass(KotlinSymbols.ObjectBuilder.parameterizedBy(mapClassName))
        .addSuperclassConstructorParameter(CodeBlock.of(customScalarAdapters))
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
                .build()
        )
        .apply {
          if (!dataBuilder.isAbstract) {
            /**
             * If this is a concrete type, initialize typename directly. If not, the user must provide it
             */
            addInitializerBlock(buildCodeBlock {
              add("__typename = %S", dataBuilder.name)
            })
          }
        }
        .addProperties(dataBuilder.properties.map { it.toPropertySpec(context) })
        .addFunction(buildFunSpec(mapClassName))
        .addType(dataBuilderFactoryCompanion())
        .build()
  }

  /**
   *   ```
   *   companion object : DataBuilderFactory<OtherAnimalBuilder> {
   *     override fun newBuilder(customScalarAdapters: CustomScalarAdapters): OtherAnimalBuilder {
   *       return OtherAnimalBuilder(customScalarAdapters)
   *     }
   *   }
   *   ```
   */
  private fun dataBuilderFactoryCompanion(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addSuperinterface(KotlinSymbols.DataBuilderFactory.parameterizedBy(builderClassName))
        .addFunction(
            FunSpec.builder("newBuilder")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(customScalarAdaptersParameter(false))
                .returns(builderClassName)
                .addCode("return %T($customScalarAdapters)", builderClassName)
                .build()
        )
        .build()
  }

  internal fun topLevelBuildWithReceiverFunSpec(): FunSpec {
    return FunSpec.builder(buildFunName)
        .returns(mapClassName)
        .apply {
          if (requiresTypename) {
            addParameter(typename, KotlinSymbols.String)
          }
        }
        .addParameter(blockParameter(builderClassName, false))
        .receiver(KotlinSymbols.DataBuilderScope)
        .addCode(
            CodeBlock.builder()
                .add("return %T(${customScalarAdapters})\n", builderClassName)
                .add(".apply {\n")
                .withIndent {
                  if (requiresTypename) {
                    add("$__typename = typename\n")
                  } else {
                    add("$__typename = %S\n", dataBuilder.name)
                  }
                }
                .add("}.apply($block)\n")
                .add(".build()")
                .build()
        )
        .build()
  }

  internal fun topLevelDataFunSpec(bound: ClassName, requiresBuilderFactory: Boolean, withResolver: Boolean): FunSpec {
    val mapTypeVariable = TypeVariableName("Map", listOf(ClassName(packageName, dataMapName(dataBuilder.name, false, layout)), KotlinSymbols.DataMap))
    val builderTypeVariable = TypeVariableName("Builder", KotlinSymbols.DataBuilder.parameterizedBy(mapTypeVariable))
    val dataTypeVariable = TypeVariableName("D", bound)
    val blockReceiver = if (requiresBuilderFactory) {
      builderTypeVariable
    } else {
      builderClassName
    }
    return FunSpec.builder(Data)
        .returns(dataTypeVariable)
        .addTypeVariable(dataTypeVariable)
        .receiver(KotlinSymbols.ExecutableDefinition.parameterizedBy(dataTypeVariable))
        .apply {
          if (requiresBuilderFactory) {
            addTypeVariable(mapTypeVariable)
            addTypeVariable(builderTypeVariable)
            addParameter(factoryParameter(builderTypeVariable))
          }
        }
        .apply {
          if (withResolver) {
            addParameter(ParameterSpec.builder("resolver", KotlinSymbols.FakeResolver).build())
          }
        }
        .addParameter(customScalarAdaptersParameter(true))
        .addParameter(blockParameter(blockReceiver = blockReceiver, withDefault = withResolver))
        .addCode(
            buildCodeBlock {
              add("return %M(\n", KotlinSymbols.buildData)
              withIndent {
                add("ADAPTER,\n")
                add("$customScalarAdapters,\n")
                if (requiresBuilderFactory) {
                  add("factory.newBuilder")
                } else {
                  add("%T", builderClassName)
                }
                add("($customScalarAdapters).apply($block).build(),\n")
                if (withResolver) {
                  add("ROOT_FIELD.selections,\n")
                  /**
                   * This is not 100% correct because buildData() expects a concrete typename
                   * but this may be an interface name. It's not clear how much of a problem this is.
                   */
                  add("%S,\n", dataBuilder.name)
                  add("$resolver,\n")
                }
              }
              add(")\n")
            }
        )

        .build()
  }

  private fun factoryParameter(builderTypeVariable: TypeVariableName): ParameterSpec {
    return ParameterSpec.builder("factory", KotlinSymbols.DataBuilderFactory.parameterizedBy(builderTypeVariable)).build()
  }

  private fun blockParameter(blockReceiver: TypeName, withDefault: Boolean) = ParameterSpec.builder(
      block,
      LambdaTypeName.get(
          receiver = blockReceiver,
          parameters = emptyArray<TypeName>(),
          returnType = KotlinSymbols.Unit
      )
  ).apply {
    if (withDefault) {
      defaultValue("{}")
    }
  }.build()
}

private fun customScalarAdaptersParameter(withDefault: Boolean) = ParameterSpec.builder(
    customScalarAdapters,
    KotlinSymbols.CustomScalarAdapters
).apply {
  if (withDefault) {
    defaultValue("%T", KotlinSymbols.CustomScalarAdaptersEmpty)
  }
}.build()

private fun buildFunSpec(mapClassName: ClassName): FunSpec {
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

