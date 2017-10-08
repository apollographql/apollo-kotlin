package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.*
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.Modifier

data class Fragment(
    val fragmentName: String,
    val source: String,
    val typeCondition: String,
    val possibleTypes: List<String>,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val fragmentsReferenced: List<String>
) : CodeGenerator {

  /** Returns the Java interface that represents this Fragment object. */
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec {
    return SchemaTypeSpecBuilder(
        typeName = formatClassName(),
        fields = fields,
        fragmentSpreads = fragmentSpreads,
        inlineFragments = inlineFragments,
        context = context
    )
        .build(Modifier.PUBLIC)
        .toBuilder()
        .addSuperinterface(ClassNames.FRAGMENT)
        .addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addFragmentDefinitionField()
        .addTypeConditionField()
        .build()
        .flatten(excludeTypeNames = listOf(
            Util.RESPONSE_FIELD_MAPPER_TYPE_NAME,
            (SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type as ClassName).simpleName(),
            ClassNames.BUILDER.simpleName()
        ))
        .let {
          if (context.generateModelBuilder) {
            it.withBuilder()
          } else {
            it
          }
        }
  }

  fun formatClassName() = fragmentName.capitalize()

  private fun TypeSpec.Builder.addFragmentDefinitionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.STRING, FRAGMENT_DEFINITION_FIELD_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$S", source)
          .build())

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun TypeSpec.Builder.addTypeConditionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.parameterizedListOf(java.lang.String::class.java), POSSIBLE_TYPES_VAR)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer(possibleTypesInitCode())
          .build())

  private fun possibleTypesInitCode(): CodeBlock {
    val builder = CodeBlock.builder().add("\$T.unmodifiableList(\$T.asList(", Collections::class.java,
        Arrays::class.java)
    return possibleTypes.foldIndexed(
        builder,
        { i, builder, type ->
          if (i > 0) {
            builder.add(",")
          }
          builder.add(" \$S", type)
        }
    ).add("))").build()
  }

  companion object {
    val FRAGMENT_DEFINITION_FIELD_NAME: String = "FRAGMENT_DEFINITION"
    val POSSIBLE_TYPES_VAR: String = "POSSIBLE_TYPES"
  }
}
