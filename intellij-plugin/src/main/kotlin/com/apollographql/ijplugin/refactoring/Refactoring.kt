package com.apollographql.ijplugin.refactoring

import com.apollographql.ijplugin.util.findFunctionsByName
import com.apollographql.ijplugin.util.ktClassOrObject
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMigration
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName

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
  return runCatching {
    // We sometimes get KotlinExceptionWithAttachments: Unsupported reference
    ReferencesSearch.search(psiElement, GlobalSearchScope.projectScope(project), false).findAll()
  }
      .onFailure { e ->
        logw(e, "findReferences failed")
      }
      .getOrDefault(emptyList())
}

fun findMethodReferences(
    project: Project,
    className: String,
    methodName: String,
    extensionTargetClassName: String? = null,
    methodPredicate: (PsiMethod) -> Boolean = { true },
): Collection<PsiReference> {
  val psiLookupClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  val methods =
    // Try Kotlin first
    psiLookupClass.ktClassOrObject?.findFunctionsByName(methodName)?.takeIf { it.isNotEmpty() }
    // Fallback to Java
        ?: psiLookupClass.findMethodsByName(methodName, false)
            .filter { method ->
              if (extensionTargetClassName == null) return@filter methodPredicate(method)
              // In Kotlin extensions, the target is passed to the first parameter
              if (method.parameterList.parametersCount < 1) return@filter false
              val firstParameter = method.parameterList.parameters.first()
              val firstParameterType = (firstParameter.type as? PsiClassType)?.rawType()?.canonicalText
              firstParameterType == extensionTargetClassName && methodPredicate(method)
            }
  return methods.flatMap { method ->
    findReferences(method, project)
  }
}

fun findFieldReferences(project: Project, className: String, fieldName: String): Collection<PsiReference> {
  val psiLookupClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  val field = psiLookupClass.findFieldByName(fieldName, true)
  // Fallback to Kotlin property
      ?: psiLookupClass.ktClassOrObject?.findPropertyByName(fieldName)
      ?: return emptyList()
  return findReferences(field, project)
}

fun findClassReferences(project: Project, className: String): Collection<PsiReference> {
  val clazz = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project)) ?: return emptyList()
  return findReferences(clazz, project)
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

fun findAllClasses(project: Project): Collection<PsiClass> {
  return AllClassesSearch.search(GlobalSearchScope.projectScope(project), project).findAll()
}
