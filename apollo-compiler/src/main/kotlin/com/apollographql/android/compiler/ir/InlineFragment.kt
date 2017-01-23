package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.Annotations
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(abstractClass: Boolean, reservedTypeNames: List<String>,
      typeDeclarations: List<TypeDeclaration>, fragmentsPackage: String, typesPackage: String): TypeSpec =
      SchemaTypeSpecBuilder(interfaceName(), fields, fragmentSpreads ?: emptyList(), emptyList(), abstractClass,
          reservedTypeNames, typeDeclarations, fragmentsPackage, typesPackage)
          .build(Modifier.PUBLIC, Modifier.STATIC)

  fun accessorMethodSpec(abstract: Boolean): MethodSpec {
    val methodSpecBuilder = MethodSpec
        .methodBuilder(interfaceName().decapitalize())
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(if (abstract) listOf(Modifier.ABSTRACT) else emptyList())
        .returns(typeName())
    if (!abstract) {
      methodSpecBuilder.addStatement("return this.\$L", interfaceName().decapitalize())
    }
    return methodSpecBuilder.build()
  }

  fun fieldSpec(): FieldSpec =
      FieldSpec.builder(typeName(), interfaceName().decapitalize())
          .addModifiers(Modifier.PRIVATE)
          .build()

  private fun interfaceName() = "${INTERFACE_PREFIX}${typeCondition.capitalize()}"

  private fun typeName() = ClassName.get("", interfaceName()).annotated(Annotations.NULLABLE)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}
