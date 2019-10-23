package com.apollographql.apollo.gradle.api

interface CompilerParams {
  var generateKotlinModels: Boolean?
  var generateTransformedQueries: Boolean?
  var customTypeMapping: Map<String, String>?
  var suppressRawTypesWarning: Boolean?
  var useSemanticNaming: Boolean?

  var nullableValueType: String?
  var generateModelBuilder: Boolean?
  var useJavaBeansSemanticNaming: Boolean?
  var generateVisitorForPolymorphicDatatypes: Boolean?
}
