//package com.apollographql.apollo.gradle.internal
//
//import com.apollographql.apollo.compiler.NullableValueType
//import com.apollographql.apollo.gradle.api.CompilerParams
//
//class ResolvedCompilerParams(
//    val generateKotlinModels: Boolean,
//    val generateTransformedQueries: Boolean,
//    val customTypeMapping: Map<String, String>,
//    val suppressRawTypesWarning: Boolean,
//    val useSemanticNaming: Boolean,
//
//    val nullableValueType: String,
//    val generateModelBuilder: Boolean,
//    val useJavaBeansSemanticNaming: Boolean,
//    val generateVisitorForPolymorphicDatatypes: Boolean
//) {
//  companion object {
//    fun from(apolloExtension: DefaultApolloExtension, serviceParams: CompilerParams?): ResolvedCompilerParams {
//
//      val params = ResolvedCompilerParams(
//          generateKotlinModels = serviceParams?.generateKotlinModels ?: apolloExtension.generateKotlinModels ?: false,
//          generateTransformedQueries = serviceParams?.generateTransformedQueries ?: apolloExtension.generateTransformedQueries ?: false,
//          customTypeMapping = serviceParams?.customTypeMapping ?: apolloExtension.customTypeMapping ?: emptyMap(),
//          suppressRawTypesWarning = serviceParams?.suppressRawTypesWarning ?: apolloExtension.suppressRawTypesWarning ?: false,
//          useSemanticNaming = serviceParams?.useSemanticNaming ?: apolloExtension.useSemanticNaming ?: true,
//
//          nullableValueType = serviceParams?.nullableValueType ?: apolloExtension.nullableValueType ?: NullableValueType.ANNOTATED.value,
//          generateModelBuilder = serviceParams?.generateModelBuilder ?: apolloExtension.generateModelBuilder ?: false,
//          useJavaBeansSemanticNaming = serviceParams?.useJavaBeansSemanticNaming ?: apolloExtension.useJavaBeansSemanticNaming ?: false,
//          generateVisitorForPolymorphicDatatypes = serviceParams?.generateVisitorForPolymorphicDatatypes
//              ?: apolloExtension.generateVisitorForPolymorphicDatatypes ?: false
//      )
//
//      if (params.generateKotlinModels && params.generateModelBuilder) {
//        throw IllegalArgumentException("""
//        Using `generateModelBuilder = true` does not make sense with `generateKotlinModels = true`. You can use .copy() as models are data classes.
//      """.trimIndent())
//      }
//
//      if (params.generateKotlinModels && params.useJavaBeansSemanticNaming) {
//        throw IllegalArgumentException("""
//        Using `useJavaBeansSemanticNaming = true` does not make sense with `generateKotlinModels = true`
//      """.trimIndent())
//      }
//
//      if (params.generateKotlinModels && serviceParams?.nullableValueType ?: apolloExtension.nullableValueType != null) {
//        throw IllegalArgumentException("""
//        Using `nullableValueType` does not make sense with `generateKotlinModels = true`
//      """.trimIndent())
//      }
//
//      if (apolloExtension.schemaFilePath != null) {
//        throw IllegalArgumentException("""
//        apollo.schemaFilePath is not supported anymore as it doesn't work for multiple services.
//
//      """.trimIndent() + ApolloPlugin.useService(apolloExtension.schemaFilePath, apolloExtension.outputPackageName))
//      }
//
//      if (apolloExtension.outputPackageName != null) {
//        throw IllegalArgumentException("""
//        apollo.outputPackageName is not supported anymore as it doesn't work for multiple services and also flattens the packages.
//
//      """.trimIndent() + ApolloPlugin.useService(apolloExtension.schemaFilePath, apolloExtension.outputPackageName))
//      }
//
//      return params
//    }
//  }
//}
