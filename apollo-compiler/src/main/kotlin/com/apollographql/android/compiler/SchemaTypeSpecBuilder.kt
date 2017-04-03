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
  private val innerTypeNameOverrideMap = buildUniqueTypeNameMap(context.reservedTypeNames + typeName)

  fun build(vararg modifiers: Modifier): TypeSpec {
    val mapper = SchemaTypeResponseMapperBuilder(uniqueTypeName, fields, fragmentSpreads, inlineFragments,
        innerTypeNameOverrideMap, context).build()
    return TypeSpec.classBuilder(uniqueTypeName)
        .addModifiers(*modifiers)
        .addFields(fields)
        .addInnerTypes(fields.filter(Field::isNonScalar))
        .addInlineFragments(inlineFragments)
        .addInnerFragmentTypes(fragmentSpreads)
        .addType(mapper)
        .build()
        .withValueInitConstructor(context.nullableValueType)
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
  }

  private fun TypeSpec.Builder.addFields(fields: List<Field>): TypeSpec.Builder {
    addFields(fields
        .map { it.fieldSpec(context, !context.generateAccessors) }
        .map { it.overrideType(innerTypeNameOverrideMap) })
    if (context.generateAccessors) {
      addMethods(fields
          .map { it.accessorMethodSpec(context) }
          .map { it.overrideReturnType(innerTypeNameOverrideMap) })
    }
    return this
  }

  private fun TypeSpec.Builder.addInnerFragmentTypes(fragments: List<String>): TypeSpec.Builder {
    if (fragments.isNotEmpty()) {
      if (context.generateAccessors) {
        addMethod(fragmentsAccessorMethodSpec())
      }
      addFields(listOf(fragmentsFieldSpec()))
      addType(fragmentsTypeSpec(fragments))
    }
    return this
  }

  private fun TypeSpec.Builder.addInnerTypes(fields: List<Field>): TypeSpec.Builder {
    val reservedTypeNames = context.reservedTypeNames + typeName + fields.map(Field::normalizedName)
    val typeSpecs = fields.map { field ->
      field.toTypeSpec(context.copy(reservedTypeNames = reservedTypeNames.minus(field.normalizedName())))
    }
    return addTypes(typeSpecs)
  }

  private fun TypeSpec.Builder.addInlineFragments(fragments: List<InlineFragment>): TypeSpec.Builder {
    val reservedTypeNames = context.reservedTypeNames + typeName +
        fields.filter(Field::isNonScalar).map(Field::normalizedName)
    val uniqueTypeNameMap = buildUniqueTypeNameMap(reservedTypeNames)

    addTypes(fragments.map { it.toTypeSpec(context.copy(reservedTypeNames = reservedTypeNames)) })
    addFields(fragments.map { it.fieldSpec(context, !context.generateAccessors).overrideType(uniqueTypeNameMap) })

    if (context.generateAccessors) {
      addMethods(fragments
          .map { it.accessorMethodSpec(context).overrideReturnType(uniqueTypeNameMap) })
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
  private fun fragmentsTypeSpec(fragments: List<String>): TypeSpec {

    fun TypeSpec.Builder.addFragmentFields(): TypeSpec.Builder {
      return addFields(fragments.map {
        FieldSpec.builder(JavaTypeResolver(context, context.fragmentsPackage).resolve(it.capitalize()),
            it.decapitalize())
            .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
            .build()
      })
    }

    fun TypeSpec.Builder.addFragmentAccessorMethods(): TypeSpec.Builder {
      if (context.generateAccessors) {
        addMethods(fragments.map {
          MethodSpec.methodBuilder(it.decapitalize())
              .returns(JavaTypeResolver(context, context.fragmentsPackage).resolve(it.capitalize()))
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return this.\$L", it.decapitalize())
              .build()
        })
      }
      return this
    }

    val mapper = FragmentsResponseMapperBuilder(fragments, context).build()
    return TypeSpec.classBuilder(FRAGMENTS_TYPE_NAME)
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

  private fun buildUniqueTypeNameMap(reservedTypeNames: List<String>) =
      reservedTypeNames.distinct().associate {
        it to formatUniqueTypeName(it, reservedTypeNames)
      }

  private fun formatUniqueTypeName(typeName: String, reservedTypeNames: List<String>): String {
    val suffix = reservedTypeNames.count { it == typeName }.let { if (it > 0) "$it" else "" }
    return "$typeName$suffix"
  }

  companion object {
    val FRAGMENTS_TYPE_NAME: String = "Fragments"
    val FRAGMENTS_TYPE: TypeName = ClassName.get("", FRAGMENTS_TYPE_NAME).annotated(Annotations.NONNULL)
  }
}
