package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.FragmentResponseFieldMapper
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Fragment
import com.squareup.javapoet.*
import javax.annotation.Nonnull
import javax.lang.model.element.Modifier

/**
 * Responsible for [Fragments.Mapper] class generation
 *
 * Example of generated class:
 *
 * ```
 * public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
 *   final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();
 *
 *   @Override
 *   public Fragments map(ResponseReader reader, @Nonnull String conditionalType) {
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
    val fragments: List<String>,
    val context: CodeGenerationContext
) {
  fun build(): TypeSpec {
    val fragmentFields = fragments.map { FieldSpec.builder(fragmentType(it), it.decapitalize()).build() }
    return TypeSpec.classBuilder(Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(RESPONSE_FIELD_MAPPER_TYPE)
        .addFields(mapperFields(fragmentFields))
        .addMethod(mapMethod(fragmentFields))
        .build()
  }

  private fun fragmentType(fragmentName: String) =
      ClassName.get(context.fragmentsPackage, fragmentName.capitalize())

  private fun mapMethod(fragmentFields: List<FieldSpec>) =
      MethodSpec.methodBuilder("map")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override::class.java)
          .addParameter(READER_PARAM)
          .addParameter(CONDITIONAL_TYPE_PARAM)
          .returns(SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type)
          .addCode(mapMethodCode(fragmentFields))
          .build()

  private fun mapMethodCode(fragmentFields: List<FieldSpec>) =
      CodeBlock.builder()
          .add(initFragmentsCode(fragmentFields))
          .add(createFragmentsCode(fragmentFields))
          .add(");\n")
          .build()

  private fun initFragmentsCode(fragmentFields: List<FieldSpec>) =
      CodeBlock.builder()
          .add(fragmentFields
              .fold(CodeBlock.builder()) { builder, field -> builder.addStatement("\$T \$N = null", field.type, field) }
              .build())
          .add(fragmentFields
              .fold(CodeBlock.builder()) { builder, field -> builder.add(initFragmentCode(field)) }
              .build())
          .build()

  private fun initFragmentCode(fragmentField: FieldSpec): CodeBlock {
    val fieldClass = fragmentField.type as ClassName
    return CodeBlock.builder()
        .beginControlFlow("if (\$T.\$L.contains(\$L))", fieldClass, Fragment.POSSIBLE_TYPES_VAR, CONDITIONAL_TYPE_VAR)
        .addStatement("\$N = \$L.map(\$L)", fragmentField, fieldClass.mapperFieldName(), READER_VAR)
        .endControlFlow()
        .build()
  }

  private fun createFragmentsCode(fragmentFields: List<FieldSpec>) =
      CodeBlock.builder()
          .add("return new \$L(", SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type.withoutAnnotations())
          .add(fragmentFields
              .mapIndexed { i, fieldSpec -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", fieldSpec.name) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .build()

  private fun mapperFields(fragments: List<FieldSpec>) =
      fragments
          .map { it.type as ClassName }
          .map {
            val mapperClassName = ClassName.get(context.fragmentsPackage, it.simpleName(),
                Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
            FieldSpec.builder(mapperClassName, it.mapperFieldName(), Modifier.FINAL)
                .initializer(CodeBlock.of("new \$T()", mapperClassName))
                .build()
          }

  companion object {
    private val API_RESPONSE_FIELD_MAPPER_TYPE = ClassName.get(
        FragmentResponseFieldMapper::class.java)
    private val RESPONSE_FIELD_MAPPER_TYPE = ParameterizedTypeName.get(API_RESPONSE_FIELD_MAPPER_TYPE,
        SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type.withoutAnnotations())
    private val CONDITIONAL_TYPE_VAR = "conditionalType"
    private val CONDITIONAL_TYPE_PARAM = ParameterSpec.builder(String::class.java, CONDITIONAL_TYPE_VAR)
        .addAnnotation(Nonnull::class.java).build()
    private val READER_VAR = "reader"
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, READER_VAR).build()
  }
}