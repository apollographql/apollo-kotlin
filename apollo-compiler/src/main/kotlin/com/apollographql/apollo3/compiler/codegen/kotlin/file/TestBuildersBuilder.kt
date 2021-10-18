package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.test.TestBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.apollographql.apollo3.compiler.ir.IrOperation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

class TestBuildersBuilder(
    val context: KotlinContext,
    val dataModelGroup: IrModelGroup,
    val operation: IrOperation,
    val flatten: Boolean,
) : CgFileBuilder {
  private val packageName = context.layout.operationTestBuildersPackageName(operation.filePath)
  private val simpleName = context.layout.operationTestBuildersWrapperName(operation)

  private val testBuildersBuilder = dataModelGroup.maybeFlatten(flatten).map {
    TestBuilderBuilder(
        context = context,
        modelGroup = it,
        path = listOf(packageName, simpleName),
    )
  }

  override fun prepare() {
    testBuildersBuilder.forEach { it.prepare() }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addTypes(
            testBuildersBuilder.flatMap { it.build() }
        )
        .addFunction(
            dataExtension()
        )
        .build()
  }

  private fun dataExtension(): FunSpec {
    return FunSpec.builder(Identifier.Data)
        .receiver(context.resolver.resolveOperation(operation.name).nestedClass(Identifier.Companion))
        .build()
  }
}