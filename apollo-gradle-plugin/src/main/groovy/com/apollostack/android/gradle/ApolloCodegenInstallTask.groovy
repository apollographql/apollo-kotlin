package com.apollostack.android.gradle

import com.moowork.gradle.node.npm.NpmTask

class ApolloCodegenInstallTask extends NpmTask {
  static final String NAME = "installApolloCodegen"
  private static final String INSTALL_DIR = "node_modules/apollo-codegen"

  public ApolloCodegenInstallTask() {
    super()
    this.group = ApolloPlugin.TASK_GROUP
    this.description = "Runs npm install for apollo-codegen"
    setArgs(["install", "apollo-codegen"])
    File installDir = this.project.file(INSTALL_DIR)
    if (!installDir.exists()) {
      installDir.mkdirs()
    }
    getOutputs().dir(installDir)
  }
}
