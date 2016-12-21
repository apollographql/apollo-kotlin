package com.apollostack.android.gradle

import com.moowork.gradle.node.npm.NpmTask
import groovy.json.JsonSlurper

class ApolloCodeGenInstallTask extends NpmTask {
  static final String NAME = "installApolloCodegen"
  static final String INSTALL_DIR = "node_modules/apollo-codegen"
  static final String APOLLOCODEGEN_VERSION = "0.9.6"

  public ApolloCodeGenInstallTask() {
    group = ApolloPlugin.TASK_GROUP
    description = "Runs npm install for apollo-codegen"
    File installDir = this.project.file(INSTALL_DIR)

    setArgs(["install", "apollo-codegen@$APOLLOCODEGEN_VERSION"])

    if (!installDir.exists()) {
      installDir.mkdirs()
    } else {
      if (!apolloVersion()?.equals(APOLLOCODEGEN_VERSION)) {
        installDir.deleteDir()
      }
    }
    getOutputs().dir(installDir)
  }

  String apolloVersion() {
    String version = null
    File packageFile = project.file("${INSTALL_DIR}/package.json")
    if (packageFile.isFile()) {
      def input = new JsonSlurper().parseText(packageFile.text)
      version = input.version
    }
    return version
  }
}
