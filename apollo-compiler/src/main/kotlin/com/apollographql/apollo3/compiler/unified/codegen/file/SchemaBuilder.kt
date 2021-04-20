package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.api.Interface
import com.apollographql.apollo3.api.Object
import com.apollographql.apollo3.api.Union
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.ir.IrCustomScalar
import com.apollographql.apollo3.compiler.unified.ir.IrInterface
import com.apollographql.apollo3.compiler.unified.ir.IrObject
import com.apollographql.apollo3.compiler.unified.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class SchemaBuilder(
    private val context: CgContext,
    private val customScalars: List<IrCustomScalar>,
    private val objects: List<IrObject>,
    private val interfaces: List<IrInterface>,
    private val unions: List<IrUnion>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  override fun prepare() {
    customScalars.forEach {
      context.resolver.registerCustomScalar(
          it.name,
          it.kotlinName
      )
      context.resolver.registerCustomScalarConst(
          it.name,
          MemberName(
              ClassName(
                  packageName,
                  layout.customScalarsName()
              ),
              layout.customScalarName(it.name)
          )
      )
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = layout.schemaFileName(),
        typeSpecs = listOf(customScalarsTypeSpec(), objectsTypeSpec(), interfacesTypeSpec(), unionsTypeSpec())
    )
  }

  private fun customScalarsTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(layout.customScalarsName())
        .addKdoc("Auto generated constants for custom scalars. Use them to register your [ResponseAdapter]s")
        .addProperties(
            customScalars.map {
              /**
               * Custom Scalars without a mapping will generate code using [AnyResponseAdapter] directly
               * so the fallback isn't really required here. We still write it as a way to hint the user
               * to what's happening behind the scenes
               */
              val kotlinName = it.kotlinName ?: "kotlin.Any"
              PropertySpec
                  .builder(layout.customScalarName(it.name), CustomScalar::class)
                  .maybeAddDescription(it.description)
                  .maybeAddDeprecation(it.deprecationReason)
                  .applyIf(it.kotlinName == null) {
                    addKdoc("\n\nNo mapping was registered for this custom scalar.")
                  }
                  .initializer("%T(%S, %S)", CustomScalar::class.asTypeName(), it.name, kotlinName)
                  .build()
            }
        )
        .build()
  }

  private fun objectsTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(context.layout.objectName(context.layout.objectsName()))
        .addProperties(objects.map { it.propertySpec() })
        .build()
  }

  private fun IrObject.propertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add(implements.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.interfacesName()}.${context.layout.interfaceName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.objectName(name), Object::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", Object::class.asTypeName(), name, builder.build())
        .build()

  }

  private fun interfacesTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(context.layout.interfacesName())
        .addProperties(interfaces.map { it.propertySpec() })
        .build()
  }

  private fun IrInterface.propertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add(implements.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.interfacesName()}.${context.layout.interfaceName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.interfaceName(name), Interface::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", Interface::class.asTypeName(), name, builder.build())
        .build()
  }

  private fun unionsTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(context.layout.unionsName())
        .addProperties(unions.map { it.propertySpec() })
        .build()
  }

  private fun IrUnion.propertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add(members.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.objectsName()}.${context.layout.objectName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.interfaceName(name), Union::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", Union::class.asTypeName(), name, builder.build())
        .build()
  }
}
