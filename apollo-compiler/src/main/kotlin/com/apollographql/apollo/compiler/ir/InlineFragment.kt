package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.JavaTypeResolver
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.moshi.JsonClass
import javax.lang.model.element.Modifier

@JsonClass(generateAdapter = true)
data class InlineFragment(
    val typeCondition: String,
    val possibleTypes: List<String> = emptyList(),
    val description: String,
    val fields: List<Field>,
    val inlineFragments: List<InlineFragment>,
    val fragments: List<FragmentRef>,
    val sourceLocation: SourceLocation,
    val conditions: List<Condition> = emptyList()
) : CodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    return SchemaTypeSpecBuilder(
        typeName = formatClassName(),
        description = description,
        schemaType = typeCondition,
        fields = fields,
        fragments = fragments,
        inlineFragments = inlineFragments,
        context = context,
        abstract = abstract
    )
        .build(Modifier.PUBLIC, Modifier.STATIC)
        .let {
          if (context.generateModelBuilder) {
            it.withBuilder()
          } else {
            it
          }
        }
  }

  fun fieldSpec(context: CodeGenerationContext, publicModifier: Boolean = false): FieldSpec =
      FieldSpec.builder(typeName(context), formatClassName().decapitalize())
          .let { if (publicModifier) it.addModifiers(Modifier.PUBLIC) else it }
          .addModifiers(Modifier.FINAL)
          .build()

  fun formatClassName(): String = "As${typeCondition.capitalize()}"

  private fun typeName(context: CodeGenerationContext) = JavaTypeResolver(context, "").resolve(formatClassName(), true)
}
