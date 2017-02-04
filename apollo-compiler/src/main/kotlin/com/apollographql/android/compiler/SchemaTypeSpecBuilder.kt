package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.Field
import com.apollographql.android.compiler.ir.InlineFragment
import com.squareup.javapoet.*
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
    val typeSpecBuilder = if (context.abstractType) {
      TypeSpec.interfaceBuilder(uniqueTypeName)
    } else {
      TypeSpec.classBuilder(uniqueTypeName)
    }
    return typeSpecBuilder
        .addModifiers(*modifiers)
        .addFields(fields, context.abstractType)
        .addInnerTypes(fields)
        .addInlineFragments(inlineFragments)
        .addInnerFragmentTypes(fragmentSpreads)
        .build()
        .withFactory()
        .withCreator()
        .let {
          if (context.abstractType)
            it
          else
            it
                .withValueInitConstructor()
                .withCreatorImplementation()
                .withFactoryImplementation()
                .toBuilder()
                .addType(mapper)
                .build()
        }
  }

  private fun TypeSpec.Builder.addFields(fields: List<Field>, abstractClass: Boolean): TypeSpec.Builder {
    val fieldSpecs = if (abstractClass) emptyList() else fields.map {
      it.fieldSpec(context.customTypeMap, context.typesPackage)
    }
    val methodSpecs = fields.map {
      it.accessorMethodSpec(abstractClass, context.typesPackage, context.customTypeMap)
    }
    return addFields(fieldSpecs.map { it.overrideType(innerTypeNameOverrideMap) })
        .addMethods(methodSpecs.map { it.overrideReturnType(innerTypeNameOverrideMap) })
  }

  private fun TypeSpec.Builder.addInnerFragmentTypes(fragments: List<String>): TypeSpec.Builder {
    if (fragments.isNotEmpty()) {
      addMethod(fragmentsAccessorMethodSpec(context.abstractType))
      addFields(if (context.abstractType) emptyList() else listOf(fragmentsFieldSpec()))
      addType(fragmentsTypeSpec(fragments, context.abstractType, context.fragmentsPackage))
    }
    return this
  }

  private fun TypeSpec.Builder.addInnerTypes(fields: List<Field>): TypeSpec.Builder {
    val reservedTypeNames = context.reservedTypeNames + typeName + fields.filter(Field::isNonScalar).map(
        Field::normalizedName)
    val typeSpecs = fields.filter(Field::isNonScalar).map {
      it.toTypeSpec(CodeGenerationContext(context.abstractType, reservedTypeNames.minus(it.normalizedName()),
          context.typeDeclarations, context.fragmentsPackage, context.typesPackage, context.customTypeMap))
    }
    return addTypes(typeSpecs)
  }

  private fun TypeSpec.Builder.addInlineFragments(fragments: List<InlineFragment>): TypeSpec.Builder {
    val reservedTypeNames = context.reservedTypeNames + typeName + fields.filter(Field::isNonScalar).map(
        Field::normalizedName)
    val typeSpecs = fragments.map {
      it.toTypeSpec(CodeGenerationContext(context.abstractType, reservedTypeNames, context.typeDeclarations,
          context.fragmentsPackage, context.typesPackage, context.customTypeMap))
    }
    val methodSpecs = fragments.map { it.accessorMethodSpec(context.abstractType) }
    val fieldSpecs = if (context.abstractType) emptyList() else fragments.map { it.fieldSpec() }
    return addTypes(typeSpecs)
        .addMethods(methodSpecs)
        .addFields(fieldSpecs)
  }

  private fun fragmentsAccessorMethodSpec(abstract: Boolean): MethodSpec {
    val methodSpecBuilder = MethodSpec
        .methodBuilder(FRAGMENTS_TYPE_NAME.decapitalize())
        .returns(ClassName.get("", FRAGMENTS_TYPE_NAME))
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(if (abstract) listOf(Modifier.ABSTRACT) else emptyList())
    if (!abstract) {
      methodSpecBuilder.addCode(CodeBlock.of("return this.${FRAGMENTS_TYPE_NAME.toLowerCase()};\n"))
    }
    return methodSpecBuilder.build()
  }

  private fun fragmentsFieldSpec(): FieldSpec = FieldSpec
      .builder(ClassName.get("", FRAGMENTS_TYPE_NAME.capitalize()), FRAGMENTS_TYPE_NAME.decapitalize())
      .addModifiers(Modifier.PRIVATE)
      .build()

  /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
  private fun fragmentsTypeSpec(fragments: List<String>, abstractClass: Boolean, fragmentsPackage: String): TypeSpec {

    fun TypeSpec.Builder.addFragmentFields(): TypeSpec.Builder {
      if (!abstractClass) {
        addFields(fragments.map {
          FieldSpec
              .builder(ClassName.get("", it.capitalize()), it.decapitalize())
              .addModifiers(Modifier.PRIVATE)
              .build()
        })
      }
      return this
    }

    fun TypeSpec.Builder.addFragmentAccessorMethods(): TypeSpec.Builder {
      return addMethods(fragments.map {
        val methodSpecBuilder = MethodSpec
            .methodBuilder(it.decapitalize())
            .returns(ClassName.get(fragmentsPackage, it.capitalize()))
            .addModifiers(Modifier.PUBLIC)
            .addModifiers(if (abstractClass) listOf(Modifier.ABSTRACT) else emptyList())
        if (!abstractClass) {
          methodSpecBuilder.addStatement("return this.\$L", it.decapitalize())
        }
        methodSpecBuilder.build()
      })
    }

    val mapper = FragmentsResponseMapperBuilder(fragments, context).build()
    val typeSpecBuilder = if (abstractClass) {
      TypeSpec.interfaceBuilder(FRAGMENTS_TYPE_NAME)
    } else {
      TypeSpec.classBuilder(FRAGMENTS_TYPE_NAME)
    }
    return typeSpecBuilder
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFragmentFields()
        .addFragmentAccessorMethods()
        .build()
        .withFactory(fragments)
        .withCreator()
        .let {
          if (context.abstractType)
            it
          else
            it
                .withValueInitConstructor()
                .withCreatorImplementation()
                .withFactoryImplementation(fragments)
                .toBuilder()
                .addType(mapper)
                .build()
        }
  }

  private fun buildUniqueTypeNameMap(reservedTypeNames: List<String>) =
      reservedTypeNames.distinct().associate {
        it to formatUniqueTypeName(it, reservedTypeNames)
      }

  private fun formatUniqueTypeName(typeName: String, reservedTypeNames: List<String>): String {
    val suffix = reservedTypeNames.count { it == typeName }.let { if (it > 0) "$".repeat(it) else "" }
    return "$typeName$suffix"
  }

  companion object {
    val FRAGMENTS_TYPE_NAME: String = "Fragments"
    val FRAGMENTS_TYPE: ClassName = ClassName.get("", SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME)
  }
}
