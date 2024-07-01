package com.apollographql.apollo.compiler.codegen.java.adapters

import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.javaOptionalAdaptersClassName
import com.apollographql.apollo.compiler.codegen.typeAdapterPackageName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class JavaOptionalAdaptersBuilder(private val context: JavaSchemaContext) : JavaClassBuilder {
  private val packageName = context.layout.typeAdapterPackageName()
  private val simpleName = context.layout.javaOptionalAdaptersClassName()

  override fun prepare() {
    context.resolver.registerJavaOptionalAdapters(ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = javaOptionalAdaptersTypeSpec()
    )
  }

  private fun javaOptionalAdaptersTypeSpec(): TypeSpec {
    val optionalAdapter = context.resolver.resolveJavaOptionalAdapter()
    val stringAdapter = ParameterizedTypeName.get(optionalAdapter, JavaClassNames.String)
    val doubleAdapter = ParameterizedTypeName.get(optionalAdapter, JavaClassNames.Double)
    val intAdapter = ParameterizedTypeName.get(optionalAdapter, JavaClassNames.Integer)
    val booleanAdapter = ParameterizedTypeName.get(optionalAdapter, JavaClassNames.Boolean)
    val anyAdapter = ParameterizedTypeName.get(optionalAdapter, JavaClassNames.Object)

    val stringAdapterField = FieldSpec.builder(stringAdapter, "OptionalStringAdapter", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("new $T($T.StringAdapter)", stringAdapter, JavaClassNames.Adapters)
        .build()

    val doubleAdapterField = FieldSpec.builder(doubleAdapter, "OptionalDoubleAdapter", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("new $T($T.DoubleAdapter)", doubleAdapter, JavaClassNames.Adapters)
        .build()

    val intAdapterField = FieldSpec.builder(intAdapter, "OptionalIntAdapter", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("new $T($T.IntAdapter)", intAdapter, JavaClassNames.Adapters)
        .build()

    val booleanAdapterField =
      FieldSpec.builder(booleanAdapter, "OptionalBooleanAdapter", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("new $T($T.BooleanAdapter)", booleanAdapter, JavaClassNames.Adapters)
          .build()

    val anyAdapterField = FieldSpec.builder(anyAdapter, "OptionalAnyAdapter", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("new $T($T.AnyAdapter)", anyAdapter, JavaClassNames.Adapters)
        .build()

    return TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addField(stringAdapterField)
        .addField(doubleAdapterField)
        .addField(intAdapterField)
        .addField(booleanAdapterField)
        .addField(anyAdapterField)
        .build()
  }
}
