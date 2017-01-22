package com.apollostack.android.gradle.unit

import com.apollostack.android.gradle.ApolloExtension
import com.apollostack.android.gradle.ApolloIRGenTask
import com.apollostack.android.gradle.ApolloPlugin
import com.apollostack.android.gradle.ApolloPluginTestHelper
import com.apollostack.android.gradle.GraphQLExtension
import com.moowork.gradle.node.NodePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloPluginSpec extends Specification {
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
    debugTask.description.equals("Generate an IR file using apollo-codgen for Debug GraphQL queries")

    releaseTask.group.equals(ApolloPlugin.TASK_GROUP)
    releaseTask.description.equals("Generate an IR file using apollo-codgen for Release GraphQL queries")
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
      assert (debugTask.description) == "Generate an IR file using apollo-codgen for " + flavor + "Debug GraphQL queries"

      assert (releaseTask.group) == ApolloPlugin.TASK_GROUP
      assert (releaseTask.description) == "Generate an IR file using apollo-codgen for " + flavor + "Release GraphQL queries"
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
      assert (sourceSet.extensions.findByType(GraphQLExtension.class)) != null
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
      assert (sourceSet.extensions.findByType(GraphQLExtension.class)) != null
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
}
