package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ResponseFieldMapper
import com.apollographql.android.api.graphql.ResponseReader
import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.*
import java.io.IOException
import javax.annotation.Nonnull
import javax.lang.model.element.Modifier

class FragmentsResponseMapperBuilder(
    val fragments: List<String>,
    val codeGenerationContext: CodeGenerationContext
) {
  fun build(): TypeSpec {
    val contentValueFields = fragments.map {
      FieldSpec.builder(ClassName.get(codeGenerationContext.fragmentsPackage, it.capitalize()), it.toLowerCase())
          .build()
    }
    return TypeSpec.classBuilder("Mapper")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(RESPONSE_FIELD_MAPPER_TYPE)
        .addMethod(constructor())
        .addField(FACTORY_FIELD)
        .addField(FieldSpec.builder(CONDITIONAL_TYPE_PARAM.type, CONDITIONAL_TYPE_PARAM.name).build())
        .addMethod(mapMethod(contentValueFields))
        .build()
  }

  private fun constructor(): MethodSpec =
      MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameter(FACTORY_PARAM)
          .addParameter(CONDITIONAL_TYPE_PARAM)
          .addStatement("this.\$L = \$L", FACTORY_PARAM.name, FACTORY_PARAM.name)
          .addStatement("this.\$L = \$L", CONDITIONAL_TYPE_PARAM.name, CONDITIONAL_TYPE_PARAM.name)
          .build()

  private fun mapMethod(contentValueFields: List<FieldSpec>) =
      MethodSpec.methodBuilder("map")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override::class.java)
          .addParameter(READER_PARAM)
          .addException(IOException::class.java)
          .returns(SchemaTypeSpecBuilder.FRAGMENTS_TYPE)
          .addCode(mapMethodCode(contentValueFields))
          .build()

  private fun mapMethodCode(contentValueFields: List<FieldSpec>) =
      CodeBlock.builder()
          .add(contentValueFields
              .fold(CodeBlock.builder()) { builder, field -> builder.addStatement("\$T \$N = null", field.type, field) }
              .build())
          .add(contentValueFields
              .fold(CodeBlock.builder()) { builder, field ->
                builder
                    .beginControlFlow("if (\$L.equals(\$T.TYPE_CONDITION))", CONDITIONAL_TYPE_VAR, field.type)
                    .addStatement("\$N = new \$T.Mapper(\$N.\$L\$L()).map(\$N)", field, field.type, FACTORY_PARAM,
                        (field.type as ClassName).simpleName().decapitalize(), Util.FACTORY_TYPE_NAME,
                        READER_PARAM)
                    .endControlFlow()
              }.build())
          .add("return \$L.\$L().\$L(", FACTORY_VAR, Util.FACTORY_CREATOR_ACCESS_METHOD_NAME,
              Util.CREATOR_CREATE_METHOD_NAME)
          .add(contentValueFields
              .mapIndexed { i, fieldSpec -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", fieldSpec.name) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(");\n")
          .build()

  companion object {
    private val API_RESPONSE_FIELD_MAPPER_TYPE = ClassName.get(ResponseFieldMapper::class.java)
    private val RESPONSE_FIELD_MAPPER_TYPE = ParameterizedTypeName.get(API_RESPONSE_FIELD_MAPPER_TYPE,
        SchemaTypeSpecBuilder.FRAGMENTS_TYPE)
    private val FACTORY_VAR = Util.FACTORY_TYPE_NAME.decapitalize()
    private val FACTORY_PARAM = ParameterSpec.builder(Util.FACTORY_INTERFACE_TYPE, FACTORY_VAR)
        .addAnnotation(Nonnull::class.java).build()
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
    private val CONDITIONAL_TYPE_PARAM = ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR)
        .addAnnotation(Nonnull::class.java).build()
    private val FACTORY_FIELD = FieldSpec.builder(Util.FACTORY_INTERFACE_TYPE, Util.FACTORY_TYPE_NAME.decapitalize(),
        Modifier.FINAL).build()
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, "reader").build()
  }
}