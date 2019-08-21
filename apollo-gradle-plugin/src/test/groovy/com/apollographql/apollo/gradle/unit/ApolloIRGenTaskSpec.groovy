package com.apollographql.apollo.gradle.unit

import com.apollographql.apollo.gradle.ApolloCodegenInstallTask
import com.apollographql.apollo.gradle.ApolloPluginTestHelper
import com.apollographql.apollo.gradle.TaskConfigurator
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloIRGenTaskSpec extends Specification {

  def setupSpec() {
    System.setProperty("apollographql.useExperimentalCodegen", "false")
  }

  def cleanupSpec() {
    System.clearProperty("apollographql.useExperimentalCodegen")
  }

  def "creates tasks for default project variants that depend on apolloCodegenInstall task"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def debugTask = project.tasks.getByName(String.format(TaskConfigurator.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Debug"))
    def releaseTask = project.tasks.getByName(String.format(TaskConfigurator.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Release"))

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

    def generateApolloIR = project.tasks.getByName(String.format(TaskConfigurator.APOLLO_CODEGEN_GENERATE_TASK_NAME, ""))

    then:
    generateApolloIR.dependsOn.contains(project.tasks.getByName(String.format(TaskConfigurator.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Debug")))
    generateApolloIR.dependsOn.contains(project.tasks.getByName(String.format(TaskConfigurator.APOLLO_CODEGEN_GENERATE_TASK_NAME, "Release")))
  }
}
