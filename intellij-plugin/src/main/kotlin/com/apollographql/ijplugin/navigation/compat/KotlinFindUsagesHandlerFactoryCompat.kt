package com.apollographql.ijplugin.navigation.compat

import com.apollographql.ijplugin.util.logw
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

private const val POST_231_CLASS_NAME = "org.jetbrains.kotlin.idea.base.searching.usages.KotlinFindUsagesHandlerFactory"
private const val PRE_231_CLASS_NAME = "org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory"

class KotlinFindUsagesHandlerFactoryCompat(project: Project) : FindUsagesHandlerFactory() {
  private val delegateClass = runCatching {
    // Try with the recent version first (changed package since platform 231)
    Class.forName(POST_231_CLASS_NAME)
  }
      .recoverCatching {
        // Fallback to the old version
        Class.forName(PRE_231_CLASS_NAME)
      }
      .onFailure { logw(it, "Could not load either $POST_231_CLASS_NAME nor $PRE_231_CLASS_NAME") }
      .getOrNull()

  private val delegate: FindUsagesHandlerFactory? = try {
    delegateClass?.let { it.getConstructor(Project::class.java).newInstance(project) as FindUsagesHandlerFactory }
  } catch (e: Exception) {
    // ProcessCanceledException can sometimes happen here
    logw(e, "Could not instantiate KotlinFindUsagesHandlerFactory")
    null
  }


  override fun canFindUsages(element: PsiElement): Boolean {
    return delegate?.canFindUsages(element) ?: false
  }

  override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
    return delegate?.createFindUsagesHandler(element, forHighlightUsages)
  }

  val findPropertyOptions: FindUsagesOptions?
    get() = delegateClass?.getDeclaredMethod("getFindPropertyOptions")
        ?.invoke(this.delegate) as? FindUsagesOptions

  val findClassOptions: FindUsagesOptions?
    get() = delegateClass?.getDeclaredMethod("getFindClassOptions")
        ?.invoke(this.delegate) as? FindUsagesOptions
}
