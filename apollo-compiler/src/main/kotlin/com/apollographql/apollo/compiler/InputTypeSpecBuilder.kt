package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.TypeDeclarationField
import com.squareup.javapoet.*
import java.io.IOException
import javax.lang.model.element.Modifier

class InputTypeSpecBuilder(
    val name: String,
    val fields: List<TypeDeclarationField>,
    val context: CodeGenerationContext
) {
  private val objectClassName = ClassName.get("", name.capitalize())

  fun build(): TypeSpec =
      TypeSpec.classBuilder(objectClassName)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addSuperinterface(ClassNames.INPUT_TYPE)
          .addConstructor()
          .addFields()
          .addBuilder()
          .addMethod(marshallerMethodSpec())
          .build()
          .withHashCodeImplementation()
          .withEqualsImplementation()

  private fun TypeSpec.Builder.addConstructor(): TypeSpec.Builder {
    val fieldInitializeCodeBuilder = fields.map {
      CodeBlock.of("this.\$L = \$L;\n", it.name.decapitalize().escapeJavaReservedWord(), it.name.decapitalize().escapeJavaReservedWord())
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return addMethod(MethodSpec
        .constructorBuilder()
        .addParameters(fields.map {
          ParameterSpec.builder(it.javaTypeName(context), it.name.decapitalize().escapeJavaReservedWord()).build()
        })
        .addCode(fieldInitializeCodeBuilder.build())
        .build()
    )
  }

  private fun Any?.isEmptyList(): Boolean {
    return if (this is List<*>) {
      if (isEmpty()) {
        true
      } else {
        first()?.isEmptyList() == true
      }
    } else {
      false
    }
  }
  private fun TypeSpec.Builder.addBuilder(): TypeSpec.Builder {
    if (fields.isEmpty()) {
      return this
    } else {
      val builderFields = fields.map { it.name.decapitalize().escapeJavaReservedWord() to it.javaTypeName(context) }
      val builderFieldDefaultValues = fields
          .filter {
            // ignore any custom type or object default values for now as we don't support them
            val normalizedType = it.type.removeSuffix("!").removeSurrounding("[", "]").removeSuffix("!")
            !normalizedType.isCustomScalarType(context)
                && (!normalizedType.isInputObject(context) || it.defaultValue.isEmptyList())
          }
          .associate { it.name.decapitalize().escapeJavaReservedWord() to it.defaultValue }
      val javaDocs = fields
          .filter { it.description.isNotBlank() }
          .associate { it.name.decapitalize().escapeJavaReservedWord() to it.description }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(
              BuilderTypeSpecBuilder(
                  targetObjectClassName = objectClassName,
                  fields = builderFields,
                  fieldDefaultValues = builderFieldDefaultValues,
                  fieldJavaDocs = javaDocs,
                  typeDeclarations = context.typeDeclarations
              ).build()
          )
    }
  }

  private fun marshallerMethodSpec(): MethodSpec {
    val writeCode = fields
        .map {
          InputFieldSpec.build(
              name = it.name,
              graphQLType = it.type,
              context = context
          )
        }
        .map {
          it.writeValueCode(
              writerParam = CodeBlock.of("\$L", WRITER_PARAM.name),
              marshaller = CodeBlock.of("$MARSHALLER_PARAM_NAME()")
          )
        }
        .fold(CodeBlock.builder(), CodeBlock.Builder::add)
        .build()
    val methodSpec = MethodSpec.methodBuilder("marshal")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(WRITER_PARAM)
        .addException(IOException::class.java)
        .addCode(writeCode)
        .build()
    val marshallerType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(InputFieldMarshaller::class.java)
        .addMethod(methodSpec)
        .build()
    return MethodSpec.methodBuilder(MARSHALLER_PARAM_NAME)
        .addAnnotation(Annotations.OVERRIDE)
        .addModifiers(Modifier.PUBLIC)
        .returns(InputFieldMarshaller::class.java)
        .addStatement("return \$L", marshallerType)
        .build()
  }

  private fun TypeSpec.Builder.addFields(): TypeSpec.Builder {
    fun addFieldDefinition(field: TypeDeclarationField) {
      addField(FieldSpec
          .builder(field.javaTypeName(context), field.name.decapitalize().escapeJavaReservedWord())
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .build())
    }

    fun addFieldAccessor(field: TypeDeclarationField) {
      val optional = !field.type.endsWith("!")
      addMethod(MethodSpec.methodBuilder(field.name.decapitalize().escapeJavaReservedWord())
          .addModifiers(Modifier.PUBLIC)
          .returns(field.javaTypeName(context).unwrapOptionalType())
          .let {
            if (!field.description.isNullOrBlank())
              it.addJavadoc(CodeBlock.of("\$L\n", field.description))
            else
              it
          }
          .addStatement("return this.\$L\$L", field.name.decapitalize().escapeJavaReservedWord(), if (optional) ".value" else "")
          .build())
    }

    fields.forEach { field ->
      addFieldDefinition(field)
      addFieldAccessor(field)
    }

    return this
  }

  private fun TypeDeclarationField.javaTypeName(context: CodeGenerationContext): TypeName {
    return JavaTypeResolver(context, context.ir.typesPackageName)
        .resolve(typeName = type, nullableValueType = NullableValueType.INPUT_TYPE)
  }

  companion object {
    private val WRITER_PARAM = ParameterSpec.builder(InputFieldWriter::class.java, "writer").build()
    private const val MARSHALLER_PARAM_NAME = "marshaller"
  }
}
