package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.DeprecatedPackageNameProvider
import com.apollographql.apollo.compiler.PackageNameProvider
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
    private val packageNameProvider: PackageNameProvider,
    private val useSemanticNaming: Boolean,
    private val generateAsInternal: Boolean = false
) {
  fun write(outputDir: File) {
    val customTypeMap = customTypeMap.supportedCustomTypes(ir.typesUsed)
    val schema = ir.ast(
        customTypeMap = customTypeMap,
        typesPackageName = packageNameProvider.typesPackageName,
        fragmentsPackage = packageNameProvider.fragmentsPackageName,
        useSemanticNaming = useSemanticNaming
    )

    val irPackageName = (packageNameProvider as? DeprecatedPackageNameProvider)?.schemaPackageName ?: ""
    if (irPackageName.isNotEmpty()) {
      File(outputDir, irPackageName.replace('.', File.separatorChar)).deleteRecursively()
    }

    val schemaCodegen = SchemaCodegen(
        packageNameProvider = packageNameProvider,
        generateAsInternal = generateAsInternal
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
