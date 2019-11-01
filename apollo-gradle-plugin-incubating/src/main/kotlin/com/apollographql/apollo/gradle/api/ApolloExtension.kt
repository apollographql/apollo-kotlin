package com.apollographql.apollo.gradle.api

import com.apollographql.apollo.gradle.internal.DefaultService
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.provider.Property

interface ApolloExtension: CompilerParams {

  val compilationUnits: DomainObjectCollection<CompilationUnit>

  fun service(name: String, action: Action<DefaultService>)

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  val schemaFilePath: Property<String>

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  fun setSchemaFilePath(schemaFilePath: String)

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  val outputPackageName: Property<String>

  /**
   * @Deprecated
   *
   * Used for backward compatibility reasons with the old groovy plugin
   */
  @Deprecated("please use services instead")
  fun setOutputPackageName(outputPackageName: String)
}