package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

/**
 * Responsible for Fragments.Mapper class generation
 *
 * Example of generated class:
 *
 * ```
 * public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
 *   final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();
 *
 *   @Override
 *   public Fragments map(ResponseReader reader, @NotNull String conditionalType) {
 *     HeroDetails heroDetails = null;
 *     if (conditionalType.equals(HeroDetails.TYPE_CONDITION)) {
 *       heroDetails = heroDetailsFieldMapper.map(reader);
 *     }
 *     return new Fragments(heroDetails);
 *   }
 * }
 *
 *```
 */
class FragmentsResponseMapperBuilder(
    private val fragmentFields: List<ResponseFieldSpec>,
    private val context: CodeGenerationContext
) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder(Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(RESPONSE_FIELD_MAPPER_TYPE)
        .addFields(mapperFields(fragmentFields))
        .addMethod(mapMethod(fragmentFields))
        .build()
  }

  private fun mapMethod(fragmentFields: List<ResponseFieldSpec>): MethodSpec {
    val code = CodeBlock.builder()
        .add(fragmentFields
            .mapIndexed { i, field ->
              field.readValueCode(
                  readerParam = CodeBlock.of("\$L", READER_PARAM.name),
                  fieldParam = CodeBlock.of("\$L[\$L]", "\$responseFields", i))
            }
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add("return new \$T(", SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type.withoutAnnotations())
        .add(fragmentFields
            .mapIndexed { i, field -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", field.fieldSpec.name) }
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add(");\n")
        .build()
    return MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(READER_PARAM)
        .returns(SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type)
        .addCode(code)
        .build()
  }

  private fun mapperFields(fragmentFields: List<ResponseFieldSpec>): List<FieldSpec> {
    return fragmentFields.map { field ->
      val fieldType = field.normalizedFieldSpec.type as ClassName
      val mapperClassName = ClassName.get(fieldType.packageName(), fieldType.simpleName(),
          Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
      FieldSpec.builder(mapperClassName, fieldType.mapperFieldName(), Modifier.FINAL)
          .initializer(CodeBlock.of("new \$T()", mapperClassName))
          .build()
    }
  }

  companion object {
    private val API_RESPONSE_FIELD_MAPPER_TYPE = ClassName.get(ResponseFieldMapper::class.java)
    private val RESPONSE_FIELD_MAPPER_TYPE = ParameterizedTypeName.get(API_RESPONSE_FIELD_MAPPER_TYPE,
        SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type.withoutAnnotations())
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, "reader").build()
  }
}
