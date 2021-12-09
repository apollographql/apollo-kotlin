package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier.OPERATION_DOCUMENT
import com.apollographql.apollo3.compiler.codegen.Identifier.OPERATION_ID
import com.apollographql.apollo3.compiler.codegen.Identifier.OPERATION_NAME
import com.apollographql.apollo3.compiler.codegen.Identifier.document
import com.apollographql.apollo3.compiler.codegen.Identifier.id
import com.apollographql.apollo3.compiler.codegen.Identifier.name
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.helpers.makeDataClassFromParameters
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.java.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.apollographql.apollo3.compiler.ir.IrOperationType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class OperationBuilder(
    private val context: JavaContext,
    private val operationId: String,
    private val generateQueryDocument: Boolean,
    private val operation: IrOperation,
    flatten: Boolean,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.operationPackageName(operation.filePath)
  private val simpleName = layout.operationName(operation)

  private val dataSuperClassName = when (operation.operationType) {
    IrOperationType.Query -> JavaClassNames.QueryData
    IrOperationType.Mutation -> JavaClassNames.MutationData
    IrOperationType.Subscription -> JavaClassNames.SubscriptionData
  }

  private val modelBuilders = operation.dataModelGroup.maybeFlatten(
      flatten = flatten,
      excludeNames = setOf(simpleName)
  ).flatMap {
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
        ClassName.get(packageName, simpleName)
    )
    modelBuilders.forEach { it.prepare() }
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(layout.operationName(operation))
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(operation.description)
        .makeDataClassFromParameters(operation.variables.map { it.toNamedType().toParameterSpec(context) })
        .addMethod(operationIdMethodSpec())
        .addMethod(queryDocumentMethodSpec(generateQueryDocument))
        .addMethod(nameMethodSpec())
        .addMethod(serializeVariablesMethodSpec())
        .addMethod(adapterMethodSpec())
        .addMethod(rootFieldMethodSpec())
        .addTypes(dataTypeSpecs())
        .addField(
            FieldSpec.builder(JavaClassNames.String, OPERATION_ID)
                .addModifiers(Modifier.FINAL)
                .addModifiers(Modifier.STATIC)
                .addModifiers(Modifier.PUBLIC)
                .initializer(S, operationId)
                .build()
        )
        .applyIf(generateQueryDocument) {
          addField(FieldSpec.builder(JavaClassNames.String, OPERATION_DOCUMENT)
              .addModifiers(Modifier.FINAL)
              .addModifiers(Modifier.STATIC)
              .addModifiers(Modifier.PUBLIC)
              .initializer(S, QueryDocumentMinifier.minify(operation.sourceWithFragments))
              .addJavadoc(L, """
                The minimized GraphQL document being sent to the server to save a few bytes.
                The un-minimized version is:


                """.trimIndent() + operation.sourceWithFragments.escapeKdoc()
              )
              .build()
          )
        }
        .addField(FieldSpec
            .builder(JavaClassNames.String, OPERATION_NAME)
            .addModifiers(Modifier.FINAL)
            .addModifiers(Modifier.STATIC)
            .addModifiers(Modifier.PUBLIC)
            .initializer(S, operation.name)
            .build()
        )
        .build()
  }

  private fun serializeVariablesMethodSpec(): MethodSpec = serializeVariablesMethodSpec(
      adapterClassName = context.resolver.resolveOperationVariablesAdapter(operation.name),
      emptyMessage = "This operation doesn't have any variable"
  )

  private fun adapterMethodSpec(): MethodSpec {
    return adapterMethodSpec(
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
      IrOperationType.Query -> JavaClassNames.Query
      IrOperationType.Mutation -> JavaClassNames.Mutation
      IrOperationType.Subscription -> JavaClassNames.Subscription
    }.let {
      ParameterizedTypeName.get(it, context.resolver.resolveModel(operation.dataModelGroup.baseModelId))
    }
  }

  private fun operationIdMethodSpec() = MethodSpec.methodBuilder(id)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(JavaClassNames.String)
      .addStatement("return $OPERATION_ID")
      .build()

  private fun queryDocumentMethodSpec(generateQueryDocument: Boolean) = MethodSpec.methodBuilder(document)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(JavaClassNames.String)
      .apply {
        if (generateQueryDocument) {
          addStatement("return $OPERATION_DOCUMENT")
        } else {
          addStatement("error(\"The query document was removed from this operation. Use generateQueryDocument.set(true) if you need it\")")
        }
      }
      .build()

  private fun nameMethodSpec() = MethodSpec.methodBuilder(name)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(JavaClassNames.String)
      .addStatement("return $OPERATION_NAME")
      .build()

  /**
   * Things like `[${'$'}oo]` do not compile. See https://youtrack.jetbrains.com/issue/KT-43906
   */
  private fun String.escapeKdoc(): String {
    return replace("[", "\\[").replace("]", "\\]")
  }

  private fun rootFieldMethodSpec(): MethodSpec {
    return rootFieldMethodSpec(
        context,
        operation.typeCondition,
        context.resolver.resolveOperationSelections(operation.name)
    )
  }
}

