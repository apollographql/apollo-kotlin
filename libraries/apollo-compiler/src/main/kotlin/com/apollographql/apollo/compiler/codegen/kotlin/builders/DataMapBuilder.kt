package com.apollographql.apollo.compiler.codegen.kotlin.builders

import com.apollographql.apollo.compiler.codegen.Identifier.__fields
import com.apollographql.apollo.compiler.codegen.dataMapName
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinDataBuilderContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.builderPackageName
import com.apollographql.apollo.compiler.ir.IrDataBuilder
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec


internal class DataMapBuilder(
    private val context: KotlinDataBuilderContext,
    private val dataBuilder: IrDataBuilder,
    private val withFields: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = dataMapName(dataBuilder.name, withFields && dataBuilder.isAbstract, layout)

  override fun prepare() {
    if (!dataBuilder.isAbstract || !withFields) {
      context.resolver.registerMapType(dataBuilder.name, ClassName(packageName, simpleName))
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec()),
    )
  }

  private fun typeSpec(): TypeSpec {
    return if (withFields) {
      TypeSpec
          .classBuilder(simpleName)
          .primaryConstructor(
              FunSpec.constructorBuilder()
                  .addParameter(
                      ParameterSpec.builder(
                          __fields,
                          KotlinSymbols.MapOfStringToNullableAny
                      ).build()
                  )
                  .build()
          )
          .superclass(KotlinSymbols.DataMap)
          .addSuperclassConstructorParameter(__fields)
          .addSuperinterfaces(dataBuilder.superTypes.map {
            ClassName(packageName, dataMapName(it, false, layout))
          })
          .apply {
            if (dataBuilder.isAbstract) {
              addSuperinterface(
                  ClassName(packageName, dataMapName(dataBuilder.name, false, layout))
              )
            }
          }
          .build()
    } else {
      TypeSpec
          .interfaceBuilder(simpleName)
          .addSuperinterfaces(dataBuilder.superTypes.map {
            ClassName(packageName, dataMapName(it, false, layout))
          })
          .build()
    }
  }
}
