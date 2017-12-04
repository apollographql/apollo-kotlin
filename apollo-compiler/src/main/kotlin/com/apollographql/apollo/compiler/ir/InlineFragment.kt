package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.JavaTypeResolver
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val possibleTypes: List<String>?,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = formatClassName(),
          fields = fields,
          fragmentSpreads = fragmentSpreads ?: emptyList(),
          inlineFragments = emptyList(),
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

  fun fieldSpec(context: CodeGenerationContext, publicModifier: Boolean = false): FieldSpec =
      FieldSpec.builder(typeName(context), formatClassName().decapitalize())
          .let { if (publicModifier) it.addModifiers(Modifier.PUBLIC) else it }
          .addModifiers(Modifier.FINAL)
          .build()

  fun formatClassName(): String = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  private fun typeName(context: CodeGenerationContext) = JavaTypeResolver(context, "").resolve(formatClassName(), true)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}
