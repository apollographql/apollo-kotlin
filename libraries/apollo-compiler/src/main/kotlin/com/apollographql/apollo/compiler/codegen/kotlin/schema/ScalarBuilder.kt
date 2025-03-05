package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.BuiltInType
import com.apollographql.apollo.compiler.ir.IrScalar
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class ScalarBuilder(
    private val context: KotlinSchemaContext,
    private val scalar: IrScalar,
    private val inlineClassName: ClassName?
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = prefixBuiltinScalarNames(layout.schemaTypeName(scalar.name))

  private fun prefixBuiltinScalarNames(name: String): String {
    // Kotlin Multiplatform won't build with class names that clash with Kotlin types (String, Int, etc.).
    // For consistency, do this for all built-in scalars, including ID.
    if (name in arrayOf("String", "Boolean", "Int", "Float", "ID")) {
      return "GraphQL$name"
    }
    return name
  }

  override fun prepare() {
    context.resolver.registerSchemaType(scalar.name, ClassName(packageName, simpleName))

    when {
      scalar.mapTo != null -> {
        context.resolver.registerScalarTarget(scalar.name, scalar.mapTo.name)
        if (scalar.mapTo.adapter != null) {
          context.resolver.registerScalarAdapter(scalar.name, scalar.mapTo.adapter)
        }
        if (scalar.mapTo.inlineProperty != null) {
          context.resolver.registerScalarInlineProperty(scalar.name, scalar.mapTo.inlineProperty)
        }
        context.resolver.registerScalarIsUserDefined(scalar.name)
      }
      scalar.mapToBuiltIn != null -> {
        if (inlineClassName != null) {
          context.resolver.registerScalarTarget(scalar.name, inlineClassName.canonicalName)
        } else {
          context.resolver.registerScalarTarget(
              scalar.name,
              scalar.mapToBuiltIn.builtIn.asTargetClassName().canonicalName
          )
        }
        val adapter =  when (scalar.mapToBuiltIn.builtIn) {
          BuiltInType.String -> KotlinSymbols.StringAdapter
          BuiltInType.Boolean -> KotlinSymbols.BooleanAdapter
          BuiltInType.Int -> KotlinSymbols.IntAdapter
          BuiltInType.Long -> KotlinSymbols.LongAdapter
          BuiltInType.Float -> KotlinSymbols.FloatAdapter
          BuiltInType.Double -> KotlinSymbols.DoubleAdapter
        }.canonicalName
        context.resolver.registerScalarAdapter(scalar.name, adapter)
        context.resolver.registerScalarIsUserDefined(scalar.name)
      }
      else -> {
        val target = when (scalar.name) {
          "String", "ID" -> KotlinSymbols.String
          "Int" -> KotlinSymbols.Int
          "Boolean" -> KotlinSymbols.Boolean
          "Float" -> KotlinSymbols.Double
          else -> KotlinSymbols.Any
        }.canonicalName
        context.resolver.registerScalarTarget(scalar.name, target)

        val adapter = when (scalar.name) {
          "String", "ID" -> KotlinSymbols.StringAdapter
          "Int" -> KotlinSymbols.IntAdapter
          "Boolean" -> KotlinSymbols.BooleanAdapter
          "Float" -> KotlinSymbols.DoubleAdapter
          else -> KotlinSymbols.AnyAdapter
        }.canonicalName
        context.resolver.registerScalarAdapter(scalar.name, adapter)
      }
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(scalar.typeSpec())
    )
  }

  private fun IrScalar.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrScalar.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver.resolveScalarTarget(name)))
        .build()
  }
}

internal fun BuiltInType.asTargetClassName(): ClassName {
  return when (this) {
    BuiltInType.String -> KotlinSymbols.String
    BuiltInType.Boolean -> KotlinSymbols.Boolean
    BuiltInType.Int -> KotlinSymbols.Int
    BuiltInType.Long -> KotlinSymbols.Long
    BuiltInType.Float -> KotlinSymbols.Float
    BuiltInType.Double -> KotlinSymbols.Double
  }
}

internal fun BuiltInType.asAdapterClassName(): ClassName {
  return when (this) {
    BuiltInType.String -> KotlinSymbols.String
    BuiltInType.Boolean -> KotlinSymbols.Boolean
    BuiltInType.Int -> KotlinSymbols.Int
    BuiltInType.Long -> KotlinSymbols.Long
    BuiltInType.Float -> KotlinSymbols.Float
    BuiltInType.Double -> KotlinSymbols.Double
  }
}