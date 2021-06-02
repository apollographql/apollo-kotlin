package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.Identifier.OPERATION_DOCUMENT
import com.apollographql.apollo3.compiler.codegen.Identifier.OPERATION_ID
import com.apollographql.apollo3.compiler.codegen.Identifier.OPERATION_NAME
import com.apollographql.apollo3.compiler.codegen.Identifier.document
import com.apollographql.apollo3.compiler.codegen.Identifier.id
import com.apollographql.apollo3.compiler.codegen.Identifier.name
import com.apollographql.apollo3.compiler.codegen.helpers.makeDataClass
import com.apollographql.apollo3.compiler.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.codegen.model.ModelBuilder
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.IrOperationType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class OperationBuilder(
    private val context: CgContext,
    private val generateFilterNotNull: Boolean,
    private val operationId: String,
    private val generateQueryDocument: Boolean,
    private val operation: IrOperation,
    flatten: Boolean,
    flattenNamesInOrder: Boolean
): CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.operationPackageName(operation.filePath)
  private val simpleName = layout.operationName(operation)

  private val dataSuperClassName = when (operation.operationType) {
    IrOperationType.Query -> Query.Data::class
    IrOperationType.Mutation -> Mutation.Data::class
    IrOperationType.Subscription -> Subscription.Data::class
  }.asClassName()

  private val modelBuilders = operation.dataModelGroup.maybeFlatten(flatten, flattenNamesInOrder).flatMap {
    it.models
  }.map {
    ModelBuilder(
        context = context,
        model = it,
        superClassName = if (it.id == operation.dataModelGroup.baseModelId) dataSuperClassName else null,
        path = listOf(packageName, simpleName)
    )
  }
  
  override fun prepare() {
    context.resolver.registerOperation(
        operation.name,
        ClassName(packageName, simpleName)
    )
    modelBuilders.forEach { it.prepare() }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(layout.operationName(operation))
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(operation.description)
        .makeDataClass(operation.variables.map { it.toNamedType().toParameterSpec(context) })
        .addFunction(operationIdFunSpec())
        .addFunction(queryDocumentFunSpec(generateQueryDocument))
        .addFunction(nameFunSpec())
        .addFunction(serializeVariablesFunSpec())
        .addFunction(adapterFunSpec())
        .addFunction(fieldSetsFunSpec())
        .addTypes(dataTypeSpecs())
        .addType(companionTypeSpec())
        .build()
        .maybeAddFilterNotNull(generateFilterNotNull)
  }

  private fun serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
      adapterClassName = context.resolver.resolveOperationVariablesAdapter(operation.name),
      emptyMessage = "This operation doesn't have any variable"
  )

  private fun adapterFunSpec(): FunSpec {
    return adapterFunSpec(
        adapterTypeName = context.resolver.resolveModelAdapter(operation.dataModelGroup.baseModelId),
        adaptedTypeName = context.resolver.resolveModel(operation.dataModelGroup.baseModelId)
    )
  }

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map {
      it.build()
    }
  }

  private fun superInterfaceType(): TypeName {
    return when (operation.operationType) {
      IrOperationType.Query -> Query::class.asTypeName()
      IrOperationType.Mutation -> Mutation::class.asTypeName()
      IrOperationType.Subscription -> Subscription::class.asTypeName()
    }.parameterizedBy(
        context.resolver.resolveModel(operation.dataModelGroup.baseModelId)
    )
  }

  private fun operationIdFunSpec() = FunSpec.builder(id)
      .addModifiers(KModifier.OVERRIDE)
      .returns(String::class)
      .addStatement("return $OPERATION_ID")
      .build()

  private fun queryDocumentFunSpec(generateQueryDocument: Boolean) = FunSpec.builder(document)
      .addModifiers(KModifier.OVERRIDE)
      .returns(String::class)
      .apply {
        if (generateQueryDocument) {
          addStatement("return $OPERATION_DOCUMENT")
        } else {
          addStatement("error(\"The query document was removed from this operation. Use generateQueryDocument = true if you need it\"")
        }
      }
      .build()

  private fun nameFunSpec() = FunSpec.builder(name)
      .addModifiers(KModifier.OVERRIDE)
      .returns(String::class)
      .addStatement("return OPERATION_NAME")
      .build()

  private fun companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(PropertySpec.builder(OPERATION_ID, String::class)
            .addModifiers(KModifier.CONST)
            .initializer("%S", operationId)
            .build()
        )
        .applyIf(generateQueryDocument) {
          addProperty(PropertySpec.builder(OPERATION_DOCUMENT, String::class)
              .addModifiers(KModifier.CONST)
              .initializer("%S", QueryDocumentMinifier.minify(operation.sourceWithFragments))
              .addKdoc("""
                The minimized GraphQL document being sent to the server to save a few bytes.
                The un-minimized version is:


                """.trimIndent() + operation.sourceWithFragments.escapeKdoc()
              )
              .build()
          )
        }
        .addProperty(PropertySpec
            .builder(OPERATION_NAME, String::class)
            .addModifiers(KModifier.CONST)
            .initializer("%S", operation.name)
            .build()
        )
        .build()
  }

  /**
   * Things like `[${'$'}oo]` do not compile. See https://youtrack.jetbrains.com/issue/KT-43906
   */
  private fun String.escapeKdoc(): String {
    return replace("[", "\\[").replace("]", "\\]")
  }

  private fun fieldSetsFunSpec(): FunSpec {
    return selectionsFunSpec(
        context.resolver.resolveOperationSelections(operation.name)
    )
  }
}

