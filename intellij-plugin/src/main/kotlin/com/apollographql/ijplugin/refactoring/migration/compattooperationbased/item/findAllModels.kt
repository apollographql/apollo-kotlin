package com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item

import com.apollographql.ijplugin.refactoring.findInheritorsOfClass
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.psi.KtClass

fun findAllModels(project: Project): List<KtClass> {
  val operationInheritors = findInheritorsOfClass(project, "com.apollographql.apollo3.api.Operation").filterIsInstance<KtLightClassBase>()
  val fragmentDataInheritors = findInheritorsOfClass(project, "com.apollographql.apollo3.api.Fragment.Data").filterIsInstance<KtLightClassBase>()
  val allModels: List<KtClass> = (operationInheritors + fragmentDataInheritors).flatMap {
    it.kotlinOrigin?.body?.declarations.orEmpty().filterIsInstance<KtClass>()
  } + fragmentDataInheritors.map { it.kotlinOrigin }.filterIsInstance<KtClass>()
  return allModels
}
