package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ClassNames
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.apollographql.apollo.compiler.Util
import com.apollographql.apollo.compiler.VisitorSpec.VISITOR_CLASSNAME
import com.apollographql.apollo.compiler.flatten
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.moshi.JsonClass
import javax.lang.model.element.Modifier

@JsonClass(generateAdapter = true)
data class Fragment(
    val fragmentName: String,
    val source: String,
    val description: String,
    val typeCondition: String,
    val possibleTypes: List<String>,
    val fields: List<Field>,
    val fragmentRefs: List<FragmentRef>,
    val inlineFragments: List<InlineFragment>,
    val filePath: String,
    val sourceLocation: SourceLocation
) : CodeGenerator {

  /** Returns the Java interface that represents this Fragment object. */
  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    return SchemaTypeSpecBuilder(
        typeName = fragmentName.capitalize(),
        schemaType = typeCondition,
        description = description,
        fields = fields,
        fragments = fragmentRefs,
        inlineFragments = inlineFragments,
        context = context,
        abstract = abstract
    )
        .build(Modifier.PUBLIC)
        .toBuilder()
        .addSuperinterface(ClassNames.FRAGMENT)
        .addFragmentDefinitionField()
        .build()
        .flatten(excludeTypeNames = listOf(
            VISITOR_CLASSNAME,
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

  private fun TypeSpec.Builder.addFragmentDefinitionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.STRING, FRAGMENT_DEFINITION_FIELD_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$S", source)
          .build())

  companion object {
    const val FRAGMENT_DEFINITION_FIELD_NAME: String = "FRAGMENT_DEFINITION"
  }
}
