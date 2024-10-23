package com.apollographql.apollo.compiler.codegen.kotlin.operations

import com.apollographql.apollo.ast.QueryDocumentMinifier
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.CgImport
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOperationsContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.builderTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.dataBuilderCtor
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.makeClassFromParameters
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddJsExport
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.adapterFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.maybeAddFilterNotNull
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.rootFieldFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.operations.util.serializeVariablesFunSpec
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.codegen.operationName
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrOperation
import com.apollographql.apollo.compiler.ir.IrOperationType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class OperationBuilder(
    private val context: KotlinOperationsContext,
    private val generateFilterNotNull: Boolean,
    private val operationId: String,
    private val generateQueryDocument: Boolean,
    private val operation: IrOperation,
    flatten: Boolean,
    private val addJvmOverloads: Boolean,
    val generateDataBuilders: Boolean,
    val generateInputBuilders: Boolean
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.executableDocumentPackageName(operation.normalizedFilePath)
  private val simpleName = layout.operationName(operation)

  private val dataSuperClassName = when (operation.operationType) {
    is IrOperationType.Query -> KotlinSymbols.QueryData
    is IrOperationType.Mutation -> KotlinSymbols.MutationData
    is IrOperationType.Subscription -> KotlinSymbols.SubscriptionData
  }

  private val modelBuilders = operation.dataModelGroup.maybeFlatten(flatten).flatMap {
    it.models
  }.map {
    ModelBuilder(
        context = context,
        model = it,
        superClassName = if (it.id == operation.dataModelGroup.baseModelId) dataSuperClassName else null,
        path = listOf(packageName, simpleName),
        hasSubclassesInSamePackage = true,
        adaptableWith = if (it.id == operation.dataModelGroup.baseModelId) it.id else null,
        reservedNames = setOf("Companion")
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
    val className = context.resolver.resolveSchemaType(operation.typeCondition)
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec()),
        imports = listOf(
            CgImport(
                className,
                "Compiled${className.simpleName.capitalizeFirstLetter()}"
            )

        )
    )
  }

  fun typeSpec(): TypeSpec {
    val namedTypes = operation.variables.map { it.toNamedType() }
    return TypeSpec.classBuilder(layout.operationName(operation))
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(operation.description)
        .makeClassFromParameters(
            context.generateMethods,
            namedTypes.map { it.toParameterSpec(context, true) },
            addJvmOverloads,
            className = context.resolver.resolveOperation(operation.name)
        )
        .maybeAddJsExport(context)
        .apply {
          if (namedTypes.isNotEmpty() && generateInputBuilders) {
            addType(namedTypes.builderTypeSpec(context, ClassName(packageName, simpleName)))
          }
        }
        .addFunction(operationIdFunSpec())
        .addFunction(queryDocumentFunSpec(generateQueryDocument))
        .addFunction(nameFunSpec())
        .addFunction(serializeVariablesFunSpec())
        .addFunction(adapterFunSpec(context, operation.dataProperty))
        .addFunction(rootFieldFunSpec())
        .addTypes(dataTypeSpecs())
        .addType(companionTypeSpec())
        .build()
        .maybeAddFilterNotNull(generateFilterNotNull)
  }

  private fun serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
      adapterClassName = context.resolver.resolveOperationVariablesAdapter(operation.name),
      emptyMessage = "This operation doesn't have any variable"
  )

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map {
      it.build()
    }
  }

  private fun superInterfaceType(): TypeName {
    return when (operation.operationType) {
      is IrOperationType.Query -> KotlinSymbols.Query
      is IrOperationType.Mutation -> KotlinSymbols.Mutation
      is IrOperationType.Subscription -> KotlinSymbols.Subscription
    }.parameterizedBy(
        context.resolver.resolveModel(operation.dataModelGroup.baseModelId)
    )
  }

  private fun operationIdFunSpec() = FunSpec.builder(Identifier.id)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.String)
      .addStatement("return ${Identifier.OPERATION_ID}")
      .build()

  private fun queryDocumentFunSpec(generateQueryDocument: Boolean) = FunSpec.builder(Identifier.document)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.String)
      .apply {
        if (generateQueryDocument) {
          addStatement("return ${Identifier.OPERATION_DOCUMENT}")
        } else {
          addStatement("error(\"The query document was removed from this operation. Use generateQueryDocument.set(true) if you need it\")")
        }
      }
      .build()

  private fun nameFunSpec() = FunSpec.builder(Identifier.name)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.String)
      .addStatement("return OPERATION_NAME")
      .build()

  private fun companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(PropertySpec.builder(Identifier.OPERATION_ID, KotlinSymbols.String)
            .addModifiers(KModifier.CONST)
            .initializer("%S", operationId)
            .build()
        )
        .applyIf(generateQueryDocument) {
          addProperty(PropertySpec.builder(Identifier.OPERATION_DOCUMENT, KotlinSymbols.String)
              .getter(FunSpec.getterBuilder()
                  .addStatement("return %S", QueryDocumentMinifier.minify(operation.sourceWithFragments))
                  .build()
              )
              .addKdoc("%L", """
                The minimized GraphQL document being sent to the server to save a few bytes.
                The un-minimized version is:


                """.trimIndent() + operation.sourceWithFragments.asKotlinCodeBlock()
              )
              .build()
          )
        }
        .applyIf(generateDataBuilders) {
          addFunction(
              dataBuilderCtor(
                  context = context,
                  modelId = operation.dataModelGroup.baseModelId,
                  selectionsClassName = context.resolver.resolveOperationSelections(operation.name),
                  typename = operation.operationType.typeName,
                  builderFactoryParameterRequired = false
              )
          )
        }
        .addProperty(PropertySpec.builder(Identifier.OPERATION_NAME, KotlinSymbols.String)
            .addModifiers(KModifier.CONST)
            .initializer("%S", operation.name)
            .build()
        )
        .build()
  }


  /**
   * Things like `[${'$'}oo]` do not compile.
   *
   * See https://youtrack.jetbrains.com/issue/KT-43906
   */
  private fun String.asKotlinCodeBlock(): String {
    return "```\n$this\n```\n"
  }

  private fun rootFieldFunSpec(): FunSpec {
    return rootFieldFunSpec(
        context,
        operation.typeCondition,
        context.resolver.resolveOperationSelections(operation.name)
    )
  }
}