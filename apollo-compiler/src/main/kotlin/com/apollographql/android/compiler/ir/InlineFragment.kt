package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.JavaTypeResolver
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
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
      SchemaTypeSpecBuilder(formatClassName(), fields, fragmentSpreads ?: emptyList(), emptyList(), context)
          .build(Modifier.PUBLIC, Modifier.STATIC)

  fun accessorMethodSpec(context: CodeGenerationContext): MethodSpec {
    return MethodSpec.methodBuilder(formatClassName().decapitalize())
        .addModifiers(Modifier.PUBLIC)
        .returns(typeName(context))
        .addStatement("return this.\$L", formatClassName().decapitalize())
        .build()
  }

  fun fieldSpec(context: CodeGenerationContext, publicModifier:Boolean = false): FieldSpec =
      FieldSpec.builder(typeName(context), formatClassName().decapitalize())
          .addModifiers(if (publicModifier) Modifier.PUBLIC else Modifier.PRIVATE, Modifier.FINAL)
          .build()

  fun formatClassName():String = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  private fun typeName(context: CodeGenerationContext) = JavaTypeResolver(context, "").resolve(formatClassName(), true)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}
