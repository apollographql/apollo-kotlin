package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType

data class CodeGenerationContext(
    var reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val customTypeMap: Map<String, String>,
    val nullableValueType: NullableValueType,
    val ir: CodeGenerationIR,
    val useSemanticNaming: Boolean,
    val generateModelBuilder: Boolean,
    val useJavaBeansSemanticNaming: Boolean,
    val suppressRawTypesWarning: Boolean,
    val generateVisitorForPolymorphicDatatypes: Boolean,
    val layoutArgs: GraphQLCompiler.LayoutArguments
)