package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.ast.builder.ast
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.squareup.kotlinpoet.asClassName
import java.io.File

class GraphQLKompiler(
    private val ir: CodeGenerationIR,
    private val customTypeMap: Map<String, String>,
    private val useSemanticNaming: Boolean,
    private val generateAsInternal: Boolean = false,
    private val operationOutput: OperationOutput,
    private val kotlinMultiPlatformProject: Boolean,
    private val enumAsSealedClassPatternFilters: List<Regex>
) {
  fun write(outputDir: File) {
    val schema = ir.ast(
        customTypeMap = CustomTypes(customTypeMap),
        typesPackageName = ir.typesPackageName,
        fragmentsPackage = ir.fragmentsPackageName,
        useSemanticNaming = useSemanticNaming,
        operationOutput = operationOutput
    )
    val schemaCodegen = SchemaCodegen(
        typesPackageName = ir.typesPackageName,
        fragmentsPackageName = ir.fragmentsPackageName,
        generateAsInternal = generateAsInternal,
        kotlinMultiPlatformProject = kotlinMultiPlatformProject,
        enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters
    )
    schemaCodegen.apply(schema::accept).writeTo(outputDir)
  }
}
