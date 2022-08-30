package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.squareup.kotlinpoet.ClassName

internal class InterfacePossibleTypeEnumBuilder(
    private val context: KotlinContext,
    private val iface: IrInterface
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  private val simpleName = possibleTypesEnumTypeName(iface.name)
  private val compiledTypeName = "${layout.compiledTypeName(iface.name)}${Identifier.PossibleTypes}"

  override fun prepare() {
    context.resolver.registerMapType(simpleName, ClassName(packageName, compiledTypeName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = compiledTypeName,
        typeSpecs = listOf(
            possibleTypesEnumTypeSpec(
                layout,
                iface.name,
                compiledTypeName,
                iface.possibleTypes
            )
        ),
    )
  }
}
