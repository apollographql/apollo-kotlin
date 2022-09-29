package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols.MapOfStringToNullableAny
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrMapProperty
import com.apollographql.apollo3.compiler.ir.IrCompositeType2
import com.apollographql.apollo3.compiler.ir.IrNonNullType2
import com.apollographql.apollo3.compiler.ir.IrObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class ObjectBuilder(
    private val context: KotlinContext,
    private val obj: IrObject,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(obj.name)

  override fun prepare() {
    context.resolver.registerSchemaType(obj.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(obj.name, ClassName(packageName, layout.mapName(obj.name)))
    context.resolver.registerBuilderType(obj.name, ClassName(packageName, layout.objectBuilderName(obj.name)))
    context.resolver.registerBuilderFun(obj.name, MemberName(packageName, layout.builderFunName(obj.name)))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(obj.typeSpec())
          if (generateDataBuilders) {
            add(obj.builderTypeSpec())
            add(obj.mapTypeSpec())
          }
        },
        funSpecs = mutableListOf<FunSpec>().apply {
          if (generateDataBuilders) {
            add(obj.builderFunSpec())
          }
        }

    )
  }

  private fun IrObject.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(layout.mapName(name))
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder(
                        Identifier.__fields,
                        MapOfStringToNullableAny
                    ).build()
                )
                .build()
        )
        .addSuperinterfaces(superTypes.map { context.resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(it))) })
        .addSuperinterface(
            superinterface = MapOfStringToNullableAny,
            delegate = CodeBlock.of(Identifier.__fields)
        )
        .build()
  }

  private fun IrObject.builderTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(layout.objectBuilderName(name))
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

  private fun IrObject.builderFunSpec(): FunSpec {
    val builderClassName = ClassName(packageName, layout.objectBuilderName(name))
    val mapClassName = ClassName(packageName, layout.mapName(obj.name))
    return FunSpec.builder(layout.builderFunName(obj.name))
        .returns(mapClassName)
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
                .addStatement("builder.__typename·=·%S", name)
                .addStatement("builder.${Identifier.block}()")
                .addStatement("return·builder.build()")
                .build()
        )
        .build()
  }

  private fun IrObject.buildFunSpec(): FunSpec {
    val mapClassName = ClassName(packageName, layout.mapName(obj.name))
    return FunSpec.builder(Identifier.build)
        .returns(mapClassName)
        .addCode(
            CodeBlock.builder()
                .addStatement("return·%T(${Identifier.__fields})", mapClassName)
                .build()
        )
        .build()
  }

  private fun IrObject.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrObject.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .build()
  }
}
