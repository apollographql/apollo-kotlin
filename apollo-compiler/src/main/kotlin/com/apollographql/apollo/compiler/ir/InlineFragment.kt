package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.JavaTypeResolver
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = formatClassName(),
          fields = listOf(Field("__typename", "__typename", "String!")) + fields,
          fragmentSpreads = fragmentSpreads ?: emptyList(),
          inlineFragments = emptyList(),
          context = context
      ).build(Modifier.PUBLIC, Modifier.STATIC)

  fun accessorMethodSpec(context: CodeGenerationContext): MethodSpec {
    return MethodSpec.methodBuilder(formatClassName().decapitalize())
        .addModifiers(Modifier.PUBLIC)
        .returns(typeName(context))
        .addStatement("return this.\$L", formatClassName().decapitalize())
        .build()
  }

  fun fieldSpec(context: CodeGenerationContext, publicModifier: Boolean = false): FieldSpec =
      FieldSpec.builder(typeName(context), formatClassName().decapitalize())
          .addModifiers(if (publicModifier) Modifier.PUBLIC else Modifier.PRIVATE, Modifier.FINAL)
          .build()

  fun formatClassName(): String = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  private fun typeName(context: CodeGenerationContext) = JavaTypeResolver(context, "").resolve(formatClassName(), true)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}
