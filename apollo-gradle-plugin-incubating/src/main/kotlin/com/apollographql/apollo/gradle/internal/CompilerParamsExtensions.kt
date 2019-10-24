package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.model.ObjectFactory

fun CompilerParams.withFallback(other: CompilerParams, factory: ObjectFactory): CompilerParams {
  val merge = DefaultCompilerParams(factory)
  merge.generateKotlinModels.set(this.generateKotlinModels.orElse(other.generateKotlinModels))
  merge.generateTransformedQueries.set(this.generateTransformedQueries.orElse(other.generateTransformedQueries))
  merge.customTypeMapping.set(this.customTypeMapping.orElse(other.customTypeMapping))
  merge.suppressRawTypesWarning.set(this.suppressRawTypesWarning.orElse(other.suppressRawTypesWarning))
  merge.useSemanticNaming.set(this.useSemanticNaming.orElse(other.useSemanticNaming))
  merge.nullableValueType.set(this.nullableValueType.orElse(other.nullableValueType))
  merge.generateModelBuilder.set(this.generateModelBuilder.orElse(other.generateModelBuilder))
  merge.useJavaBeansSemanticNaming.set(this.useJavaBeansSemanticNaming.orElse(other.useJavaBeansSemanticNaming))
  merge.generateVisitorForPolymorphicDatatypes.set(this.generateVisitorForPolymorphicDatatypes.orElse(other.generateVisitorForPolymorphicDatatypes))
  return merge
}

