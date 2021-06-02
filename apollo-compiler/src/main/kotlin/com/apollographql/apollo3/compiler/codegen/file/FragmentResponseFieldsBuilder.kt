package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.ir.IrNamedFragment
import com.apollographql.apollo3.compiler.codegen.selections.CompiledSelectionsBuilder
import com.squareup.kotlinpoet.MemberName

class FragmentResponseFieldsBuilder(
    val context: CgContext,
    val fragment: IrNamedFragment,
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>
) : CgFileBuilder {
  private val packageName = context.layout.fragmentResponseFieldsPackageName(fragment.filePath)
  private val simpleName = context.layout.fragmentSelectionsName(fragment.name)

  override fun prepare() {
    context.resolver.registerFragmentSelections(
        fragment.name,
        MemberName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        propertySpecs = CompiledSelectionsBuilder(
            context = context,
            allFragmentDefinitions = allFragmentDefinitions,
            schema = schema
        ).build(fragment.selections, simpleName, fragment.typeCondition)
    )
  }
}