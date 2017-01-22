package com.apollostack.android.gradle.unit

import com.apollostack.android.gradle.ApolloCodeGenInstallTask
import com.apollostack.android.gradle.ApolloIRGenTask
import com.apollostack.android.gradle.ApolloPluginTestHelper
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloIRGenTaskSpec extends Specification {
  def "creates tasks for default project variants that depend on apolloCodgenInstall task"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)

    def debugTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Debug"))
    def releaseTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Release"))

    then:
    debugTask.dependsOn.contains(ApolloCodeGenInstallTask.NAME)
    releaseTask.dependsOn.contains(ApolloCodeGenInstallTask.NAME)
  }

  def "creates a top-level generateApolloIR task that depends on the variant tasks in a default project"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    def generateApolloIR = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, ""))

    then:
    generateApolloIR.dependsOn.contains(project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Debug")))
    generateApolloIR.dependsOn.contains(project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Release")))
  }
}
