package com.apollographql.apollo.gradle.unit

import com.apollographql.apollo.gradle.ApolloCodegenInstallTask
import com.apollographql.apollo.gradle.ApolloPlugin
import com.apollographql.apollo.gradle.ApolloPluginTestHelper
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloIRGenTaskSpec extends Specification {
  def "creates tasks for default project variants that depend on apolloCodegenInstall task"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def debugTask = project.tasks.getByName(String.format(ApolloPlugin.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Debug"))
    def releaseTask = project.tasks.getByName(String.format(ApolloPlugin.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Release"))

    then:
    debugTask.dependsOn.contains(ApolloCodegenInstallTask.NAME)
    releaseTask.dependsOn.contains(ApolloCodegenInstallTask.NAME)
  }

  def "creates a top-level generateApolloIR task that depends on the variant tasks in a default project"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def generateApolloIR = project.tasks.getByName(String.format(ApolloPlugin.APOLLO_CODEGEN_GENERATE_TASK_NAME, ""))

    then:
    generateApolloIR.dependsOn.contains(project.tasks.getByName(String.format(ApolloPlugin.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Debug")))
    generateApolloIR.dependsOn.contains(project.tasks.getByName(String.format(ApolloPlugin.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Release")))
  }
}
