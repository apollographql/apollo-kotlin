package com.apollographql.apollo.gradle.unit

import com.apollographql.apollo.gradle.ApolloExtension
import com.apollographql.apollo.gradle.ApolloPluginTestHelper
import com.apollographql.apollo.gradle.GraphQLSourceDirectorySet
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ApolloAndroidPluginSpec extends Specification {

  def "adds a graphql extension for all sourceSets in a default project"() {
    given:
    def project = ProjectBuilder.builder().build()
    ApolloPluginTestHelper.setupDefaultAndroidProject(project)

    when:
    ApolloPluginTestHelper.applyApolloPlugin(project)
    project.evaluate()

    then:
    project.android.sourceSets.all { sourceSet ->
      assert (sourceSet.extensions.findByName(GraphQLSourceDirectorySet.NAME)) != null
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
      assert (sourceSet.extensions.findByName(GraphQLSourceDirectorySet.NAME)) != null
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
