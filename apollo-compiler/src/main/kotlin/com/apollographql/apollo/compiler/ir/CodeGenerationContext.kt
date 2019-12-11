package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.DeprecatedPackageNameProvider
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.CustomIdGenerator

data class CodeGenerationContext(
    var reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val customTypeMap: Map<String, String>,
    val customIdGenerator: CustomIdGenerator?,
    val nullableValueType: NullableValueType,
    val ir: CodeGenerationIR,
    val useSemanticNaming: Boolean,
    val generateModelBuilder: Boolean,
    val useJavaBeansSemanticNaming: Boolean,
    val suppressRawTypesWarning: Boolean,
    val generateVisitorForPolymorphicDatatypes: Boolean,
    val packageNameProvider: PackageNameProvider
)
