package com.apollographql.ijplugin.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer

fun Disposable.newDisposable(): CheckedDisposable {
  val checkedDisposable = Disposer.newCheckedDisposable()
  Disposer.register(this, checkedDisposable)
  return checkedDisposable
}

fun dispose(disposable: Disposable?) = disposable?.let { Disposer.dispose(it) }

fun CheckedDisposable?.isNotDisposed() = this != null && !isDisposed
