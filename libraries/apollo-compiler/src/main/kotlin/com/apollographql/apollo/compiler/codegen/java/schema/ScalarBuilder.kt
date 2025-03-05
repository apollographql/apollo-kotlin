package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.BuiltInType
import com.apollographql.apollo.compiler.ir.IrScalar
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal val builtinScalars = arrayOf("String", "Boolean", "Int", "Float", "ID")

internal class ScalarBuilder(
    private val context: JavaSchemaContext,
    private val scalar: IrScalar,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = prefixBuiltinScalarNames(layout.schemaTypeName(scalar.name))

  private fun prefixBuiltinScalarNames(name: String): String {
    // Kotlin Multiplatform won't build with class names that clash with Kotlin types (String, Int, etc.).
    // For consistency, do this for all built-in scalars, including ID, and in the Java codegen as well.
    if (name in builtinScalars) {
      return "GraphQL$name"
    }
    return name
  }

  override fun prepare() {
    context.resolver.registerSchemaType(scalar.name, ClassName.get(packageName, simpleName))

    when {
      scalar.mapTo != null -> {
        context.resolver.registerScalarTarget(scalar.name, scalar.mapTo.name)
        if (scalar.mapTo.adapter != null) {
          context.resolver.registerScalarAdapter(scalar.name, scalar.mapTo.adapter)
        }
        context.resolver.registerScalarIsUserDefined(scalar.name)
      }
      scalar.mapToBuiltIn != null -> {
        val target = when (scalar.mapToBuiltIn.builtIn) {
          BuiltInType.String -> JavaClassNames.String
          BuiltInType.Boolean -> JavaClassNames.Boolean
          BuiltInType.Int -> JavaClassNames.Integer
          BuiltInType.Long -> JavaClassNames.Long
          BuiltInType.Float -> JavaClassNames.Float
          BuiltInType.Double -> JavaClassNames.Double
        }.canonicalName()
        context.resolver.registerScalarTarget(scalar.name, target)

        val adapter = javaScalarAdapterInitializer(scalar.mapToBuiltIn.builtIn)
        context.resolver.registerScalarAdapter(scalar.name, adapter)
        context.resolver.registerScalarIsUserDefined(scalar.name)
      }
      else -> {
        val target = when (scalar.name) {
          "String", "ID" -> JavaClassNames.String
          "Int" -> JavaClassNames.Integer
          "Boolean" -> JavaClassNames.Boolean
          "Float" -> JavaClassNames.Double
          else -> JavaClassNames.Object
        }.canonicalName()
        context.resolver.registerScalarTarget(scalar.name, target)

        val adapter = when (scalar.name) {
          "ID" -> javaScalarAdapterInitializer(BuiltInType.String)
          "String" -> javaScalarAdapterInitializer(BuiltInType.String)
          "Int" -> javaScalarAdapterInitializer(BuiltInType.Int)
          "Boolean" -> javaScalarAdapterInitializer(BuiltInType.Boolean)
          "Float" -> javaScalarAdapterInitializer(BuiltInType.Double)
          else -> javaScalarAdapterInitializer("Any")
        }
        context.resolver.registerScalarAdapter(scalar.name, adapter)
      }
    }
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
        .addField(typeFieldSpec(context.resolver.resolveScalarTarget(name)))
        .build()
  }
}

internal fun javaScalarAdapterInitializer(builtInType: BuiltInType): String {
  return javaScalarAdapterInitializer(builtInType.name)
}
internal fun javaScalarAdapterInitializer(builtInType: String): String {
  return "${JavaClassNames.Adapters.canonicalName()}.${builtInType}Adapter"
}