package com.apollographql.ijplugin.ui.tree

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.NullNode
import com.intellij.ui.treeStructure.SimpleNode
import org.jetbrains.kotlin.idea.util.application.executeOnPooledThread

abstract class DynamicNode(
    project: Project,
    parent: DynamicNode?,
    private val computeChildrenOn: ComputeChildrenOn = ComputeChildrenOn.EXPANDED,
) : SimpleNode(project, parent) {
  private var children: List<SimpleNode> = listOf(LoadingNode())
  private var computeChildrenRequested: Boolean = false

  init {
    if (computeChildrenOn == ComputeChildrenOn.INIT) {
      requestUpdateChildren()
    }
  }

  fun onExpanded() {
    if (computeChildrenOn == ComputeChildrenOn.EXPANDED) {
      requestUpdateChildren()
    }
  }

  private fun requestUpdateChildren() {
    if (computeChildrenRequested) {
      return
    }
    computeChildrenRequested = true
    executeOnPooledThread {
      try {
        computeChildren()
      } catch (e: Exception) {
        logw(e, "DynamicNode: exception while computing children")
      }
    }
  }

  override fun getChildren(): Array<SimpleNode> {
    return children.toTypedArray()
  }

  protected fun updateChildren(children: List<SimpleNode>) {
    this.children = children
    invalidate()
  }

  protected fun updateChild(child: SimpleNode) {
    updateChildren(listOf(child))
  }


  open fun invalidate() {
    (parent as DynamicNode?)?.invalidate()
  }

  abstract fun computeChildren()

  enum class ComputeChildrenOn {
    INIT,
    EXPANDED,
  }
}

abstract class RootDynamicNode(
    project: Project,
    private val invalidate: () -> Unit,
    computeChildrenOn: ComputeChildrenOn = ComputeChildrenOn.EXPANDED,
) : DynamicNode(project, null, computeChildrenOn) {
  init {
    onExpanded()
  }

  override fun invalidate() {
    invalidate.invoke()
  }
}

private class LoadingNode : NullNode() {
  init {
    presentation.addText(ApolloBundle.message("tree.dynamicNode.loading"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
