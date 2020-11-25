package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.FragmentType.typeSpec(generateAsInternal: Boolean): TypeSpec {
  return TypeSpec
      .interfaceBuilder(this.name.escapeKotlinReservedWord())
      .addAnnotation(suppressWarningsAnnotation)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addSuperinterface(GraphqlFragment::class)
      .applyIf(this.description.isNotBlank()) { addKdoc("%L\n", this@typeSpec.description) }
      .addProperties(this.interfaceType.fields.map { field -> field.asPropertySpec() })
      .addTypes(
          this.interfaceType.nestedObjects.map { nestedObject ->
            nestedObject.typeSpec()
          }
      )
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(PropertySpec.builder("FRAGMENT_DEFINITION", String::class)
              .initializer(CodeBlock.of("%S", fragmentDefinition))
              .build()
          )
          // FIXME
//      .addType(this.implementationType.typeSpec())
//          .addFunction(
//              FunSpec.builder("invoke")
//                  .addModifiers(KModifier.OPERATOR)
//                  .returns(ClassName.bestGuess(fragmentType.name))
//                  .addParameter(ParameterSpec.builder("reader", ResponseReader::class).build())
//                  .addStatement(
//                      "return·%T.fromResponse(reader)",
//                      rootType.asAdapterTypeName(),
//                  )
//                  .build()
//          )
//          .addFunction(
//              FunSpec.builder("Mapper")
//                  .returns(ResponseFieldMapper::class.asTypeName().parameterizedBy(ClassName(packageName = "", fragmentType.name)))
//                  .beginControlFlow("return·%T·{·reader·->", ResponseFieldMapper::class)
//                  .addStatement("%T.fromResponse(reader)", rootType.asAdapterTypeName())
//                  .endControlFlow()
//                  .build()
//          )
          .build()
      )
      .build()
}
