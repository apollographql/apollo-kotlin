package com.apollographql.android.gradle.unit

import com.apollographql.android.VersionKt
import com.apollographql.android.gradle.*
import com.moowork.gradle.node.NodePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloAndroidPluginSpec extends Specification {
  def "creates an IRGen task under the apollo group for a default project"() {
    setup:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    def debugTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Debug"))
    def releaseTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "Release"))

    then:
    debugTask.group.equals(ApolloPlugin.TASK_GROUP)
    debugTask.description.equals("Generate an IR file using apollo-codegen for Debug GraphQL queries")

    releaseTask.group.equals(ApolloPlugin.TASK_GROUP)
    releaseTask.description.equals("Generate an IR file using apollo-codegen for Release GraphQL queries")
  }

  def "creates an IRGen task under the apollo group for a product-flavoured project"() {
    setup:
    def project = ProjectBuilder.builder().build()
    def flavors = ["Demo", "Full"]
    ApolloPluginTestHelper.setupAndroidProjectWithProductFlavours(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    flavors.each { flavor ->
      def debugTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "${flavor}Debug"))
      def releaseTask = project.tasks.getByName(String.format(ApolloIRGenTask.NAME, "${flavor}Release"))

      assert (debugTask.group) == ApolloPlugin.TASK_GROUP
      assert (debugTask.description) == "Generate an IR file using apollo-codegen for " + flavor + "Debug GraphQL queries"

      assert (releaseTask.group) == ApolloPlugin.TASK_GROUP
      assert (releaseTask.description) == "Generate an IR file using apollo-codegen for " + flavor + "Release GraphQL queries"
    }
  }

  def "adds the node plugin to the project"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    project.plugins.hasPlugin(NodePlugin.class)
  }

  def "adds a graphql extension for all sourceSets in a default project"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    project.android.sourceSets.all { sourceSet ->
      assert (sourceSet.extensions.findByName("graphql")) != null
      assert (sourceSet.extensions.findByType(GraphQLSourceDirectorySet.class)) != null
    }
  }

  def "adds a graphql extensions for all sourceSets in a product-flavoured project"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupAndroidProjectWithProductFlavours(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    project.android.sourceSets.all { sourceSet ->
      assert (sourceSet.extensions.findByName("graphql")) != null
      assert (sourceSet.extensions.findByType(GraphQLSourceDirectorySet.class)) != null
    }
  }

  def "adds apollo project-level extension"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupAndroidProjectWithProductFlavours(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    assert (project.extensions.findByName("apollo")) != null
    assert (project.extensions.findByType(ApolloExtension.class)) != null
  }

  def "adds apollo-api dependency"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    def apolloApi = project.configurations.getByName("compile").dependencies.find {
      it.group == "com.apollographql.android" && it.name == "api"
    }
    assert apolloApi != null
  }
}
