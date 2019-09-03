package com.apollographql.apollo.gradle.android

import org.gradle.api.Project

class AndroidTaskConfiguratorFactory {

  static AndroidTaskConfigurator create(Project project) {
    return new AndroidTaskConfigurator(project)
  }
}
