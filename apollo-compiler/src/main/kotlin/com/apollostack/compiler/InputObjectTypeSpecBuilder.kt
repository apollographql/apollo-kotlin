package com.apollostack.compiler

import com.apollostack.compiler.ir.GraphQLType
import com.apollostack.compiler.ir.TypeDeclarationField
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class InputObjectTypeSpecBuilder(
    val name: String,
    val fields: List<TypeDeclarationField>
) {
  private val objectName = name.decapitalize()
  private val objectClassName = ClassName.get("", name.capitalize())

  fun build(): TypeSpec =
      TypeSpec.classBuilder(objectClassName)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addMethod(MethodSpec.constructorBuilder().build())
          .addFields(fields)
          .addBuilder(fields)
          .build()

  private fun TypeSpec.Builder.addFieldDefinition(field: TypeDeclarationField): TypeSpec.Builder {
    val fieldGraphQLType = field.graphQLType()
    val defaultValue = field.defaultValue?.let { (it as? Number)?.castTo(fieldGraphQLType) ?: it }
    return addField(FieldSpec
        .builder(fieldGraphQLType.toJavaTypeName(), field.name.decapitalize())
        .initializer(defaultValue?.let { CodeBlock.of("\$L", it) } ?: CodeBlock.of(""))
        .build())
  }

  private fun TypeSpec.Builder.addFieldAccessor(field: TypeDeclarationField) =
      addMethod(MethodSpec.methodBuilder(field.name.decapitalize())
          .addModifiers(Modifier.PUBLIC)
          .returns(field.graphQLType().toJavaTypeName())
          .addStatement("return this.\$L", field.name.decapitalize())
          .build())

  private fun TypeSpec.Builder.addBuilder(fields: List<TypeDeclarationField>): TypeSpec.Builder {
    if (fields.isEmpty()) {
      return this
    } else {
      val builderFields = fields.map { it.name.decapitalize() to it.graphQLType().toJavaTypeName() }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(BuilderTypeSpecBuilder(objectName, objectClassName, builderFields).build())
    }
  }

  private fun TypeSpec.Builder.addFields(fields: List<TypeDeclarationField>): TypeSpec.Builder {
    fields.forEach { field ->
      addFieldDefinition(field)
      addFieldAccessor(field)
    }
    return this
  }

  companion object {
    private fun TypeDeclarationField.graphQLType() = GraphQLType.resolveByName(type, !type.endsWith("!"))

    private fun Number.castTo(graphQLType: GraphQLType) =
      if (graphQLType is GraphQLType.GraphQLInt) {
        toInt()
      } else if (graphQLType is GraphQLType.GraphQLFloat) {
        toFloat()
      } else {
        this
      }
  }
}