package com.apollographql.ijplugin.normalizedcache

import android.annotation.SuppressLint
import com.android.ddmlib.IDevice
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.ui.tree.DynamicNode
import com.apollographql.ijplugin.ui.tree.RootDynamicNode
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.AutoExpandSimpleNodeListener
import com.intellij.ui.treeStructure.NullNode
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.util.ui.tree.TreeUtil
import icons.StudioIcons
import java.awt.event.InputEvent
import java.io.File
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeSelectionModel

class PullFromDeviceDialogWrapper(
    private val project: Project,
    private val onFilePullSuccess: (File) -> Unit,
    private val onFilePullError: (Throwable) -> Unit,
) : DialogWrapper(project, true), Disposable {
  private lateinit var tree: SimpleTree
  private lateinit var model: StructureTreeModel<PullFromDeviceTreeStructure>

  init {
    title = ApolloBundle.message("normalizedCacheViewer.pullFromDevice.title")
    init()
    okAction.isEnabled = false
  }

  override fun createCenterPanel(): DialogPanel = panel {
    row {
      scrollCell(createTree())
          .horizontalAlign(HorizontalAlign.FILL)
    }

  }.withPreferredWidth(450)

  override fun getDimensionServiceKey(): String? {
    return PullFromDeviceDialogWrapper::class.java.simpleName
  }

  private fun createTree(): SimpleTree {
    tree = SimpleTree().apply {
      emptyText.setText(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.loading"))
      selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
      isRootVisible = false
      showsRootHandles = true
      isLargeModel = true
      TreeUtil.installActions(this)
      TreeSpeedSearch(this)

      model = createModel(this)

      addTreeExpansionListener(object : TreeExpansionListener {
        override fun treeExpanded(event: TreeExpansionEvent) {
          event.path.lastPathComponent.cast<DefaultMutableTreeNode>()?.userObject?.cast<DynamicNode>()?.onExpanded()
        }

        override fun treeCollapsed(event: TreeExpansionEvent) {}
      })

      addTreeSelectionListener {
        okAction.isEnabled = it.path.lastPathComponent.cast<DefaultMutableTreeNode>()?.userObject is DatabaseNode
      }
    }
    return tree
  }

  private fun createModel(tree: SimpleTree): TreeModel {
    val structure = PullFromDeviceTreeStructure(project, invalidate = {
      invokeLater(ModalityState.any()) {
        model.invalidateAsync()
      }
    })
    model = StructureTreeModel(structure, this)
    return AsyncTreeModel(model, this).apply {
      addTreeModelListener(AutoExpandSimpleNodeListener(tree))
    }
  }

  override fun doOKAction() {
    val dbNode = tree.selectionPath?.lastPathComponent.cast<DefaultMutableTreeNode>()?.userObject as? DatabaseNode ?: return
    pullFileAsync(
        project = project,
        device = dbNode.device,
        packageName = dbNode.packageName,
        remoteDirName = dbNode.databasesDir,
        remoteFileName = dbNode.databaseFileName,
        onFilePullSuccess = onFilePullSuccess,
        onFilePullError = onFilePullError,
    )
    super.doOKAction()
  }

  override fun dispose() {
    super.dispose()
  }

  private inner class PullFromDeviceTreeStructure(project: Project, invalidate: () -> Unit) : SimpleTreeStructure() {
    private val root = PullFromDeviceRootNode(project, invalidate)
    override fun getRootElement() = root
  }

  private inner class PullFromDeviceRootNode(project: Project, invalidate: () -> Unit) : RootDynamicNode(project, invalidate) {
    override fun computeChildren() {
      try {
        val connectedDevices = getConnectedDevices()
        if (connectedDevices.isEmpty()) {
          updateChild(EmptyNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDevices.empty")))
        } else {
          updateChildren(
              connectedDevices
                  .map { device ->
                    DeviceNode(project, this, device)
                  }
          )
        }
      } catch (e: Exception) {
        logw(e, "Could not list devices")
        updateChild(ErrorNode(
            ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDevices.error", e.message?.let { ": $it" } ?: "")
        ))
      }
    }
  }

  private inner class DeviceNode(project: Project, parent: DynamicNode, private val device: IDevice) : DynamicNode(project, parent) {
    init {
      myName = device.name
      icon = if (device.isEmulator) StudioIcons.DeviceExplorer.VIRTUAL_DEVICE_PHONE else StudioIcons.DeviceExplorer.PHYSICAL_DEVICE_PHONE
    }

    override fun computeChildren() {
      updateChildren(
          buildList {
            // Add running apps
            addAll(device.clients
                .filter { it.isValid }
                .sortedBy { it.clientData.packageName }
                .map { client ->
                  val packageName = client.clientData.packageName
                  val databasesDir = client.clientData.dataDir + "/databases"
                  PackageNode(project, this@DeviceNode, device, packageName, databasesDir)
                }
            )

            // Add other debuggable apps
            add(DebuggablePackagesNode(project, this@DeviceNode, device))
          }
      )
    }

    override fun isAutoExpandNode(): Boolean {
      return true
    }
  }

  private inner class DebuggablePackagesNode(
      project: Project,
      parent: DynamicNode,
      private val device: IDevice,
  ) : DynamicNode(project, parent) {
    init {
      myName = ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.title")
    }

    @SuppressLint("SdCardPath")
    override fun computeChildren() {
      device.getDebuggablePackageList().onFailure {
        updateChild(ErrorNode((ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.error"))))
      }.onSuccess {
        if (it.isEmpty()) {
          updateChild(EmptyNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.empty")))
        } else {
          updateChildren(
              it.map { packageName ->
                PackageNode(project, this, device, packageName, "/data/data/$packageName/databases")
              }
          )
        }
      }
    }
  }

  private inner class PackageNode(
      project: Project,
      parent: DynamicNode,
      private val device: IDevice,
      private val packageName: String,
      private val databasesDir: String,
  ) : DynamicNode(project, parent) {
    init {
      myName = packageName
      icon = ApolloIcons.Node.Package
    }

    override fun computeChildren() {
      device.getDatabaseList(packageName, databasesDir).onFailure {
        updateChild(ErrorNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDatabases.error")))
      }.onSuccess {
        if (it.isEmpty()) {
          updateChild(EmptyNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDatabases.empty")))
        } else {
          updateChildren(
              it
                  .map { databaseFileName ->
                    DatabaseNode(
                        device = device,
                        packageName = packageName,
                        databasesDir = databasesDir,
                        databaseFileName = databaseFileName,
                    )
                  }
          )
        }
      }
    }
  }

  private inner class DatabaseNode(
      val device: IDevice,
      val packageName: String,
      val databasesDir: String,
      val databaseFileName: String,
  ) : NullNode() {
    init {
      myName = databaseFileName
      icon = StudioIcons.DatabaseInspector.DATABASE
    }

    override fun handleDoubleClickOrEnter(tree: SimpleTree, inputEvent: InputEvent) {
      doOKAction()
    }
  }

  private class ErrorNode(message: String) : NullNode() {
    init {
      presentation.addText(message, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
  }

  private class EmptyNode(message: String) : NullNode() {
    init {
      presentation.addText(message, SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
  }
}
