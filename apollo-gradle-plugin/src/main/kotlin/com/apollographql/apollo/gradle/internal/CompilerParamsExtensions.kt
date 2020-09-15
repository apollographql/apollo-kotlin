package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.model.ObjectFactory

fun CompilerParams.withFallback(objects: ObjectFactory, other: CompilerParams): CompilerParams {
  val merge = objects.newInstance(DefaultCompilerParams::class.java)

  if (this.graphqlSourceDirectorySet.srcDirs.isEmpty()) {
    merge.graphqlSourceDirectorySet.source(other.graphqlSourceDirectorySet)
  } else {
    merge.graphqlSourceDirectorySet.source(this.graphqlSourceDirectorySet)
  }
  merge.schemaFile.set(this.schemaFile.orElse(other.schemaFile))

  merge.generateKotlinModels.set(this.generateKotlinModels.orElse(other.generateKotlinModels))
  merge.generateOperationOutput.set(this.generateOperationOutput.orElse(other.generateOperationOutput))
  merge.customTypeMapping.set(this.customTypeMapping.orElse(other.customTypeMapping))
  merge.suppressRawTypesWarning.set(this.suppressRawTypesWarning.orElse(other.suppressRawTypesWarning))
  merge.useSemanticNaming.set(this.useSemanticNaming.orElse(other.useSemanticNaming))
  merge.nullableValueType.set(this.nullableValueType.orElse(other.nullableValueType))
  merge.generateModelBuilder.set(this.generateModelBuilder.orElse(other.generateModelBuilder))
  merge.useJavaBeansSemanticNaming.set(this.useJavaBeansSemanticNaming.orElse(other.useJavaBeansSemanticNaming))
  merge.generateVisitorForPolymorphicDatatypes.set(this.generateVisitorForPolymorphicDatatypes.orElse(other.generateVisitorForPolymorphicDatatypes))
  merge.rootPackageName.set(this.rootPackageName.orElse(other.rootPackageName))
  merge.generateAsInternal.set(this.generateAsInternal.orElse(other.generateAsInternal))
  merge.operationIdGenerator.set(this.operationIdGenerator.orElse(other.operationIdGenerator))
  merge.operationOutputGenerator.set(this.operationOutputGenerator.orElse(other.operationOutputGenerator))
  merge.sealedClassesForEnumsMatching.set(this.sealedClassesForEnumsMatching.orElse(other.sealedClassesForEnumsMatching))
  merge.generateApolloMetadata.set(this.generateApolloMetadata.orElse(other.generateApolloMetadata))
  merge.alwaysGenerateTypesMatching.set(this.alwaysGenerateTypesMatching.orElse(other.alwaysGenerateTypesMatching))

  return merge
}

