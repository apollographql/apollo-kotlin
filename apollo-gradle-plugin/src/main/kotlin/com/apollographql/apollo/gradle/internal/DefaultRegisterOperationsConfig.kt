package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.RegisterOperationsConfig
import org.gradle.api.provider.Property

abstract class DefaultRegisterOperationsConfig: RegisterOperationsConfig {
  abstract override val key: Property<String>
  abstract override val graph: Property<String>
  abstract override val graphVariant: Property<String>
}