package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.Data
import com.apollographql.apollo3.compiler.codegen.Identifier.block
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.testResolver
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinMemberNames
import com.apollographql.apollo3.compiler.codegen.kotlin.test.TestBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

class TestBuildersBuilder(
    val context: KotlinContext,
    val dataModelGroup: IrModelGroup,
    val operation: IrOperation,
    val flatten: Boolean,
) : CgFileBuilder {
  private val packageName = context.layout.operationTestBuildersPackageName(operation.filePath)
  private val simpleName = context.layout.operationTestBuildersWrapperName(operation)

  private val testBuildersBuilder = dataModelGroup.maybeFlatten(flatten).map {
    TestBuilderBuilder(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName),
        inner = false
    )
  }

  override fun prepare() {
    testBuildersBuilder.forEach { it.prepare() }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addTypes(
            testBuildersBuilder.flatMap { it.build() }
        )
        .addFunction(
            dataExtension()
        )
        .build()
  }

  private fun dataExtension(): FunSpec {
    return FunSpec.builder(Data)
        .receiver(context.resolver.resolveOperation(operation.name).nestedClass(Identifier.Companion))
        .addParameter(
            ParameterSpec.builder(testResolver, KotlinClassNames.TestResolver)
                .defaultValue(CodeBlock.of("%T()", KotlinClassNames.DefaultTestResolver))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(customScalarAdapters, KotlinClassNames.CustomScalarAdapters)
                .defaultValue(CodeBlock.of("%T.Empty", KotlinClassNames.CustomScalarAdapters))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                block,
                LambdaTypeName.get(
                    receiver = context.resolver.resolveTestBuilder(dataModelGroup.baseModelId),
                    parameters = emptyArray<TypeName>(),
                    returnType = KotlinClassNames.Unit
                )
            )
                .build()
        )
        .returns(context.resolver.resolveModel(operation.dataModelGroup.baseModelId))
        .addCode(dataExtensionBody())
        .build()
  }

  /**
   * fun SimpleQuery.Companion.Data(
   *     testResolver: TestResolver = DefaultTestResolver(),
   *     customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
   *     block: SimpleQueryBuilders.Data.() -> Unit,
   * ): SimpleQuery.Data {
   *   return withTestResolver(testResolver) {
   *     SimpleQuery_ResponseAdapter.Data.fromJson(
   *         MapJsonReader(SimpleQueryBuilders.Data().apply(block).build()),
   *         customScalarAdapters
   *     )
   *   }
   * }
   */
  private fun dataExtensionBody(): CodeBlock {

    val builder = CodeBlock.builder()

    builder.beginControlFlow("return %M(${testResolver}", KotlinMemberNames.withTestResolver)

    builder.add("%T.$fromJson(\n", context.resolver.resolveModelAdapter(operation.dataModelGroup.baseModelId))
    builder.indent()
    builder.add("%T(%T().apply($block).build()),\n",
        KotlinClassNames.MapJsonReader,
        context.resolver.resolveTestBuilder(dataModelGroup.baseModelId)
    )
    builder.add("$customScalarAdapters,\n")
    builder.unindent()
    builder.add(")\n")
    builder.endControlFlow()
    return builder.build()
  }
}
