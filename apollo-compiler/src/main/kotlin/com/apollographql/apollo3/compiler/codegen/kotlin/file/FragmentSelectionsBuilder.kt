package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.apollographql.apollo3.compiler.codegen.kotlin.selections.CompiledSelectionsBuilder
import com.squareup.kotlinpoet.ClassName

class FragmentSelectionsBuilder(
    val context: KotlinContext,
    val fragment: IrNamedFragment,
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
) : CgOutputFileBuilder {
  private val packageName = context.layout.fragmentResponseFieldsPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentSelectionsName(fragment.name)

  override fun prepare() {
    context.resolver.registerFragmentSelections(
        fragment.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(
            CompiledSelectionsBuilder(
                context = context,
                allFragmentDefinitions = allFragmentDefinitions,
                schema = schema
            ).build(fragment.selections, simpleName, fragment.typeCondition)
        )
    )
  }
}