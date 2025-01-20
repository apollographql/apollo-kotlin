package com.apollographql.ijplugin.util

val isLspAvailable = runCatching { Class.forName("com.intellij.platform.lsp.api.LspServerManager") }.isSuccess

val isAndroidPluginPresent = runCatching { Class.forName("com.android.ddmlib.AndroidDebugBridge") }.isSuccess

val isJavaPluginPresent = runCatching { Class.forName("com.intellij.psi.PsiJavaFile") }.isSuccess

val isKotlinPluginPresent = runCatching { Class.forName("org.jetbrains.kotlin.psi.KtFile") }.isSuccess

val isGradlePluginPresent = runCatching { Class.forName("org.jetbrains.plugins.gradle.util.GradleConstants") }.isSuccess
