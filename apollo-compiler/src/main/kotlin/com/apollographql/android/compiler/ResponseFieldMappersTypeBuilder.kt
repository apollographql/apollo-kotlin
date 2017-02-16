package com.apollographql.android.compiler

import com.apollographql.android.api.graphql.ResponseFieldMapper
import com.squareup.javapoet.*
import java.lang.reflect.Type
import java.util.*
import javax.lang.model.element.Modifier

/**
 * Generates map initialized with all POJO mappers:
 *
 * Example of generated class:
 *
 * ```
 * public final class ResponseFieldMappers {
 *   public static final Map<Type, ResponseFieldMapper> MAPPERS = Collections.unmodifiableMap(
 *     new HashMap<Type, ResponseFieldMapper>() {
 *       {
 *         put(TestQuery1.Data.class, new TestQuery1.Data.Mapper(TestQuery1.Data.FACTORY));
 *         put(TestQuery2.Data.class, new TestQuery2.Data.Mapper(TestQuery2.Data.FACTORY));
 *         put(TestQuery3.Data.class, new TestQuery3.Data.Mapper(TestQuery3.Data.FACTORY));
 *       }
 *     });
 *
 *   private ResponseFieldMappers() {
 *   }
 * }
 * ```
 */
class ResponseFieldMappersTypeBuilder(val operationJavaClasses: List<ClassName>) {
  fun build(): TypeSpec {
    return TypeSpec.classBuilder("ResponseFieldMappers")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addField(FieldSpec
            .builder(ClassNames.parameterizedMapOf(Type::class.java, ResponseFieldMapper::class.java), "MAPPERS")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer(mapperInitializerCode())
            .build())
        .build()
  }

  private fun mapperInitializerCode() =
      CodeBlock.builder()
          .add("\$T.unmodifiableMap(\n", Collections::class.java)
          .indent()
          .add("\$L", operationDataMappers())
          .unindent()
          .add(")")
          .build()

  private fun operationDataMappers() =
      TypeSpec.anonymousClassBuilder("")
          .addSuperinterface(ClassNames.parameterizedHashMapOf(Type::class.java, ResponseFieldMapper::class.java))
          .addInitializerBlock(operationJavaClasses
              .map { CodeBlock.of("put(\$T.Data.class, new \$T.Data.Mapper(\$T.Data.FACTORY));\n", it, it, it) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .build()
}