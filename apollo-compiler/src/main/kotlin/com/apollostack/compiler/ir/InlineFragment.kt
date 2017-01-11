package com.apollostack.compiler.ir

import com.apollostack.compiler.Annotations
import com.apollostack.compiler.SchemaTypeSpecBuilder
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) : CodeGenerator {
  override fun toTypeSpec(abstract: Boolean): TypeSpec {
    val typeSpecBuilder = if (abstract) {
      TypeSpec.interfaceBuilder(interfaceName())
    } else {
      TypeSpec.classBuilder(interfaceName())
    }
    return typeSpecBuilder
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFields(if (abstract) emptyList() else fields.map(Field::fieldSpec))
        .addMethods(fields.map { it.accessorMethodSpec(abstract) })
        .addTypes(fields.filter(Field::isNonScalar).map { field ->
          SchemaTypeSpecBuilder(field.normalizedName(), field.fields ?: emptyList(), fragmentSpreads ?: emptyList(),
              field.inlineFragments ?: emptyList(), abstract).build(Modifier.PUBLIC, Modifier.STATIC)
        })
        .build()
  }

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

  private fun interfaceName() = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  private fun typeName() = ClassName.get("", interfaceName()).annotated(Annotations.NULLABLE)

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}