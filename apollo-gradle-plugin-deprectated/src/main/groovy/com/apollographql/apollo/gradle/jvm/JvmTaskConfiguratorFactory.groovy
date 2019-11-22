package com.apollographql.apollo.gradle.jvm

import org.gradle.api.Project

class JvmTaskConfiguratorFactory {

  static JvmTaskConfigurator create(Project project) {
    return new JvmTaskConfigurator(project)
  }
}
