package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.api.EnumType
import com.apollographql.apollo3.api.InterfaceType
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.api.UnionType
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrCustomScalar
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrObject
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class TypesBuilder(
    private val context: CgContext,
    private val customScalars: List<IrCustomScalar>,
    private val objects: List<IrObject>,
    private val interfaces: List<IrInterface>,
    private val unions: List<IrUnion>,
    private val enums: List<IrEnum>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  /**
   * Unions go last as they depend of objects
   */
  private val allTypes = (customScalars + enums + interfaces + objects + unions)

  override fun prepare() {
    customScalars.forEach {
      context.resolver.registerCustomScalar(
          it.name,
          it.kotlinName
      )
    }
    allTypes.forEach {
      val name = name(it)

      context.resolver.registerCompiledType(
          name,
          MemberName(
              ClassName(
                  packageName,
                  layout.typesName()
              ),
              layout.compiledTypeName(name)
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

  /**
   * The order of declaration is important as the Kotlin compiler will require that any property
   * only references properties before it.
   * We recursively track the required properties in this mutable map
   */
  private val properties = mutableMapOf<String, PropertySpec>()

  private fun property(irType: Any): PropertySpec {
    return properties.getOrPut(propertyName(irType)) {
      when (irType) {
        is IrCustomScalar -> irType.propertySpec()
        is IrEnum -> irType.propertySpec()
        is IrInterface -> irType.propertySpec()
        is IrUnion -> irType.propertySpec()
        is IrObject -> irType.propertySpec()
        else -> error("")
      }
    }
  }

  private fun name(irType: Any): String {
    return when (irType) {
      is IrCustomScalar -> irType.name
      is IrEnum -> irType.name
      is IrInterface -> irType.name
      is IrUnion -> irType.name
      is IrObject -> irType.name
      else -> error("")
    }
  }

  private fun propertyName(irType: Any): String {
    return layout.compiledTypeName(name(irType))
  }

  private fun buildProperties() {
    allTypes.forEach {
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

    return PropertySpec.builder("all", List::class.asClassName().parameterizedBy(CompiledType::class.asClassName()))
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

    builder.addParameter("type", CompiledNamedType::class.asClassName())
    builder.returns(List::class.asClassName().parameterizedBy(ObjectType::class.asClassName()))
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
        .builder(layout.compiledTypeName(name), CustomScalarType::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S, %S)", CustomScalarType::class.asTypeName(), name, kotlinName)
        .build()
  }

  private fun IrEnum.propertySpec(): PropertySpec {
    return PropertySpec
        .builder(layout.compiledTypeName(name), EnumType::class)
        .maybeAddDescription(description)
        .initializer("%T(%S)", EnumType::class.asTypeName(), name)
        .build()
  }

  private fun Set<String>.toCode(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("listOf(")
    builder.add("%L", sorted().map { CodeBlock.of("%S", it) }.joinToCode(", "))
    builder.add(")")
    return builder.build()
  }

  private fun List<String>.implementsToCode(): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add("listOf(")
    builder.add("%L", sorted().map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(context.layout.compiledTypeName(it))
    }.joinToCode(", "))
    builder.add(")")
    return builder.build()
  }

  private fun IrObject.propertySpec(): PropertySpec {
    // Make sure the interfaces appear first in the file else we get "Variable 'Bar' must be initialized"
    implements.forEach { interfaceName ->
      property(interfaces.first { it.name == interfaceName })
    }

    val builder = CodeBlock.builder()
    builder.add("%T(name = %S", ObjectType::class.asTypeName(), name)
    if (keyFields.isNotEmpty()) {
      builder.add(", ")
      builder.add("keyFields = %L", keyFields.toCode())
    }
    if (implements.isNotEmpty()) {
      builder.add(", ")
      builder.add("implements = %L", implements.implementsToCode())
    }
    builder.add(")")

    return PropertySpec
        .builder(context.layout.compiledTypeName(name), ObjectType::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer(builder.build())
        .build()
  }

  private fun IrInterface.propertySpec(): PropertySpec {
    // Make sure the interfaces appear first in the file else we get "Variable 'Bar' must be initialized"
    implements.forEach { interfaceName ->
      property(interfaces.first { it.name == interfaceName })
    }

    val builder = CodeBlock.builder()
    builder.add("%T(name = %S", InterfaceType::class.asTypeName(), name)
    if (keyFields.isNotEmpty()) {
      builder.add(", ")
      builder.add("keyFields = %L", keyFields.toCode())
    }
    if (implements.isNotEmpty()) {
      builder.add(", ")
      builder.add("implements = %L", implements.implementsToCode())
    }
    builder.add(")")

    return PropertySpec
        .builder(context.layout.compiledTypeName(name), InterfaceType::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer(builder.build())
        .build()
  }


  private fun IrUnion.propertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add(members.map {
      // We do not use typenames here to avoid having long qualified names as arguments
      CodeBlock.of(", ${context.layout.compiledTypeName(it)}")
    }.joinToString(""))

    return PropertySpec
        .builder(context.layout.compiledTypeName(name), UnionType::class)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .initializer("%T(%S%L)", UnionType::class.asTypeName(), name, builder.build())
        .build()
  }
}
