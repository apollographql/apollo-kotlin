package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.operationoutput.OperationOutput

data class CodeGenerationContext(
    var reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val customTypeMap: Map<String, String>,
    val operationOutput: OperationOutput,
    val nullableValueType: NullableValueType,
    val ir: CodeGenerationIR,
    val useSemanticNaming: Boolean,
    val generateModelBuilder: Boolean,
    val useJavaBeansSemanticNaming: Boolean,
    val suppressRawTypesWarning: Boolean,
    val generateVisitorForPolymorphicDatatypes: Boolean
)
