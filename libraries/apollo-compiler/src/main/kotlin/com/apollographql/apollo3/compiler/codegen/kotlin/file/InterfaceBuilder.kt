package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrCompositeType2
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrMapProperty
import com.apollographql.apollo3.compiler.ir.IrNonNullType2
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class InterfaceBuilder(
    private val context: KotlinContext,
    private val iface: IrInterface,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(iface.name)

  override fun prepare() {
    context.resolver.registerSchemaType(iface.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(iface.name, ClassName(packageName, layout.objectMapName(iface.name)))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(iface.typeSpec())
          if (generateDataBuilders) {
            add(iface.builderTypeSpec())
            add(iface.mapTypeSpec())
            add(iface.unknownMapTypeSpec())
          }
        },
        funSpecs = mutableListOf<FunSpec>().apply {
          if (generateDataBuilders) {
            add(iface.builderFunSpec())
          }
        }
    )
  }

  private fun IrInterface.builderTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(layout.unknownBuilderName(name))
        .superclass(KotlinSymbols.ObjectBuilder)
        .addSuperclassConstructorParameter(CodeBlock.of(Identifier.customScalarAdapters))
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
                .build()
        )
        .addProperties(mapProperties.map { it.toPropertySpec() })
        .addFunction(buildFunSpec())
        .build()
  }

  private fun IrMapProperty.toPropertySpec(): PropertySpec {
    return PropertySpec.builder(layout.propertyName(name), context.resolver.resolveIrType2(type))
        .mutable(true)
        .apply {
          val initializer = context.resolver.adapterInitializer2(type)
          if (initializer == null) {
            // Composite or no mapping registered (Int/Boolean/String/...)
            delegate(CodeBlock.of(Identifier.__fields))
          } else {
            delegate(CodeBlock.of("%T(%L)", KotlinSymbols.BuilderProperty, initializer))
          }
        }
        .build()
  }

  private fun IrInterface.buildFunSpec(): FunSpec {
    val mapClassName = ClassName(packageName, layout.unknownMapName(name))
    return FunSpec.builder(Identifier.build)
        .returns(mapClassName)
        .addCode(
            CodeBlock.builder()
                .addStatement("return·%T(${Identifier.__fields})", mapClassName)
                .build()
        )
        .build()
  }

  private fun IrInterface.unknownMapTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(layout.unknownMapName(name))
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
        .addSuperinterfaces(implements.map { context.resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(it))) })
        .addSuperinterface(context.resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(name))))
        .addSuperinterface(
            superinterface = KotlinSymbols.MapOfStringToNullableAny,
            delegate = CodeBlock.of(Identifier.__fields)
        )
        .build()
  }

  private fun IrInterface.builderFunSpec(): FunSpec {
    val builderClassName = ClassName(packageName, layout.unknownBuilderName(name))
    val mapClassName = ClassName(packageName, layout.unknownMapName(name))
    return FunSpec.builder(layout.unknownBuilderFunName(name))
        .returns(mapClassName)
        .addParameter(Identifier.__typename, String::class)
        .addParameter(
            ParameterSpec.builder(
                Identifier.block,
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
                .addStatement("val·builder·=·%T(${Identifier.customScalarAdapters})", builderClassName)
                .addStatement("builder.__typename·=·__typename")
                .addStatement("builder.${Identifier.block}()")
                .addStatement("return·builder.build()")
                .build()
        )
        .build()
  }

  private fun IrInterface.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .interfaceBuilder(layout.objectMapName(name))
        .addSuperinterfaces(implements.map { context.resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(it))) })
        .build()
  }

  private fun IrInterface.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrInterface.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .build()
  }
}
