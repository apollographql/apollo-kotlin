package com.apollographql.apollo.gradle.unit

import com.apollographql.apollo.gradle.ApolloCodegenInstallTask
import com.apollographql.apollo.gradle.ApolloPlugin
import com.apollographql.apollo.gradle.ApolloPluginTestHelper
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloCodeGenInstallTaskSpec extends Specification {
  def "creates a task under the apollo group"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    def task = project.tasks.getByName(ApolloCodegenInstallTask.NAME)
    task.group.equals(ApolloPlugin.TASK_GROUP)
    task.description.equals("Runs npm install for apollo-codegen")
  }

  def "creates a task that depends on nodeSetup"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    def task = project.tasks.getByName(ApolloCodegenInstallTask.NAME)
    task.dependsOn.contains("npmSetup")
  }
}
