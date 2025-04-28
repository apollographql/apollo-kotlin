package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.ast.QueryDocumentMinifier
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.OPERATION_DOCUMENT
import com.apollographql.apollo.compiler.codegen.Identifier.OPERATION_ID
import com.apollographql.apollo.compiler.codegen.Identifier.OPERATION_NAME
import com.apollographql.apollo.compiler.codegen.Identifier.document
import com.apollographql.apollo.compiler.codegen.Identifier.id
import com.apollographql.apollo.compiler.codegen.Identifier.name
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.BuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.helpers.makeClassFromParameters
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.apollographql.apollo.compiler.codegen.maybeFlatten
import com.apollographql.apollo.compiler.codegen.operationName
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrOperationDefinition
import com.apollographql.apollo.compiler.ir.IrOperationType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class OperationBuilder(
    private val context: JavaOperationsContext,
    private val operationId: String,
    private val generateQueryDocument: Boolean,
    private val operation: IrOperationDefinition,
    flatten: Boolean,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.executableDocumentPackageName(operation.normalizedFilePath)
  private val simpleName = layout.operationName(operation)
  private fun definitionTypeName(): ParameterizedTypeName? {
    return ParameterizedTypeName.get(JavaClassNames.ExecutableDefinition, context.resolver.resolveModel(operation.dataModelGroup.baseModelId))
  }


  private val dataSuperClassName = when (operation.operationType) {
    is IrOperationType.Query -> JavaClassNames.QueryData
    is IrOperationType.Mutation -> JavaClassNames.MutationData
    is IrOperationType.Subscription -> JavaClassNames.SubscriptionData
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
        path = listOf(packageName, simpleName),
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
        .makeClassFromParameters(
            context.generateMethods,
            operation.variables.map { it.toNamedType().toParameterSpec(context) },
            className = context.resolver.resolveOperation(operation.name)
        )
        .addBuilder(context)
        .addType(executableDefinitionTypeSpec())
        .addField(executableDefinitionFieldSpec())
        .addMethod(operationIdMethodSpec())
        .addMethod(queryDocumentMethodSpec(generateQueryDocument))
        .addMethod(nameMethodSpec())
        .addMethod(serializeVariablesMethodSpec())
        .addMethod(adapterMethodSpec(context.resolver, operation.dataProperty))
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


                """.trimIndent() + operation.sourceWithFragments.asJavadocCodeBlock()
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

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map {
      it.build()
    }
  }

  private fun executableDefinitionFieldSpec(): FieldSpec {
    return FieldSpec.builder(
        definitionTypeName(),
        "definition"
    ).addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .initializer("new Definition()")
        .build()
  }

  private fun executableDefinitionTypeSpec(): TypeSpec {
    val adaptedTypeName = context.resolver.resolveModel(operation.dataModelGroup.baseModelId)
    return TypeSpec.classBuilder("Definition")
        .addModifiers(Modifier.STATIC, Modifier.PRIVATE)
        .addSuperinterface(definitionTypeName())
        .addMethod(
            MethodSpec.methodBuilder("getADAPTER")
                .addAnnotation(JavaClassNames.Override)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(JavaClassNames.Adapter, adaptedTypeName))
                .addCode(
                    "return $L;\n",
                    context.resolver.adapterInitializer(operation.dataProperty.info.type, operation.dataProperty.requiresBuffering)
                )
                .build()

        )
        .addMethod(
            MethodSpec.methodBuilder("getROOT_FIELD")
                .addAnnotation(JavaClassNames.Override)
                .addModifiers(Modifier.PUBLIC)
                .returns(JavaClassNames.CompiledField)
                .addCode(
                    CodeBlock.builder()
                        .add("return new $T(\n", JavaClassNames.CompiledFieldBuilder)
                        .indent()
                        .add("$S,\n", Identifier.data)
                        .add("$L\n", context.resolver.resolveCompiledType(operation.typeCondition))
                        .unindent()
                        .add(")\n")
                        .add(".${Identifier.selections}($T.${Identifier.root})\n", context.resolver.resolveOperationSelections(operation.name))
                        .add(".build();\n")
                        .build()
                )
                .build()

        )
        .build()
  }


  private fun superInterfaceType(): TypeName {
    return when (operation.operationType) {
      is IrOperationType.Query -> JavaClassNames.Query
      is IrOperationType.Mutation -> JavaClassNames.Mutation
      is IrOperationType.Subscription -> JavaClassNames.Subscription
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

  private fun String.asJavadocCodeBlock(): String {
    /**
     * TODO: proper Javadoc formatting
     */
    return this
  }

  private fun rootFieldMethodSpec(): MethodSpec {
    return rootFieldMethodSpec(
        context,
        operation.typeCondition,
        context.resolver.resolveOperationSelections(operation.name)
    )
  }

  private fun TypeSpec.Builder.addBuilder(context: JavaOperationsContext): TypeSpec.Builder {
    addMethod(BuilderBuilder.builderFactoryMethod())

    val operationClassName = context.resolver.resolveOperation(operation.name)

    if (operation.variables.isEmpty()) {
      return addType(
          BuilderBuilder(
              targetObjectClassName = operationClassName,
              fields = emptyList(),
              context = context
          ).build()
      )
    }

    operation.variables
        .map {
          val irType = context.resolver.resolveIrType(it.type)
          FieldSpec.builder(irType.withoutAnnotations(), context.layout.javaPropertyName(it.name))
              .addAnnotations(irType.annotations)
              .build()
        }
        .let {
          BuilderBuilder(
              targetObjectClassName = operationClassName,
              fields = it,
              context = context
          )
        }
        .let { addType(it.build()) }

    return this
  }
}
