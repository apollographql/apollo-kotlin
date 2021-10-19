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
import com.apollographql.apollo3.compiler.codegen.kotlin.test.TBuilderBuilder
import com.apollographql.apollo3.compiler.ir.IrFieldInfo
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.PossibleTypes
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

  private val testBuildersBuilder = dataModelGroup.toTBuilders().single().maybeFlatten(flatten).map {
    TBuilderBuilder(
        context = context,
        tbuilder = it,
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
            testBuildersBuilder.map { it.build() }
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

internal data class TBuilder(
    val kotlinName: String,
    val id: String,
    val possibleTypes: PossibleTypes,
    val properties: List<IrFieldInfo>,
    val ctors: List<TCtor>,
    val nestedTBuilders: List<TBuilder>
)

internal data class TCtor(
    val kotlinName: String,
    val id: String,
)

internal fun IrModel.toTBuilder(): TBuilder {
  val nestedBuilders = modelGroups.flatMap { it.toTBuilders() }
  return TBuilder(
      kotlinName = modelName,
      properties = properties.map { it.info },
      id = id,
      ctors = nestedBuilders.map { TCtor(it.kotlinName, it.id) },
      nestedTBuilders = nestedBuilders,
      possibleTypes = possibleTypes
  )
}

internal fun IrModelGroup.toTBuilders(): List<TBuilder> {
  return models.filter { !it.isInterface }.map {
    it.toTBuilder()
  }
}

internal fun TBuilder.maybeFlatten(flatten: Boolean): List<TBuilder> {
  if (!flatten) {
    return listOf(this)
  }
  return listOf(this.copy(nestedTBuilders = emptyList())) + nestedTBuilders.flatMap { it.maybeFlatten(flatten) }
}