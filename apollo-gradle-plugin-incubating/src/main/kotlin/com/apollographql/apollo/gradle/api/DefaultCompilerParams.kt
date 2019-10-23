package com.apollographql.apollo.gradle.api

class DefaultCompilerParams(
    override var generateKotlinModels: Boolean? = null,
    override var generateTransformedQueries: Boolean? = null,
    override var customTypeMapping: Map<String, String>? = null,
    override var suppressRawTypesWarning: Boolean? = null,
    override var useSemanticNaming: Boolean? = null,

    override var generateModelBuilder: Boolean? = null,
    override var useJavaBeansSemanticNaming: Boolean? = null,
    override var generateVisitorForPolymorphicDatatypes: Boolean? = null,
    override var nullableValueType: String? = null
) : CompilerParams