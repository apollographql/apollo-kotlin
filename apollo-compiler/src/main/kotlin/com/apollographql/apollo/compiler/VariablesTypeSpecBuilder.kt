package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Variable
import com.squareup.javapoet.*
import java.io.IOException
import java.util.*
import javax.lang.model.element.Modifier

class VariablesTypeSpecBuilder(
    val variables: List<Variable>,
    val context: CodeGenerationContext
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(VARIABLES_CLASS_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .superclass(ClassNames.GRAPHQL_OPERATION_VARIABLES)
        .addFields(variableFieldSpecs())
        .addField(valueMapFieldSpec())
        .addMethod(constructor())
        .addMethods(variableAccessorMethodSpecs())
        .addMethod(valueMapAccessorMethodSpec())
        .addMethod(marshallerMethodSpec())
        .build()
  }

  private fun variableFieldSpecs(): List<FieldSpec> {
    return variables.map { variable ->
      FieldSpec.builder(variable.javaTypeName(context), variable.name.decapitalize())
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .build()
    }
  }

  private fun valueMapFieldSpec(): FieldSpec {
    return FieldSpec.builder(ClassNames.parameterizedMapOf(java.lang.String::class.java, Object::class.java),
        VALUE_MAP_FIELD_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.TRANSIENT)
        .initializer("new \$T<>()", LinkedHashMap::class.java)
        .build()
  }

  private fun constructor(): MethodSpec {
    val fieldInitializeCode = variables.map {
      CodeBlock.of("this.\$L = \$L;\n", it.name.decapitalize(), it.name.decapitalize())
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    val fieldMapInitializeCode = variables.map { variable ->
      val javaType = variable.javaTypeName(context)
      CodeBlock.builder()
          .apply {
            if (javaType.isOptional()) {
              beginControlFlow("if (\$L.defined)", variable.name.decapitalize())
                  .addStatement("this.valueMap.put(\$S, \$L.value)", variable.name, variable.name.decapitalize())
                  .endControlFlow()
            } else {
              addStatement("this.valueMap.put(\$S, \$L)", variable.name, variable.name.decapitalize())
            }
          }
          .build()
    }.fold(CodeBlock.builder(), CodeBlock.Builder::add)

    return MethodSpec.constructorBuilder()
        .addParameters(variables.map {
          ParameterSpec.builder(it.javaTypeName(context), it.name.decapitalize()).build()
        })
        .addCode(fieldInitializeCode.build())
        .addCode(fieldMapInitializeCode.build())
        .build()
  }

  private fun variableAccessorMethodSpecs(): List<MethodSpec> {
    return variables.map { variable ->
      MethodSpec
          .methodBuilder(variable.name.decapitalize())
          .addModifiers(Modifier.PUBLIC)
          .returns(variable.javaTypeName(context))
          .addStatement("return \$L", variable.name.decapitalize())
          .build()
    }
  }

  private fun valueMapAccessorMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(VALUE_MAP_FIELD_NAME)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .returns(ClassNames.parameterizedMapOf(java.lang.String::class.java, Object::class.java))
        .addStatement("return \$T.unmodifiableMap(\$L)", Collections::class.java, VALUE_MAP_FIELD_NAME)
        .build()
  }

  fun TypeSpec.Builder.builder(): TypeSpec.Builder {
    if (variables.isEmpty()) {
      return this
    } else {
      val builderFields = variables.map { it.name.decapitalize() to it.javaTypeName(context) }
      return addMethod(BuilderTypeSpecBuilder.builderFactoryMethod())
          .addType(
              BuilderTypeSpecBuilder(
                  targetObjectClassName = VARIABLES_TYPE_NAME,
                  fields = builderFields,
                  fieldDefaultValues = emptyMap(),
                  fieldJavaDocs = emptyMap(),
                  typeDeclarations = context.typeDeclarations
              ).build()
          )
    }
  }

  private fun marshallerMethodSpec(): MethodSpec {
    val writeCode = variables
        .map {
          InputFieldSpec.build(
              name = it.name.decapitalize(),
              graphQLType = it.type,
              context = context,
              nullableValueType = NullableValueType.INPUT_TYPE
          )
        }
        .map {
          it.writeValueCode(
              writerParam = CodeBlock.of("\$L", WRITER_PARAM.name),
              marshaller = CodeBlock.of("\$L()", MARSHALLER_PARAM_NAME)
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
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC)
        .returns(InputFieldMarshaller::class.java)
        .addStatement("return \$L", marshallerType)
        .build()
  }

  private fun Variable.javaTypeName(context: CodeGenerationContext): TypeName {
    return JavaTypeResolver(context.copy(nullableValueType = NullableValueType.INPUT_TYPE), context.typesPackage)
        .resolve(type)
  }

  companion object {
    private val VARIABLES_CLASS_NAME: String = "Variables"
    private val VARIABLES_TYPE_NAME: ClassName = ClassName.get("", VARIABLES_CLASS_NAME)
    private val VALUE_MAP_FIELD_NAME = "valueMap"
    private val WRITER_PARAM = ParameterSpec.builder(InputFieldWriter::class.java, "writer").build()
    private const val MARSHALLER_PARAM_NAME = "marshaller"
  }
}
