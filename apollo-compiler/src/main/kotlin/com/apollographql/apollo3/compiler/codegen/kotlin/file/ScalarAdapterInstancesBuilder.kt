package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.NoArgConstructorAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class ScalarAdapterInstancesBuilder(
    context: KotlinContext,
    private val scalarMapping: Map<String, ScalarInfo>,
) : CgOutputFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typeAdapterPackageName()
  private val simpleName = layout.compiledTypeName(Identifier.ScalarAdapterInstances)

  override fun prepare() {}

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(scalarAdapterInstancesTypeSpec())
    )

  }

  private fun scalarAdapterInstancesTypeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(Identifier.ScalarAdapterInstances)
        .apply {
          for ((name, scalarInfo) in scalarMapping) {
            if (scalarInfo.adapterInitializer is NoArgConstructorAdapterInitializer) {
              addProperty(scalarPropertySpec(name, scalarInfo.targetName, scalarInfo.adapterInitializer))
            }
          }
        }
        .build()

  }

  private fun scalarPropertySpec(name: String, targetName: String, adapterInitializer: NoArgConstructorAdapterInitializer): PropertySpec {
    return PropertySpec.builder(
        name = layout.scalarAdapterName(name),
        type = KotlinSymbols.Adapter.parameterizedBy(ClassName.bestGuess(targetName))
    )
        .initializer("${adapterInitializer.qualifiedName}()")
        .build()
  }
}
