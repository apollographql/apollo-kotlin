package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.compiler.codegen.ClassNames
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.Data
import com.apollographql.apollo3.compiler.codegen.Identifier.block
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.testResolver
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgTestFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodegenLayout
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinMemberNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.test.TBuilderBuilder
import com.apollographql.apollo3.compiler.decapitalizeFirstLetter
import com.apollographql.apollo3.compiler.ir.IrModel
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrModelType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.IrProperty
import com.apollographql.apollo3.compiler.ir.IrType
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
) : CgTestFileBuilder {
  private val packageName = context.layout.operationTestBuildersPackageName(operation.filePath)
  private val simpleName = context.layout.operationTestBuildersWrapperName(operation)

  private val testBuildersBuilder = dataModelGroup.toTBuilders(context.layout).single().maybeFlatten(flatten, mutableSetOf()).map {
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
        .addAnnotation(ClassNames.ApolloExperimental)
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
            ParameterSpec.builder(testResolver, KotlinSymbols.TestResolver)
                .defaultValue(CodeBlock.of("%T()", KotlinSymbols.DefaultTestResolver))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
                .defaultValue(CodeBlock.of("%T.Empty", KotlinSymbols.CustomScalarAdapters))
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                block,
                LambdaTypeName.get(
                    receiver = context.resolver.resolveTestBuilder(dataModelGroup.baseModelId),
                    parameters = emptyArray<TypeName>(),
                    returnType = KotlinSymbols.Unit
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

    builder.beginControlFlow("return %M(${testResolver})", KotlinMemberNames.withTestResolver)

    builder.add(
        "%L.$fromJson(\n",
        context.resolver.adapterInitializer(
            IrNonNullType(IrModelType(operation.dataModelGroup.baseModelId)),
            requiresBuffering = false
        )
    )
    builder.indent()
    builder.add("%T(%T().apply($block).build()),\n",
        KotlinSymbols.MapJsonReader,
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
    val properties: List<TProperty>,
    val nestedTBuilders: List<TBuilder>,
)

internal data class TProperty(
    /**
     * Properties taken from [com.apollographql.apollo3.compiler.ir.IrProperty]
     */
    val responseName: String,
    val description: String?,
    val type: IrType,
    val gqlType: GQLType?,
    val deprecationReason: String?,

    val ctors: List<TCtor>,
)

internal data class TCtor(
    val kotlinName: String,
    val id: String,
)

private fun IrProperty.tProperty(modelGroups: List<IrModelGroup>): TProperty {
  val leafPath = (info.type.leafType() as? IrModelType)?.path

  /**
   * Lookup the modelGroup for this property
   * This feels a bit weird because this is information we had before the tree gets split into properties and models
   * We might be able to remove that lookup
   */

  val modelGroup = if (leafPath != null) {
    modelGroups.single { it.baseModelId == leafPath }
  } else {
    null
  }

  return TProperty(
      responseName = info.responseName,
      type = info.type,
      description = info.description,
      deprecationReason = info.deprecationReason,
      gqlType = info.gqlType ?: error("Synthetic fields do not belong in the Test Builders"),
      ctors = modelGroup?.models?.filter { !it.isInterface }?.map { TCtor(it.modelName.decapitalizeFirstLetter(), it.id) } ?: emptyList(),
  )

}

private fun resolveNameClashes(usedNames: MutableSet<String>, modelName: String): String {
  var i = 0
  var name = modelName
  while (usedNames.contains(name)) {
    i++
    name = "$modelName$i"
  }
  usedNames.add(name)
  return name
}


internal fun IrModel.toTBuilder(layout: KotlinCodegenLayout): TBuilder {
  val nestedBuilders = modelGroups.flatMap { it.toTBuilders(layout) }
  return TBuilder(
      kotlinName = layout.testBuilder(modelName),
      properties = properties.map { it.tProperty(modelGroups) },
      id = id,
      nestedTBuilders = nestedBuilders,
      possibleTypes = possibleTypes
  )
}

internal fun IrModelGroup.toTBuilders(layout: KotlinCodegenLayout): List<TBuilder> {
  return models.filter { !it.isInterface }.map {
    it.toTBuilder(layout)
  }
}

internal fun TBuilder.maybeFlatten(flatten: Boolean, usedNames: MutableSet<String>): List<TBuilder> {
  if (!flatten) {
    return listOf(this)
  }
  return listOf(
      this.copy(
          kotlinName = resolveNameClashes(usedNames, kotlinName),
          nestedTBuilders = emptyList()
      )
  ) + nestedTBuilders.flatMap { it.maybeFlatten(flatten, usedNames) }
}
