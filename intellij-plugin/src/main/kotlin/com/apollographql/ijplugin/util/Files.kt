package com.apollographql.ijplugin.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import java.nio.file.Path

fun Project.findPsiFilesByName(fileName: String, searchScope: GlobalSearchScope): List<PsiFile> {
  val virtualFiles = FilenameIndex.getVirtualFilesByName(fileName, searchScope)
  return PsiUtilCore.toPsiFiles(PsiManager.getInstance(this), virtualFiles)
}

fun Project.findPsiFileByPath(path: String): PsiFile? {
  return VirtualFileManager.getInstance().findFileByNioPath(Path.of(path))?.let { PsiManager.getInstance(this).findFile(it) }
}

fun Project.findPsiFileByUrl(url: String): PsiFile? {
  return VirtualFileManager.getInstance().findFileByUrl(url)?.let { PsiManager.getInstance(this).findFile(it) }
}


fun Project.findPsiFilesByExtension(extension: String, searchScope: GlobalSearchScope): List<PsiFile> {
  val virtualFiles = FilenameIndex.getAllFilesByExt(this, extension, searchScope)
  return PsiUtilCore.toPsiFiles(PsiManager.getInstance(this), virtualFiles)
}

fun VirtualFile.isGenerated(project: Project): Boolean {
  return GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(this, project) || isApolloGenerated() || name.endsWith(".keystream")
}
