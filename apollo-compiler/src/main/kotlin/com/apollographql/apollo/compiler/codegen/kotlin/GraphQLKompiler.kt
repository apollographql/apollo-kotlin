package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.ast.builder.ast
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.kotlinpoet.asClassName
import java.io.File

class GraphQLKompiler(
    private val ir: CodeGenerationIR,
    private val customTypeMap: Map<String, String>,
    private val layoutArgs: GraphQLCompiler.LayoutArguments,
    private val useSemanticNaming: Boolean
) {
  fun write(outputDir: File) {
    val customTypeMap = customTypeMap.supportedCustomTypes(ir.typesUsed)
    val schema = ir.ast(
        customTypeMap = customTypeMap,
        typesPackageName = layoutArgs.typesPackageName(),
        fragmentsPackage = layoutArgs.fragmentsPackageName(),
        useSemanticNaming = useSemanticNaming
    )

    val irPackageName = layoutArgs.irPackageName
    if (irPackageName.isNotEmpty()) {
      File(outputDir, irPackageName.replace('.', File.separatorChar)).deleteRecursively()
    }

    val schemaCodegen = SchemaCodegen(
        layoutArgs = layoutArgs
    )
    schemaCodegen.apply(schema::accept).writeTo(outputDir)
  }

  private fun Map<String, String>.supportedCustomTypes(typeDeclarations: List<TypeDeclaration>): CustomTypes {
    val idScalarTypeMap = ScalarType.ID.name to (this[ScalarType.ID.name] ?: String::class.asClassName().toString())
    return CustomTypes(
        typeDeclarations
            .filter { it.kind == TypeDeclaration.KIND_SCALAR_TYPE }
            .associate { it.name to (this[it.name] ?: Any::class.asClassName().canonicalName) }
            .plus(idScalarTypeMap)
    )
  }
}
