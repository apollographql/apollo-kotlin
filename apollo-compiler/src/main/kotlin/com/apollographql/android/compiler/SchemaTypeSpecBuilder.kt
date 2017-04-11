package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.Field
import com.apollographql.android.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.Modifier

class SchemaTypeSpecBuilder(
    val typeName: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val context: CodeGenerationContext
) {
  private val uniqueTypeName = formatUniqueTypeName(typeName, context.reservedTypeNames)

  fun build(vararg modifiers: Modifier): TypeSpec {
    context.reservedTypeNames += uniqueTypeName

    val nestedTypeSpecs = nestedTypeSpecs()
    val nameOverrideMap = nestedTypeSpecs.map { it.first to it.second.name }.toMap()
    val mapper = SchemaTypeResponseMapperBuilder(uniqueTypeName, fields, fragmentSpreads, inlineFragments,
        nameOverrideMap, context).build()

    return TypeSpec.classBuilder(uniqueTypeName)
        .addModifiers(*modifiers)
        .addTypes(nestedTypeSpecs.map { it.second })
        .addFields(nameOverrideMap)
        .addInlineFragments(nameOverrideMap)
        .addFragments()
        .addType(mapper)
        .build()
        .withValueInitConstructor(context.nullableValueType)
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
  }

  private fun TypeSpec.Builder.addFields(nameOverrideMap: Map<String, String>): TypeSpec.Builder {
    addFields(fields
        .map { it.fieldSpec(context, !context.generateAccessors) }
        .map { it.overrideType(nameOverrideMap) })
    if (context.generateAccessors) {
      addMethods(fields
          .map { it.accessorMethodSpec(context) }
          .map { it.overrideReturnType(nameOverrideMap) })
    }
    return this
  }

  private fun TypeSpec.Builder.addFragments(): TypeSpec.Builder {
    if (fragmentSpreads.isNotEmpty()) {
      addType(fragmentsTypeSpec())
      addField(fragmentsFieldSpec())
      if (context.generateAccessors) {
        addMethod(fragmentsAccessorMethodSpec())
      }
    }
    return this
  }

  private fun nestedTypeSpecs(): List<Pair<String, TypeSpec>> {
    return fields.filter(Field::isNonScalar)
        .map { it.formatClassName() to it.toTypeSpec(context) }
        .plus(inlineFragments.map { it.formatClassName() to it.toTypeSpec(context) })
  }

  private fun TypeSpec.Builder.addInlineFragments(nameOverrideMap: Map<String, String>): TypeSpec.Builder {
    addFields(inlineFragments
        .map { it.fieldSpec(context, !context.generateAccessors) }
        .map { it.overrideType(nameOverrideMap) })

    if (context.generateAccessors) {
      addMethods(inlineFragments
          .map { it.accessorMethodSpec(context) }
          .map { it.overrideReturnType(nameOverrideMap) })
    }

    return this
  }

  private fun fragmentsAccessorMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(FRAGMENTS_TYPE_NAME.decapitalize())
        .returns(JavaTypeResolver(context, "").resolve(FRAGMENTS_TYPE_NAME, false))
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(emptyList())
        .addCode(CodeBlock.of("return this.${FRAGMENTS_TYPE_NAME.toLowerCase(Locale.ENGLISH)};\n"))
        .build()
  }

  private fun fragmentsFieldSpec(): FieldSpec = FieldSpec
      .builder(ClassName.get("", FRAGMENTS_TYPE_NAME.capitalize()).annotated(Annotations.NONNULL),
          FRAGMENTS_TYPE_NAME.decapitalize())
      .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
      .build()

  /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
  private fun fragmentsTypeSpec(): TypeSpec {

    fun TypeSpec.Builder.addFragmentFields(): TypeSpec.Builder {
      return addFields(fragmentSpreads.map {
        FieldSpec.builder(JavaTypeResolver(context, context.fragmentsPackage).resolve(it.capitalize()),
            it.decapitalize())
            .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
            .build()
      })
    }

    fun TypeSpec.Builder.addFragmentAccessorMethods(): TypeSpec.Builder {
      if (context.generateAccessors) {
        addMethods(fragmentSpreads.map {
          MethodSpec.methodBuilder(it.decapitalize())
              .returns(JavaTypeResolver(context, context.fragmentsPackage).resolve(it.capitalize()))
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return this.\$L", it.decapitalize())
              .build()
        })
      }
      return this
    }

    val mapper = FragmentsResponseMapperBuilder(fragmentSpreads, context).build()
    return TypeSpec.classBuilder(formatUniqueTypeName(FRAGMENTS_TYPE_NAME, context.reservedTypeNames))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFragmentFields()
        .addFragmentAccessorMethods()
        .addType(mapper)
        .build()
        .withValueInitConstructor(context.nullableValueType)
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
  }

  private fun formatUniqueTypeName(typeName: String, reservedTypeNames: List<String>): String {
    var index = 1
    var name = typeName
    while (reservedTypeNames.contains(name)) {
      name = "$typeName$index"
      index++
    }
    return name
  }

  companion object {
    val FRAGMENTS_TYPE_NAME: String = "Fragments"
    val FRAGMENTS_TYPE: TypeName = ClassName.get("", FRAGMENTS_TYPE_NAME).annotated(Annotations.NONNULL)
  }
}
