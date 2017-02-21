package com.apollographql.android.gradle.unit

import com.apollographql.android.gradle.*
import com.moowork.gradle.node.NodePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloJavaPluginSpec extends Specification {
  def "creates an IRGen task under the apollo group"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupJavaProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def mainTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Main"))

    then:
    mainTask.group.equals(ApolloPlugin.TASK_GROUP)
    mainTask.description.equals("Generate an IR file using apollo-codegen for Main GraphQL queries")
  }

  def "creates a ClassGen task under the apollo group"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupJavaProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def mainTask = project.tasks.getByName(String.format(ApolloClassGenTask.NAME, "Main"))

    then:
    mainTask.group.equals(ApolloPlugin.TASK_GROUP)
    mainTask.description.equals("Generate Android classes for Main GraphQL queries")
  }

  def "adds the node plugin to the project"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupJavaProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    project.plugins.hasPlugin(NodePlugin.class)
  }

  def "adds a graphql extension for all sourceSets"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupJavaProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    project.sourceSets.all { sourceSet ->
      assert (sourceSet.extensions.findByName("graphql")) != null
      assert (sourceSet.extensions.findByType(GraphQLSourceDirectorySet.class)) != null
    }
  }

  def "adds apollo project-level extension"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupJavaProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    assert (project.extensions.findByName("apollo")) != null
    assert (project.extensions.findByType(ApolloExtension.class)) != null
  }
}
