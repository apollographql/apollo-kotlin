package com.apollographql.apollo3.compiler.codegen.kotlin.experimental

import com.apollographql.apollo3.annotations.ApolloExperimental
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime
import com.google.common.truth.Truth.assertThat


@OptIn(ExperimentalTime::class, ApolloExperimental::class)
class ExplicitlyRemovedSubClassesTest {

  private lateinit var testClass: ExplicitlyRemovedSubClasses
  private lateinit var testClassWithReplace: ExplicitlyRemovedSubClasses

  private val replaceName = "TestContainerData"
  private val splitNames = listOf(
    "GetConversationMessagesQuery", "Data", "V3GetConversationMessagesV3GetConversationMessagesQuery",
    "V3GetConversationMessagesDataConnectionContainerData", "Connection", "Edge", "Node", "Board",
    "UserCollaboratingUsersConnectionContainerCollaboratingUsers", "Connection", "Edge", "Node"
  )

  @Before
  fun setUp() {
    val longClassName = "GetConversationMessagesQuery\$Data\$V3GetConversationMessagesV3GetConversationMessagesQuery$" +
        "V3GetConversationMessagesDataConnectionContainerData\$Connection\$Edge\$Node\$Board$" +
        "UserCollaboratingUsersConnectionContainerCollaboratingUsers\$Connection\$Edge\$Node"

    val extractSubclass = "GetConversationMessagesQuery\$Data\$V3GetConversationMessagesV3GetConversationMessagesQuery$" +
        "V3GetConversationMessagesDataConnectionContainerData"

    testClass = ExplicitlyRemovedSubClasses(
      className = longClassName,
      extract = extractSubclass,
      renameTo = ""
    )
    testClassWithReplace = ExplicitlyRemovedSubClasses(
      className = longClassName,
      extract = extractSubclass,
      renameTo = replaceName
    )
  }

  @Test
  fun getClassHierarchy() {
    val classHierarchy = testClass.classHierarchy
    assertThat(classHierarchy).isEqualTo(splitNames)
  }

  @Test
  fun getExtractHierarchy() {
    val extractHierarchy = testClass.extractHierarchy
    assertThat(extractHierarchy).isEqualTo(splitNames.dropLast(8))
  }

  @Test
  fun getRenamedExtractedHierarchy() {
    val renamedExtractedHierarchy = testClassWithReplace.renamedExtractedHierarchy
    assertThat(renamedExtractedHierarchy).isEqualTo(splitNames.dropLast(9) + replaceName)

    val renamedExtractedHierarchy2 = testClass.renamedExtractedHierarchy
    assertThat(renamedExtractedHierarchy2).isEqualTo(splitNames.dropLast(8))
  }

  @Test
  fun getSubclasses() {
    val testSubClasses = testClass.subclasses
    assertThat(testSubClasses).isEqualTo(splitNames.drop(4))

    val testSubClasses2 = testClassWithReplace.subclasses
    assertThat(testSubClasses2).isEqualTo(splitNames.drop(4))
  }

  @Test
  fun getParentPath() {
    val testPackagePath = testClass.parentPath
    assertThat(testPackagePath).isEqualTo(splitNames.dropLast(9))

    val testPackagePath2 = testClassWithReplace.parentPath
    assertThat(testPackagePath2).isEqualTo(splitNames.dropLast(9))
  }
}