package com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item

import com.apollographql.ijplugin.refactoring.findInheritorsOfClass
import com.apollographql.ijplugin.util.apollo3
import com.apollographql.ijplugin.util.logd
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.psi.KtClass

fun findAllModels(project: Project): List<KtClass> {
  logd()
  val operationInheritors = findInheritorsOfClass(project, "$apollo3.api.Operation").filterIsInstance<KtLightClassBase>()
  val fragmentDataInheritors = findInheritorsOfClass(project, "$apollo3.api.Fragment.Data").filterIsInstance<KtLightClassBase>()
  val allModels: List<KtClass> = (operationInheritors + fragmentDataInheritors).flatMap {
    it.kotlinOrigin?.body?.declarations.orEmpty().filterIsInstance<KtClass>()
  } + fragmentDataInheritors.map { it.kotlinOrigin }.filterIsInstance<KtClass>()
  logd("size=${allModels.size}")
  return allModels
}
