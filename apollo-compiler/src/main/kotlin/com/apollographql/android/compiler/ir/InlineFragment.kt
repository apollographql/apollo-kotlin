package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.JavaTypeResolver
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) {
  fun toTypeSpec(context: CodeGenerationContext, superClass: TypeName): TypeSpec =
      SchemaTypeSpecBuilder(
          typeName = formatClassName(),
          fields = fields,
          fragmentSpreads = fragmentSpreads ?: emptyList(),
          inlineFragments = emptyList(),
          context = context,
          superClass = superClass
      ).build(Modifier.PUBLIC, Modifier.STATIC)

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
