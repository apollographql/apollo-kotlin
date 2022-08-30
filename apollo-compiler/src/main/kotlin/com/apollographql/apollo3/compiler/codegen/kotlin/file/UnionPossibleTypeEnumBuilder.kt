package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName

internal class UnionPossibleTypeEnumBuilder(
    private val context: KotlinContext,
    private val union: IrUnion
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  private val simpleName = possibleTypesEnumTypeName(union.name)
  private val compiledTypeName = "${layout.compiledTypeName(union.name)}${Identifier.PossibleTypes}"


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
                union.name,
                compiledTypeName,
                union.members
            )
        ),
    )
  }
}
