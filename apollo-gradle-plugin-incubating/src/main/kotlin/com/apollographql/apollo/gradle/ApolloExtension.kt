package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.NullableValueType
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

open class ApolloExtension(project: Project) {
  @JvmField
  var nullableValueType: String? = null
  @JvmField
  var useSemanticNaming = true
  @JvmField
  var generateModelBuilder = false
  @JvmField
  var useJavaBeansSemanticNaming = false
  @JvmField
  var suppressRawTypesWarning = false
  @JvmField
  var generateKotlinModels = false
  @JvmField
  var generateVisitorForPolymorphicDatatypes = false
  @JvmField
  var generateTransformedQueries = false

  @JvmField
  var customTypeMapping: Map<String, String> = emptyMap()

  @JvmField
  var services = mutableListOf<Service>()

  val compilationUnits = project.container(CompilationUnit::class.java)

  /**
   * Deprecated,use @{link service} instead
   */
  @JvmField
  var schemaFilePath: String? = null

  /**
   * Deprecated, use @{link service} instead
   */
  @JvmField
  var outputPackageName: String? = null

  fun service(name: String, action: Action<Service>) {
    val service = Service(name)
    action.execute(service)
    services.add(service)
  }

  /**
   * For backward compatibility
   */
  fun setNullableValueType(nullableValueType: String) {
    this.nullableValueType = nullableValueType
  }

  /**
   * For backward compatibility
   */
  fun setUseSemanticNaming(useSemanticNaming: Boolean) {
    this.useSemanticNaming = useSemanticNaming
  }

  /**
   * For backward compatibility
   */
  fun setGenerateModelBuilder(generateModelBuilder: Boolean) {
    this.generateModelBuilder = generateModelBuilder
  }

  /**
   * For backward compatibility
   */
  fun setUseJavaBeansSemanticNaming(useJavaBeansSemanticNaming: Boolean) {
    this.useJavaBeansSemanticNaming = useJavaBeansSemanticNaming
  }

  /**
   * For backward compatibility
   */
  fun setSuppressRawTypesWarning(suppressRawTypesWarning: Boolean) {
    this.suppressRawTypesWarning = suppressRawTypesWarning
  }

  /**
   * For backward compatibility
   */
  fun setGenerateKotlinModels(generateKotlinModels: Boolean) {
    this.generateKotlinModels = generateKotlinModels
  }

  /**
   * For backward compatibility
   */
  fun setGenerateVisitorForPolymorphicDatatypes(generateVisitorForPolymorphicDatatypes: Boolean) {
    this.generateKotlinModels = generateVisitorForPolymorphicDatatypes
  }

  /**
   * For backward compatibility
   */
  fun setSchemaFilePath(schemaFilePath: String) {
    this.schemaFilePath = schemaFilePath
  }

  /**
   * For backward compatibility
   */
  fun setOutputPackageName(outputPackageName: String) {
    this.outputPackageName = outputPackageName
  }

  /**
   * For backward compatibility
   */
  fun setCustomTypeMapping(customTypeMapping: Map<String, String>) {
    this.customTypeMapping = customTypeMapping
  }

  /**
   * For backward compatibility
   */
  fun setGenerateTransformedQueries(generateTransformedQueries: Boolean) {
    this.generateTransformedQueries = generateTransformedQueries
  }
}