package com.apollostack.android.gradle.project

import com.apollostack.android.gradle.ApolloCodeGenInstallTask
import com.apollostack.android.gradle.ApolloPlugin
import com.apollostack.android.gradle.Utils
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloCodeGenInstallTaskSpec extends Specification {

  def "creates a task under the apollo group"() {
    setup:
    def project = ProjectBuilder.builder().build()
    Utils.setupDefaultAndroidProject(project)

    when:
    Utils.applyApolloPlugin(project)

    then:
    def task = project.tasks.getByName(ApolloCodeGenInstallTask.NAME)
    task.group.equals(ApolloPlugin.TASK_GROUP)
    task.description.equals("Runs npm install for apollo-codegen")
  }

  def "creates a task that depends on nodeSetup"() {
    setup:
    def project = ProjectBuilder.builder().build()
    Utils.setupDefaultAndroidProject(project)

    when:
    Utils.applyApolloPlugin(project)

    then:
    def task = project.tasks.getByName(ApolloCodeGenInstallTask.NAME)
    task.dependsOn.contains("nodeSetup")
  }

  def "configures the npm install params"() {
    setup:
    def project = ProjectBuilder.builder().build()
    Utils.setupDefaultAndroidProject(project)

    when:
    Utils.applyApolloPlugin(project)

    then:
    project.tasks.getByName(ApolloCodeGenInstallTask.NAME).args.equals(
        ["install", "apollo-codegen@${ApolloCodeGenInstallTask.APOLLOCODEGEN_VERSION}"])
  }

  def "task creates node_modules/apollo-codegen output dir"() {
    setup:
    def project = ProjectBuilder.builder().build()
    Utils.setupDefaultAndroidProject(project)

    when:
    Utils.applyApolloPlugin(project)

    then:
    project.tasks.getByName(ApolloCodeGenInstallTask.NAME).outputs.hasOutput
    project.tasks.getByName(ApolloCodeGenInstallTask.NAME).outputs.files
        .contains(project.file(ApolloCodeGenInstallTask.INSTALL_DIR))
    project.file(ApolloCodeGenInstallTask.INSTALL_DIR).exists()
  }
}
