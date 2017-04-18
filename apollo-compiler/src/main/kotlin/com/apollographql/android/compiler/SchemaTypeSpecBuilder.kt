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
    val context: CodeGenerationContext,
    val superClass: TypeName = TypeName.OBJECT
) {
  private val uniqueTypeName = formatUniqueTypeName(typeName, context.reservedTypeNames)
  private val javaClassName = ClassName.get("", uniqueTypeName)

  fun build(vararg modifiers: Modifier): TypeSpec {
    context.reservedTypeNames += uniqueTypeName

    val nestedTypeSpecs = nestedTypeSpecs()
    val nameOverrideMap = nestedTypeSpecs.map { it.first to it.second.name }.toMap()
    val mapper = SchemaTypeResponseMapperBuilder(uniqueTypeName, fields, fragmentSpreads, inlineFragments,
        nameOverrideMap, context).build()

    return TypeSpec.classBuilder(uniqueTypeName)
        .addModifiers(*modifiers)
        .superclass(superClass)
        .addTypes(nestedTypeSpecs.map { it.second })
        .addFields(nameOverrideMap)
        .addFragments()
        .addType(mapper)
        .let {
          if (inlineFragments.isNotEmpty())
            it.addMethod(constructorWithInlineFragments(nameOverrideMap))
          else
            it
        }
        .build()
        .let {
          if (inlineFragments.isEmpty())
            it.withValueInitConstructor(nullableValueGenerationType = context.nullableValueType)
          else
            it
        }
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
  }

  private fun TypeSpec.Builder.addFields(nameOverrideMap: Map<String, String>): TypeSpec.Builder {
    if (inlineFragments.isEmpty()) {
      addFields(fields
          .map { it.fieldSpec(context, !context.generateAccessors) }
          .map { it.overrideType(nameOverrideMap) })
      if (context.generateAccessors) {
        addMethods(fields
            .map { it.accessorMethodSpec(context) }
            .map { it.overrideReturnType(nameOverrideMap) })
      }
    } else {
      addFields(inlineFragments
          .map { it.fieldSpec(context, !context.generateAccessors) }
          .map { it.overrideType(nameOverrideMap) })
    }
    return this
  }

  private fun TypeSpec.Builder.addFragments(): TypeSpec.Builder {
    if (fragmentSpreads.isNotEmpty() && inlineFragments.isEmpty()) {
      addType(fragmentsTypeSpec())
      addField(fragmentsFieldSpec())
      if (context.generateAccessors) {
        addMethod(fragmentsAccessorMethodSpec())
      }
    }
    return this
  }

  private fun nestedTypeSpecs(): List<Pair<String, TypeSpec>> {
    if (inlineFragments.isEmpty()) {
      return fields.filter(Field::isNonScalar)
          .map { it.formatClassName() to it.toTypeSpec(context) }
    } else {
      return inlineFragments.map {
        it.formatClassName() to it.toTypeSpec(
            context = context,
            superClass = javaClassName
        )
      }
    }
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

    fun isOptional(fragmentName: String): Boolean {
      return context.ir.fragments
          .find { it.fragmentName == fragmentName }
          ?.let { it.typeCondition == typeName } ?: true
    }

    fun TypeSpec.Builder.addFragmentFields(): TypeSpec.Builder {
      return addFields(fragmentSpreads.map { fragmentName ->
        val optional = isOptional(fragmentName)
        FieldSpec.builder(
            JavaTypeResolver(context = context, packageName = context.fragmentsPackage)
                .resolve(typeName = fragmentName.capitalize(), isOptional = optional), fragmentName.decapitalize())
            .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
            .build()
      })
    }

    fun TypeSpec.Builder.addFragmentAccessorMethods(): TypeSpec.Builder {
      if (context.generateAccessors) {
        addMethods(fragmentSpreads.map { fragmentName ->
          val optional = isOptional(fragmentName)
          MethodSpec.methodBuilder(fragmentName.decapitalize())
              .returns(JavaTypeResolver(context = context, packageName = context.fragmentsPackage)
                  .resolve(typeName = fragmentName.capitalize(), isOptional = optional))
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return this.\$L", fragmentName.decapitalize())
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

  private fun constructorWithInlineFragments(nameOverrideMap: Map<String, String>): MethodSpec {
    val code = inlineFragments
        .map { it.fieldSpec(context = context, publicModifier = true) }
        .mapIndexed { i, fieldSpec ->
          val type = fieldSpec.type.unwrapOptionalType().withoutAnnotations().overrideTypeName(nameOverrideMap)
          val nullableTypeWrapperClass = when (context.nullableValueType) {
            NullableValueType.APOLLO_OPTIONAL -> ClassNames.OPTIONAL
            NullableValueType.GUAVA_OPTIONAL -> ClassNames.GUAVA_OPTIONAL
            else -> null
          }

          val codeBuilder = CodeBlock.builder()
          codeBuilder.beginControlFlow("if (this instanceof \$T)", type)
          if (nullableTypeWrapperClass != null) {
            codeBuilder.addStatement("\$L = \$T.fromNullable((\$T) this)", fieldSpec.name, nullableTypeWrapperClass,
                type)
          } else {
            codeBuilder.addStatement("\$L = (\$T) this", fieldSpec.name, type)
          }
          codeBuilder.nextControlFlow("else")
          if (nullableTypeWrapperClass != null) {
            codeBuilder.addStatement("\$L = \$T.absent()", fieldSpec.name, nullableTypeWrapperClass)
          } else {
            codeBuilder.addStatement("\$L = null", fieldSpec.name)
          }
          codeBuilder.endControlFlow()
          codeBuilder.build()
        }
        .fold(CodeBlock.builder(), CodeBlock.Builder::add)
        .build()
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addCode(code)
        .build()
  }

  companion object {
    val FRAGMENTS_TYPE_NAME: String = "Fragments"
    val FRAGMENTS_TYPE: TypeName = ClassName.get("", FRAGMENTS_TYPE_NAME).annotated(Annotations.NONNULL)
  }
}
