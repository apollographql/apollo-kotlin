package com.apollographql.apollo.compiler.codegen.java.operations.util

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo.compiler.codegen.Identifier.reader
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaContext
import com.apollographql.apollo.compiler.codegen.java.helpers.readFromResponseCodeBlock
import com.apollographql.apollo.compiler.codegen.java.helpers.responseNamesFieldSpec
import com.apollographql.apollo.compiler.codegen.java.helpers.toClassName
import com.apollographql.apollo.compiler.codegen.java.helpers.writeToResponseCodeBlock
import com.apollographql.apollo.compiler.ir.IrModel
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class MonomorphicFieldResponseAdapterBuilder(
    val context: JavaContext,
    val model: IrModel,
    val path: List<String>,
    val public: Boolean,
) : ResponseAdapterBuilder {

  private val adapterName = model.modelName
  private val adaptedClassName by lazy {
    context.resolver.resolveModel(model.id)
  }

  private val nestedAdapterBuilders = model.modelGroups.map {
    ResponseAdapterBuilder.create(
        context,
        it,
        path + adapterName,
        false
    )
  }

  override fun prepare() {
    context.resolver.registerModelAdapter(
        model.id,
        (path + adapterName).toClassName()
    )
    nestedAdapterBuilders.map { it.prepare() }
  }

  override fun build(): List<TypeSpec> {
    return listOf(typeSpec())
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.enumBuilder(adapterName)
        .addSuperinterface(
            ParameterizedTypeName.get(JavaClassNames.Adapter, context.resolver.resolveModel(model.id))
        )
        .apply {
          addModifiers(if (public) Modifier.PUBLIC else Modifier.PRIVATE)
        }
        .apply {
          val responseNames = responseNamesFieldSpec(model)
          if (responseNames != null) {
            addField(responseNames)
          }
        }
        .addEnumConstant("INSTANCE")
        .addMethod(readFromResponseMethodSpec())
        .addMethod(writeToResponseMethodSpec())
        .addTypes(nestedAdapterBuilders.flatMap { it.build() })
        .build()
  }

  private fun readFromResponseMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(fromJson)
        .addModifiers(Modifier.PUBLIC)
        .returns(adaptedClassName)
        .addException(JavaClassNames.IOException)
        .addParameter(JavaClassNames.JsonReader, reader)
        .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addAnnotation(JavaClassNames.Override)
        .addCode(readFromResponseCodeBlock(model, context, false))
        .build()
  }

  private fun writeToResponseMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(toJson)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(JavaClassNames.Override)
        .addException(JavaClassNames.IOException)
        .addParameter(JavaClassNames.JsonWriter, Identifier.writer)
        .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addParameter(adaptedClassName, Identifier.value)
        .addCode(writeToResponseCodeBlock(model, context))
        .build()
  }
}
