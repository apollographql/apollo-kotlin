package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.ast.builder.ast
import com.apollographql.apollo.compiler.formatPackageName
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.kotlinpoet.asClassName
import com.squareup.moshi.Moshi
import java.io.File

class GraphQLKompiler(
    private val irFile: File,
    private val customTypeMap: Map<String, String>,
    private val outputPackageName: String?,
    private val useSemanticNaming: Boolean
) {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(CodeGenerationIR::class.java)

  fun write(outputDir: File) {
    val ir = irAdapter.fromJson(irFile.readText())!!
    val irPackageName = outputPackageName ?: irFile.absolutePath.formatPackageName()
    val fragmentsPackage = if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
    val typesPackageName = if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
    val customTypeMap = customTypeMap.supportedCustomTypes(ir.typesUsed)
    val schema = ir.ast(
        customTypeMap = customTypeMap,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        useSemanticNaming = useSemanticNaming
    )

    if (irPackageName.isNotEmpty()) {
      File(outputDir, irPackageName.replace('.', File.separatorChar)).deleteRecursively()
    }

    val schemaCodegen = SchemaCodegen(
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        outputPackageName = outputPackageName
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
