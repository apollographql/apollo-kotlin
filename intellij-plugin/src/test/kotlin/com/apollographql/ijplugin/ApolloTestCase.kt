package com.apollographql.ijplugin

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

@TestDataPath("\$CONTENT_ROOT/testData/project")
abstract class ApolloTestCase : LightJavaCodeInsightFixtureTestCase() {
  abstract val mavenLibraries: List<String>

  private val projectDescriptor = object : DefaultLightProjectDescriptor() {
    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
      for (library in mavenLibraries) {
        addFromMaven(model, library, true, DependencyScope.COMPILE)
      }
    }
  }

  override fun getTestDataPath() = "src/test/testData/project"

  override fun getProjectDescriptor(): LightProjectDescriptor {
    return projectDescriptor
  }

  override fun setUp() {
    super.setUp()
    // Set the indent size to 2 to match the fixtures (default is 4)
    val codeStyleSettings = CodeStyle.getSettings(project)
    val kotlinSettings = codeStyleSettings.getCommonSettings(KotlinLanguage.INSTANCE)
    kotlinSettings.indentOptions!!.INDENT_SIZE = 2

    // Copy the 'project' test project to the virtual project dir
    if (testDataPath == "src/test/testData/project") {
      myFixture.copyDirectoryToProject("", "")
    }
  }

  protected inline fun <reified T : PsiElement> elementAt(text: String, afterText: String? = null): T? {
    val index = if (afterText != null) {
      file.text.indexOf(text, startIndex = file.text.indexOf(afterText)).takeIf { it != -1 } ?: throw NoSuchElementException("Couldn't find $text after $afterText")
    } else {
      file.text.indexOf(text).takeIf { it != -1 } ?: throw NoSuchElementException("Couldn't find $text")
    }
    return file.findElementAt(index)?.getNonStrictParentOfType<T>()
  }
}
