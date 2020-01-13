package com.apollographql.apollo.gradle.unit

import com.apollographql.apollo.gradle.*
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloJavaPluginSpec extends Specification {

  def "creates a ClassGen task under the apollo group"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupJavaProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def mainTask = project.tasks.getByName(String.format(ApolloCodegenTask.NAME, "Main"))

    then:
    mainTask.group.equals(ApolloPlugin.TASK_GROUP)
    mainTask.description.equals("Generate Android classes for Main GraphQL queries")
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
      assert (sourceSet.extensions.findByName(GraphQLSourceDirectorySet.NAME)) != null
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
