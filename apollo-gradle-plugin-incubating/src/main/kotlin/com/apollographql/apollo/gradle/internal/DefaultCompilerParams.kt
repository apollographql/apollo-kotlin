package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilerParams
import org.gradle.api.model.ObjectFactory

class DefaultCompilerParams(val factory: ObjectFactory) : CompilerParams {
  override val generateKotlinModels = factory.property(Boolean::class.java)
  override fun generateKotlinModels(generateKotlinModels: Boolean) {
    this.generateKotlinModels.set(generateKotlinModels)
  }

  override val generateTransformedQueries = factory.property(Boolean::class.java)
  override fun generateTransformedQueries(generateTransformedQueries: Boolean) {
    this.generateTransformedQueries.set(generateTransformedQueries)
  }

  override val customTypeMapping = factory.mapProperty(String::class.java, String::class.java)
  override fun customTypeMapping(customTypeMapping: Map<String, String>) {
    this.customTypeMapping.set(customTypeMapping)
  }

  override val suppressRawTypesWarning = factory.property(Boolean::class.java)
  override fun suppressRawTypesWarning(suppressRawTypesWarning: Boolean) {
    this.suppressRawTypesWarning.set(suppressRawTypesWarning)
  }

  override val useSemanticNaming = factory.property(Boolean::class.java)
  override fun useSemanticNaming(useSemanticNaming: Boolean) {
    this.useSemanticNaming.set(useSemanticNaming)
  }

  override val nullableValueType = factory.property(String::class.java)
  override fun nullableValueType(nullableValueType: String) {
    this.nullableValueType.set(nullableValueType)
  }

  override val generateModelBuilder = factory.property(Boolean::class.java)
  override fun generateModelBuilder(generateModelBuilder: Boolean) {
    this.generateModelBuilder.set(generateModelBuilder)
  }

  override val useJavaBeansSemanticNaming = factory.property(Boolean::class.java)
  override fun useJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean) {
    this.useJavaBeansSemanticNaming.set(useJavaBeansSemanticNaming)
  }

  override val generateVisitorForPolymorphicDatatypes = factory.property(Boolean::class.java)
  override fun generateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean) {
    this.generateVisitorForPolymorphicDatatypes.set(generateVisitorForPolymorphicDatatypes)
  }

  @Deprecated(message = "please use generateKotlinModels instead", replaceWith = ReplaceWith("generateKotlinModels"))
  override fun setGenerateKotlinModels(generateKotlinModels: Boolean) {
    System.err.println("setGenerateKotlinModels() is deprecated, please use generateKotlinModels() instead")
    generateKotlinModels(generateKotlinModels)
  }

  @Deprecated(message = "please use generateTransformedQueries instead", replaceWith = ReplaceWith("generateTransformedQueries"))
  override fun setGenerateTransformedQueries(generateTransformedQueries: Boolean) {
    System.err.println("setGenerateTransformedQueries() is deprecated, please use generateTransformedQueries() instead")
    generateTransformedQueries(generateTransformedQueries)
  }

  @Deprecated(message = "please use customTypeMapping instead", replaceWith = ReplaceWith("customTypeMapping"))
  override fun setCustomTypeMapping(customTypeMapping: Map<String, String>) {
    System.err.println("setCustomTypeMapping() is deprecated, please use customTypeMapping() instead")
    customTypeMapping(customTypeMapping)
  }

  @Deprecated(message = "please use suppressRawTypesWarning instead", replaceWith = ReplaceWith("suppressRawTypesWarning"))
  override fun setSuppressRawTypesWarning(suppressRawTypesWarning: Boolean) {
    System.err.println("setSuppressRawTypesWarning() is deprecated, please use suppressRawTypesWarning() instead")
    suppressRawTypesWarning(suppressRawTypesWarning)
  }

  @Deprecated(message = "please use useSemanticNaming instead", replaceWith = ReplaceWith("useSemanticNaming"))
  override fun setUseSemanticNaming(useSemanticNaming: Boolean) {
    System.err.println("setUseSemanticNaming() is deprecated, please use useSemanticNaming() instead")
    useSemanticNaming(useSemanticNaming)
  }

  @Deprecated(message = "please use nullableValueType instead", replaceWith = ReplaceWith("nullableValueType"))
  override fun setNullableValueType(nullableValueType: String) {
    System.err.println("setNullableValueType() is deprecated, please use nullableValueType() instead")
    nullableValueType(nullableValueType)
  }

  @Deprecated(message = "please use generateModelBuilder instead", replaceWith = ReplaceWith("generateModelBuilder"))
  override fun setGenerateModelBuilder(generateModelBuilder: Boolean) {
    System.err.println("setGenerateModelBuilder() is deprecated, please use generateModelBuilder() instead")
    generateModelBuilder(generateModelBuilder)
  }

  @Deprecated(message = "please use useJavaBeansSemanticNaming instead", replaceWith = ReplaceWith("useJavaBeansSemanticNaming"))
  override fun setUseJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean) {
    System.err.println("setUseJavaBeansSemanticNaming() is deprecated, please use useJavaBeansSemanticNaming() instead")
    useJavaBeansSemanticNaming(useJavaBeansSemanticNaming)
  }

  @Deprecated(message = "please use generateVisitorForPolymorphicDatatypes instead", replaceWith = ReplaceWith("generateVisitorForPolymorphicDatatypes"))
  override fun setGenerateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean) {
    System.err.println("setGenerateVisitorForPolymorphicDatatypes() is deprecated, please use generateVisitorForPolymorphicDatatypes() instead")
    generateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes)
  }
}