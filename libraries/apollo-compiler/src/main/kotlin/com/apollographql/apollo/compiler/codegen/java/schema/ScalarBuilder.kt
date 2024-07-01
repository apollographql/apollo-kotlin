package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrScalar
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class ScalarBuilder(
    private val context: JavaSchemaContext,
    private val scalar: IrScalar,
    private val targetTypeName: String?,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = prefixBuiltinScalarNames(layout.schemaTypeName(scalar.name))

  private fun prefixBuiltinScalarNames(name: String): String {
    // Kotlin Multiplatform won't build with class names that clash with Kotlin types (String, Int, etc.).
    // For consistency, do this for all built-in scalars, including ID, and in the Java codegen as well.
    if (name in arrayOf("String", "Boolean", "Int", "Float", "ID")) {
      return "GraphQL$name"
    }
    return name
  }

  override fun prepare() {
    context.resolver.registerSchemaType(scalar.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = scalar.typeSpec()
    )
  }

  private fun IrScalar.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addField(typeFieldSpec(targetTypeName))
        .build()
  }
}
