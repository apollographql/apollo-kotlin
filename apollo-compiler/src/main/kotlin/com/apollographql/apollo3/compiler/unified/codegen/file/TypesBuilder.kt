package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.api.Interface
import com.apollographql.apollo3.api.Object
import com.apollographql.apollo3.api.SchemaType
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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class TypesBuilder(
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
                  layout.typesName()
              ),
              layout.customScalarName(it.name)
          )
      )
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = layout.typesName(),
        typeSpecs = listOf(typeSpec())
    )
  }

  private val properties = mutableMapOf<String, PropertySpec>()

  private fun property(irType: Any): PropertySpec {
    return properties.getOrPut(propertyName(irType)) {
      when (irType) {
        is IrCustomScalar -> irType.propertySpec()
        is IrObject -> irType.propertySpec()
        is IrInterface -> irType.propertySpec()
        is IrUnion -> irType.propertySpec()
        else -> error("")
      }
    }
  }

  private fun propertyName(irType: Any): String {
    return  when (irType) {
      is IrCustomScalar -> layout.customScalarName(irType.name)
      is IrObject -> layout.objectName(irType.name)
      is IrInterface -> layout.interfaceName(irType.name)
      is IrUnion -> layout.customScalarName(irType.name)
      else -> error("")
    }
  }

  private fun buildProperties() {
    (customScalars + interfaces + objects + unions).forEach {
      property(it)
    }
  }

  private fun allTypesPropertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add("listOf(\n")
    builder.indent()
    builder.add(
        properties.keys.map {
          CodeBlock.of("%L", it)
        }.joinToString(", ")
    )
    builder.unindent()
    builder.add(")\n")

    return PropertySpec.builder("all", List::class.asClassName().parameterizedBy(SchemaType::class.asClassName()))
        .initializer(builder.build())
        .build()
  }

  private fun typeSpec(): TypeSpec {
    buildProperties()

    return TypeSpec.objectBuilder(layout.typesName())
        .addKdoc("Auto generated constants representing the custom scalars, objects, interfaces and unions in the schema. Input objects " +
            "are left out because they are generated separately")
        .addProperties(properties.values)
        .addFunction(possibleTypesFunSpec())
        .addProperty(allTypesPropertySpec())
        .build()
  }

  private fun possibleTypesFunSpec(): FunSpec {
    val builder = FunSpec.builder("possibleTypes")

    builder.addParameter("type", SchemaType::class.asClassName())
    builder.returns(List::class.asClassName().parameterizedBy(Object::class.asClassName()))
    builder.addCode("return %M(all, type)\n", MemberName("com.apollographql.apollo3.api", "possibleTypes"))
    return builder.build()
  }

  private fun IrCustomScalar.propertySpec(): PropertySpec {
    /**
     * Custom Scalars without a mapping will generate code using [AnyResponseAdapter] directly
     * so the fallback isn't really required here. We still write it as a way to hint the user
     * to what's happening behind the scenes
     */
    val kotlinName = kotlinName ?: "kotlin.Any"
    return PropertySpec
        .builder(layout.customScalarName(name), CustomScalar::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S, %S)", CustomScalar::class.asTypeName(), name, kotlinName)
        .build()
  }

  private fun IrObject.propertySpec(): PropertySpec {
    // Make sure the interfaces appear first in the file else we get "Variable 'Bar' must be initialized"
    implements.forEach { interfaceName ->
      property(interfaces.first { it.name == interfaceName })
    }

    val builder = CodeBlock.builder()
    builder.add(implements.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.interfaceName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.objectName(name), Object::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", Object::class.asTypeName(), name, builder.build())
        .build()

  }

  private fun IrInterface.propertySpec(): PropertySpec {
    // Make sure the interfaces appear first in the file else we get "Variable 'Bar' must be initialized"
    implements.forEach { interfaceName ->
      property(interfaces.first { it.name == interfaceName })
    }

    val builder = CodeBlock.builder()
    builder.add(implements.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.interfaceName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.interfaceName(name), Interface::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", Interface::class.asTypeName(), name, builder.build())
        .build()
  }


  private fun IrUnion.propertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add(members.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.objectName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.interfaceName(name), Union::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", Union::class.asTypeName(), name, builder.build())
        .build()
  }
}
