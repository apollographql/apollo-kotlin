package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.TypeDeclarationField
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class InputObjectTypeSpecBuilder(
    val name: String,
    val fields: List<TypeDeclarationField>,
    val context: CodeGenerationContext
) {
  private val objectClassName = ClassName.get("", name.capitalize())

  fun build(): TypeSpec =
      TypeSpec.classBuilder(objectClassName)
          .addAnnotation(Annotations.GENERATED_BY_APOLLO)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addConstructor()
          .addFields()
          .addBuilder()
          .build()

  private fun TypeSpec.Builder.addConstructor(): TypeSpec.Builder {
    val fieldInitializeCodeBuilder = fields.map {
      CodeBlock.of("this.\$L = \$L;\n", it.name.decapitalize(), it.name.decapitalize())
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return addMethod(MethodSpec
        .constructorBuilder()
        .addParameters(fields.map {
          ParameterSpec.builder(it.javaTypeName(context),
              it.name.decapitalize()).build()
        })
        .addCode(fieldInitializeCodeBuilder.build())
        .build()
    )
  }

  private fun TypeSpec.Builder.addFieldDefinition(field: TypeDeclarationField): TypeSpec.Builder =
      addField(FieldSpec
          .builder(field.javaTypeName(context), field.name.decapitalize())
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .build())

  private fun TypeSpec.Builder.addFieldAccessor(field: TypeDeclarationField) =
      addMethod(MethodSpec.methodBuilder(field.name.decapitalize())
          .addModifiers(Modifier.PUBLIC)
          .returns(field.javaTypeName(context))
          .addStatement("return this.\$L", field.name.decapitalize())
          .build())

  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (fields.isEmpty()) {
      return this
    } else {
      val builderFields = fields.map { it.name.decapitalize() to it.javaTypeName(context) }
      val builderFieldDefaultValues = fields.associate { it.name.decapitalize() to it.defaultValue }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(BuilderTypeSpecBuilder(objectClassName, builderFields, builderFieldDefaultValues).build())
    }
  }

  private fun TypeSpec.Builder.addFields(): TypeSpec.Builder {
    fields.forEach { field ->
      addFieldDefinition(field)
      addFieldAccessor(field)
    }
    return this
  }

  companion object {
    private fun TypeDeclarationField.javaTypeName(context: CodeGenerationContext) =
        JavaTypeResolver(context.customScalarTypeMap, context.typesPackage).resolve(type, !type.endsWith("!"))
  }
}
