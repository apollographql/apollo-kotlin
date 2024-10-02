package com.apollographql.ijplugin.normalizedcache

import android.annotation.SuppressLint
import com.android.ddmlib.Client
import com.android.ddmlib.IDevice
import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.apollodebugserver.ApolloDebugClient
import com.apollographql.ijplugin.apollodebugserver.ApolloDebugClient.Companion.getApolloDebugClients
import com.apollographql.ijplugin.apollodebugserver.GetApolloClientsQuery
import com.apollographql.ijplugin.apollodebugserver.normalizedCacheSimpleName
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
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.AutoExpandSimpleNodeListener
import com.intellij.ui.treeStructure.NullNode
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.util.ui.tree.TreeUtil
import icons.StudioIcons
import kotlinx.coroutines.runBlocking
import java.awt.event.InputEvent
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeSelectionModel

class PullFromDeviceDialog(
    private val project: Project,
    private val onSourceSelected: (normalizedCacheSource: NormalizedCacheSource) -> Unit,
) : DialogWrapper(project, true), Disposable {
  private lateinit var tree: SimpleTree
  private lateinit var model: StructureTreeModel<PullFromDeviceTreeStructure>

  private val apolloDebugClientsToClose = mutableListOf<ApolloDebugClient>()

  init {
    title = ApolloBundle.message("normalizedCacheViewer.pullFromDevice.title")
    init()
    okAction.isEnabled = false
  }

  override fun createCenterPanel(): DialogPanel = panel {
    row {
      scrollCell(createTree())
          .align(AlignX.FILL)
    }

  }.withPreferredWidth(450)

  override fun getDimensionServiceKey(): String? {
    return PullFromDeviceDialog::class.java.simpleName
  }

  private fun createTree(): SimpleTree {
    tree = object : SimpleTree() {
      override fun configureUiHelper(helper: TreeUIHelper?) {
        TreeSpeedSearch(this).apply {
          setCanExpand(true)
        }
      }
    }.apply {
      emptyText.setText(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.loading"))
      selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
      isRootVisible = false
      showsRootHandles = true
      isLargeModel = true
      TreeUtil.installActions(this)

      model = createModel(this)

      addTreeExpansionListener(object : TreeExpansionListener {
        override fun treeExpanded(event: TreeExpansionEvent) {
          event.path.lastPathComponent.cast<DefaultMutableTreeNode>()?.userObject?.cast<DynamicNode>()?.onExpanded()
        }

        override fun treeCollapsed(event: TreeExpansionEvent) {}
      })

      addTreeSelectionListener {
        val selectedNode = it.path.lastPathComponent.cast<DefaultMutableTreeNode>()?.userObject
        okAction.isEnabled = selectedNode is DatabaseNode || selectedNode is ApolloDebugNormalizedCacheNode
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
    when (val selectedNode = tree.selectionPath?.lastPathComponent.cast<DefaultMutableTreeNode>()?.userObject) {
      is DatabaseNode -> {
        onSourceSelected(
            NormalizedCacheSource.DeviceFile(
                device = selectedNode.device,
                packageName = selectedNode.packageName,
                remoteDirName = selectedNode.databasesDir,
                remoteFileName = selectedNode.databaseFileName,
            )
        )
      }

      is ApolloDebugNormalizedCacheNode -> {
        // Don't close the apolloClient, it will be closed later by the caller
        apolloDebugClientsToClose.remove(selectedNode.apolloDebugClient)
        onSourceSelected(
            NormalizedCacheSource.ApolloDebugServer(
                apolloDebugClient = selectedNode.apolloDebugClient,
                apolloClientId = selectedNode.apolloClient.id,
                normalizedCacheId = selectedNode.normalizedCache.id
            )
        )
      }
    }
    super.doOKAction()
  }

  override fun dispose() {
    super.dispose()
    apolloDebugClientsToClose.forEach { runCatching { it.close() } }
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
      val apolloDebugClients: List<ApolloDebugClient> = device.getApolloDebugClients().getOrDefault(emptyList())
      apolloDebugClientsToClose.addAll(apolloDebugClients)
      val clients: List<Client> = device.clients
          .filter { client ->
            client.isValid &&
                client.clientData.packageName != null &&
                // If a package has the Apollo Debug running, don't show it as a database package
                client.clientData.packageName !in apolloDebugClients.map { it.packageName }
          }
          .sortedBy { it.clientData.packageName }

      val allClients = (apolloDebugClients + clients).sortedBy {
        when (it) {
          is ApolloDebugClient -> it.packageName
          is Client -> it.clientData.packageName
          else -> throw IllegalStateException()
        }
      }

      updateChildren(
          buildList {
            val autoExpand = allClients.size <= 4

            // Add running apps
            addAll(
                allClients.map { client ->
                  when (client) {
                    is ApolloDebugClient -> ApolloDebugPackageNode(
                        project = project,
                        parent = this@DeviceNode,
                        apolloDebugClient = client,
                        computeChildrenOn = ComputeChildrenOn.INIT,
                        autoExpand = autoExpand,
                    )

                    is Client -> {
                      val packageName = client.clientData.packageName
                      val databasesDir = client.clientData.dataDir + "/databases"
                      DatabasePackageNode(
                          project = project,
                          parent = this@DeviceNode,
                          device = device,
                          packageName = packageName,
                          databasesDir = databasesDir,
                          computeChildrenOn = ComputeChildrenOn.INIT,
                          autoExpand = autoExpand,
                      )
                    }

                    else -> throw IllegalStateException()
                  }
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
        logw(it, "Could not list debuggable packages")
        updateChild(ErrorNode((ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.error"))))
      }.onSuccess {
        if (it.isEmpty()) {
          updateChild(EmptyNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listDebuggablePackages.empty")))
        } else {
          updateChildren(
              it.map { packageName ->
                DatabasePackageNode(
                    project = project,
                    parent = this,
                    device = device,
                    packageName = packageName,
                    databasesDir = "/data/data/$packageName/databases",
                    computeChildrenOn = ComputeChildrenOn.EXPANDED,
                    autoExpand = false,
                )
              }
          )
        }
      }
    }
  }

  private inner class DatabasePackageNode(
      project: Project,
      parent: DynamicNode,
      private val device: IDevice,
      private val packageName: String,
      private val databasesDir: String,
      computeChildrenOn: ComputeChildrenOn,
      private val autoExpand: Boolean,
  ) : DynamicNode(project, parent, computeChildrenOn) {
    init {
      myName = packageName
      icon = ApolloIcons.Node.Package
    }

    override fun computeChildren() {
      device.getDatabaseList(packageName, databasesDir).onFailure {
        logw(it, "Could not list databases")
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

    override fun isAutoExpandNode(): Boolean {
      return autoExpand
    }
  }

  private inner class ApolloDebugPackageNode(
      project: Project,
      parent: DynamicNode,
      private val apolloDebugClient: ApolloDebugClient,
      computeChildrenOn: ComputeChildrenOn,
      private val autoExpand: Boolean,
  ) : DynamicNode(project, parent, computeChildrenOn) {
    init {
      myName = apolloDebugClient.packageName
      icon = ApolloIcons.Node.Package
    }

    override fun computeChildren() {
      runBlocking { apolloDebugClient.getApolloClients() }.onFailure {
        logw(it, "Could not list Apollo clients")
        updateChild(ErrorNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listApolloClients.error")))
      }.onSuccess { apolloClients ->
        if (apolloClients.isEmpty()) {
          updateChild(EmptyNode(ApolloBundle.message("normalizedCacheViewer.pullFromDevice.listApolloClients.empty")))
        } else {
          val showClientName = apolloClients.size > 1
          updateChildren(
              apolloClients
                  .flatMap { apolloClient -> apolloClient.normalizedCaches.map { normalizedCache -> apolloClient to normalizedCache } }
                  .filter { (_, normalizedCacheInfo) -> normalizedCacheInfo.recordCount != 0 }
                  .map { (apolloClient, normalizedCache) ->
                    ApolloDebugNormalizedCacheNode(
                        apolloDebugClient = apolloDebugClient,
                        apolloClient = apolloClient,
                        normalizedCache = normalizedCache,
                        showClientName = showClientName,
                    )
                  }
          )
        }
      }
    }

    override fun isAutoExpandNode(): Boolean {
      return autoExpand
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

  private inner class ApolloDebugNormalizedCacheNode(
      val apolloDebugClient: ApolloDebugClient,
      val apolloClient: GetApolloClientsQuery.ApolloClient,
      val normalizedCache: GetApolloClientsQuery.NormalizedCach,
      showClientName: Boolean,
  ) : NullNode() {
    init {
      myName = if (showClientName) {
        "${apolloClient.displayName} - ${normalizedCache.displayName.normalizedCacheSimpleName}"
      } else {
        normalizedCache.displayName.normalizedCacheSimpleName
      }
      presentation.locationString = ApolloBundle.message("normalizedCacheViewer.pullFromDevice.apolloDebugNormalizedCache.records", normalizedCache.recordCount)
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
