package com.apollographql.ijplugin.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

fun Disposable.newDisposable(debugName: String) = Disposer.newDisposable(this, debugName)

fun dispose(disposable: Disposable?) = disposable?.let { Disposer.dispose(it) }

fun Disposable?.isNotDisposed() = this != null && !Disposer.isDisposed(this)
