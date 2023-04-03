package com.apollographql.ijplugin.refactoring

import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMigration
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.rename.RenamePsiElementProcessor

fun findOrCreatePackage(project: Project, migration: PsiMigration, qName: String): PsiPackage {
  val aPackage = JavaPsiFacade.getInstance(project).findPackage(qName)
  return aPackage ?: WriteAction.compute<PsiPackage, RuntimeException> {
    migration.createPackage(qName)
  }
}

fun findOrCreateClass(project: Project, migration: PsiMigration, qName: String): PsiClass {
  val classes = JavaPsiFacade.getInstance(project).findClasses(qName, GlobalSearchScope.allScope(project))
  return classes.firstOrNull() ?: WriteAction.compute<PsiClass, RuntimeException> {
    migration.createClass(qName)
  }
}

fun PsiElement.bindReferencesToElement(element: PsiElement): PsiElement? {
  for (reference in references) {
    try {
      return reference.bindToElement(element)
    } catch (t: Throwable) {
      logw(t, "bindToElement failed: ignoring")
    }
  }
  return null
}

fun findReferences(psiElement: PsiElement, project: Project): Collection<PsiReference> {
  return ReferencesSearch.search(psiElement, GlobalSearchScope.projectScope(project), false).toList()
}

fun findMethodReferences(
    project: Project,
    className: String,
    methodName: String,
    extensionTargetClassName: String? = null,
): Collection<PsiReference> {
  val psiLookupClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  val methods = psiLookupClass.findMethodsByName(methodName, false)
      .filter { method ->
        if (extensionTargetClassName == null) return@filter true
        // In Kotlin extensions, the target is passed to the first parameter
        if (method.parameterList.parametersCount < 1) return@filter false
        val firstParameter = method.parameterList.parameters.first()
        val firstParameterType = (firstParameter.type as? PsiClassType)?.rawType()?.canonicalText
        firstParameterType == extensionTargetClassName
      }
  return methods.flatMap { method ->
    val processor = RenamePsiElementProcessor.forElement(method)
    processor.findReferences(method, GlobalSearchScope.projectScope(project), false)
  }
}

fun findFieldReferences(project: Project, className: String, fieldName: String): Collection<PsiReference> {
  val psiLookupClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  val field = psiLookupClass.findFieldByName(fieldName, true) ?: return emptyList()
  val processor = RenamePsiElementProcessor.forElement(field)
  return processor.findReferences(field, GlobalSearchScope.projectScope(project), false)
}

fun findClassReferences(project: Project, className: String): Collection<PsiReference> {
  val clazz = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  val processor = RenamePsiElementProcessor.forElement(clazz)
  return processor.findReferences(clazz, GlobalSearchScope.projectScope(project), false)
}

fun findPackageReferences(project: Project, packageName: String): Collection<PsiReference> {
  val psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName) ?: return emptyList()
  return findReferences(psiPackage, project)
}

fun findInheritorsOfClass(project: Project, className: String): Collection<PsiClass> {
  val psiLookupClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  // Using allScope for the search so all inheritors are found even if some of them are not in the project
  return ClassInheritorsSearch.search(psiLookupClass, GlobalSearchScope.allScope(project), true).toList()
}
