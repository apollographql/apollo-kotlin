package com.apollographql.ijplugin

import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.ijplugin.util.logw
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.jsgraphql.GraphQLLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ui.UIUtil
import junit.framework.AssertionFailedError
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.io.File

@TestDataPath("\$CONTENT_ROOT/../../../tests/intellij-plugin-test-project")
abstract class ApolloTestCase : LightJavaCodeInsightFixtureTestCase() {
  open val mavenLibraries: List<String> = listOf("apollo-annotations", "apollo-api", "apollo-runtime", "org.jetbrains.kotlin:kotlin-stdlib:2.0.0")

  private val projectDescriptor = object : DefaultLightProjectDescriptor() {
    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
      for (library in mavenLibraries) {
        if (library.contains(":")) {
          addFromMaven(model, library, true, DependencyScope.COMPILE)
        } else {
          // XXX: tunnel that in an environment variable if possible
          val jarPath = "../libraries/$library/build/libs/$library-jvm-$APOLLO_VERSION.jar"

          PsiTestUtil.addProjectLibrary(
              model,
              "com.apollographql.apollo:$library:$APOLLO_VERSION",
              listOf(File(".").resolve(jarPath).absolutePath)
          )
        }
      }
    }
  }

  override fun getTestDataPath() = "../tests/intellij-plugin-test-project"

  override fun getProjectDescriptor(): LightProjectDescriptor {
    return projectDescriptor
  }

  override fun setUp() {
    super.setUp()
    // Set the indent size to 2 to match the fixtures (default is 4)
    val codeStyleSettings = CodeStyle.getSettings(project)
    val kotlinSettings = codeStyleSettings.getCommonSettings(KotlinLanguage.INSTANCE)
    kotlinSettings.indentOptions!!.INDENT_SIZE = 2
    kotlinSettings.WRAP_LONG_LINES = false
    val graphQLSettings = codeStyleSettings.getCommonSettings(GraphQLLanguage.INSTANCE)
    graphQLSettings.indentOptions!!.INDENT_SIZE = 2

    // Copy the 'project' test project to the virtual project dir
    if (testDataPath == "../tests/intellij-plugin-test-project") {
      myFixture.copyDirectoryToProject("", "")
    }

    // Make sure that all file system events up to this point have been processed
    VirtualFileManager.getInstance().syncRefresh()
    UIUtil.dispatchAllInvocationEvents()
  }

  protected inline fun <reified T : PsiElement> elementAt(text: String, afterText: String? = null): T? {
    val index = if (afterText != null) {
      file.text.indexOf(text, startIndex = file.text.indexOf(afterText)).takeIf { it != -1 }
          ?: throw NoSuchElementException("Couldn't find $text after $afterText")
    } else {
      file.text.indexOf(text).takeIf { it != -1 } ?: throw NoSuchElementException("Couldn't find $text")
    }
    return file.findElementAt(index)?.getNonStrictParentOfType<T>()
  }

  protected inline fun <reified T> PsiElement.assertTypeAndText(prefix: String) {
    try {
      assertInstanceOf(this, T::class.java)
    } catch (e: AssertionError) {
      throw AssertionError("Expected ${T::class.java.simpleName} but was ${this::class.java.simpleName}", e)
    }
    try {
      assertTrue(text.startsWith(prefix))
    } catch (e: AssertionError) {
      throw AssertionError("Expected text to start with $prefix but was $text", e)
    }
  }

  protected fun moveCaret(text: String, afterText: String? = null) {
    val index = if (afterText != null) {
      file.text.indexOf(text, startIndex = file.text.indexOf(afterText)).takeIf { it != -1 }
          ?: throw NoSuchElementException("Couldn't find $text after $afterText")
    } else {
      file.text.indexOf(text).takeIf { it != -1 } ?: throw NoSuchElementException("Couldn't find $text")
    }
    myFixture.editor.caretModel.moveToOffset(index)
  }

  protected fun doHighlighting(): List<HighlightInfo> {
    // Hack: sometimes doHighlighting fails with "AssertionError: PSI/document/model changes are not allowed during highlighting"
    (myFixture as? CodeInsightTestFixtureImpl)?.canChangeDocumentDuringHighlighting(true)
    // Wait a bit for project to settle and try again
    return attempt(3) { myFixture.doHighlighting() }
  }

  protected fun checkHighlighting() {
    attempt(3) { myFixture.checkHighlighting() }
  }

  private fun <T> attempt(times: Int, block: () -> T): T {
    var attempt = 1
    while (true) {
      try {
        return block()
      } catch (e: AssertionFailedError) {
        throw e
      } catch (e: AssertionError) {
        if (attempt <= times) {
          logw(e, "Attempt #$attempt failed, retrying")
          Thread.sleep(1000)
          attempt++
        } else {
          throw e
        }
      }
    }
  }

  protected val Segment.line: Int
    get() = StringUtil.offsetToLineNumber(myFixture.editor.document.text, startOffset) + 1

  /**
   * Load a .kts file and embed it inside a main function, so it becomes a valid .kt file.
   * This is an unfortunately necessary hack when testing inspections on .kts files which, in some cases, doesn't work because the IDE
   * doesn't register them in its scripting subsystem.
   */
  protected fun loadKtsAsKt(ktsFilePath: String): String {
    val ktsFileContents = FileUtil.loadFile(File(getTestDataPath() + "/" + ktsFilePath), null)
    return "fun main(){$ktsFileContents}"
  }
}
