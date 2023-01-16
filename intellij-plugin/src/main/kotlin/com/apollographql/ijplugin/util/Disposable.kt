package com.apollographql.ijplugin.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

fun Disposable?.isNullOrDisposed() = this == null || Disposer.isDisposed(this)
