package com.apollographql.apollo.compiler.codegen.kotlin.builders

import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinDataBuilderContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.builderResolverPackageName
import com.apollographql.apollo.compiler.ir.IrNamedType
import com.apollographql.apollo.compiler.ir.IrScalarType
import com.apollographql.apollo.compiler.ir.IrType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.withIndent

internal class AdaptBuilder(
    private val context: KotlinDataBuilderContext,
    private val scalars: List<String>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.builderResolverPackageName()
  private val simpleName = "adapt"

  override fun prepare() {}
  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        funSpecs = listOf(funSpec()),
    )
  }
  private fun funSpec(): FunSpec {
    return FunSpec.builder("adaptToJson")
        .addParameter("value", KotlinSymbols.Any)
        .returns(KotlinSymbols.Any)
        .receiver(KotlinSymbols.FakeResolverContext)
        .addCode(
            buildCodeBlock {
              add("val writer = %T()\n", KotlinSymbols.MapJsonWriter)
              add("when(mergedField.type.rawType().name){\n")
              withIndent {
                scalars.forEach {
                  if (context.resolver.resolve(ResolverKey(ResolverKeyKind.SchemaType, it)) != null) {
                    val initializer = context.resolver.adapterInitializer(IrScalarType(it), false, jsExport = false)
                    add("%S -> %L.toJson(writer, customScalarAdapters, value as %T)\n", it, initializer, context.resolver.resolveScalarTarget(it))
                  }
                }
                add("else -> return value\n")
              }
              add("}\n")
              add("return writer.root()!!\n")
            }
        )
        .build()
  }

}