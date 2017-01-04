package com.apollostack.android.gradle.unit

import com.apollostack.android.gradle.ApolloCodeGenInstallTask
import com.apollostack.android.gradle.ApolloPlugin
import com.apollostack.android.gradle.ApolloPluginTestHelper
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

    then:
    def task = project.tasks.getByName(ApolloCodeGenInstallTask.NAME)
    task.dependsOn.contains("nodeSetup")
  }

  def "configures the npm install params"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)

    then:
    project.tasks.getByName(ApolloCodeGenInstallTask.NAME).args.equals(
        ["install", "apollo-codegen@$ApolloCodeGenInstallTask.APOLLOCODEGEN_VERSION", "--save", "--save-exact"])
  }

  def "task creates node_modules/apollo-codegen output dir"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)

    then:
    project.tasks.getByName(ApolloCodeGenInstallTask.NAME).outputs.hasOutput
    project.tasks.getByName(ApolloCodeGenInstallTask.NAME).outputs.files
        .contains(project.file(ApolloCodeGenInstallTask.INSTALL_DIR))
  }

  def "task creates a package.json file in project root"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)

    then:
    File packageFile = project.file("package.json")
    assert packageFile.isFile()
    def input = new JsonSlurper().parseText(packageFile.text)
    assert input.name == "apollo-android"
    assert input.author == "Apollo"
  }

}
