package com.apollographql.apollo.gradle.unit

import com.apollographql.apollo.gradle.ApolloCodeGenInstallTask
import com.apollographql.apollo.gradle.ApolloPlugin
import com.apollographql.apollo.gradle.ApolloPluginTestHelper
import groovy.json.JsonSlurper
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
    def task = project.tasks.getByName(ApolloCodeGenInstallTask.NAME)
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
    def task = project.tasks.getByName(ApolloCodeGenInstallTask.NAME)
    task.dependsOn.contains("nodeSetup")
  }

  def "task creates a package.json file under build/apollo-codegen"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    File packageFile = new File(project.buildDir, "apollo-codegen/package.json")
    assert packageFile.isFile()
    def input = new JsonSlurper().parseText(packageFile.text)
    assert input.name == "apollo-android"
    assert input.author == "Apollo"
  }

}
