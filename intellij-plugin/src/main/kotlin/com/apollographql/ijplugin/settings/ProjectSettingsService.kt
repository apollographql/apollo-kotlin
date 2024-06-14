package com.apollographql.ijplugin.settings

import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.lang.jsgraphql.GraphQLSettings
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Transient
import java.util.UUID

/**
 * Project level settings.
 * These settings are not meant to be shared with the team, which is why they are stored in workspace.xml, typically ignored by VCS.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "com.apollographql.ijplugin.settings.ProjectSettingsState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ProjectSettingsService(private val project: Project) : PersistentStateComponent<ProjectSettingsStateImpl>, ProjectSettingsState {
  private val _state = ProjectSettingsStateImpl()

  override fun getState(): ProjectSettingsStateImpl {
    return _state
  }

  override fun loadState(state: ProjectSettingsStateImpl) {
    XmlSerializerUtil.copyBean(state, this._state)
    notifySettingsChanged()
  }

  override var automaticCodegenTriggering: Boolean
    get() = _state.automaticCodegenTriggering
    set(value) {
      _state.automaticCodegenTriggering = value
      notifySettingsChanged()
    }

  override var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean
    get() = _state.hasEnabledGraphQLPluginApolloKotlinSupport
    set(value) {
      _state.hasEnabledGraphQLPluginApolloKotlinSupport = value
    }

  override var contributeConfigurationToGraphqlPlugin: Boolean
    get() = _state.contributeConfigurationToGraphqlPlugin
    set(value) {
      _state.contributeConfigurationToGraphqlPlugin = value
      notifySettingsChanged()
    }

  override var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration>
    get() = _state.apolloKotlinServiceConfigurations
    set(value) {
      _state.apolloKotlinServiceConfigurations = value
      notifySettingsChanged()
    }

  override var telemetryInstanceId: String
    get() = _state.telemetryInstanceId
    set(value) {
      _state.telemetryInstanceId = value
    }

  override var apolloKotlinServices: List<ApolloKotlinService>
    get() = _state.apolloKotlinServices
    set(value) {
      _state.apolloKotlinServices = value
    }

  private var lastNotifiedState: ProjectSettingsState? = null

  private fun notifySettingsChanged() {
    if (lastNotifiedState != _state) {
      lastNotifiedState = _state.copy()
      project.messageBus.syncPublisher(ProjectSettingsListener.TOPIC).settingsChanged(_state)
    }
  }

  override fun initializeComponent() {
    // Automatically enable the "Frameworks / Apollo Kotlin" support in the GraphQL plugin's settings
    if (!hasEnabledGraphQLPluginApolloKotlinSupport) {
      project.service<GraphQLSettings>().isApolloKotlinSupportEnabled = true
      hasEnabledGraphQLPluginApolloKotlinSupport = true
    }

    if (telemetryInstanceId.isEmpty()) {
      telemetryInstanceId = UUID.randomUUID().toString()
    }
  }
}

interface ProjectSettingsState {
  var automaticCodegenTriggering: Boolean
  var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean
  var contributeConfigurationToGraphqlPlugin: Boolean
  var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration>
  var telemetryInstanceId: String

  /**
   * Cache of the ApolloKotlinServices constructed from the Gradle tooling models.
   * @see com.apollographql.ijplugin.gradle.GradleToolingModelService
   */
  var apolloKotlinServices: List<ApolloKotlinService>
}

/**
 * User configuration associated with an [ApolloKotlinService].
 */
data class ApolloKotlinServiceConfiguration(
    @Attribute
    val id: String = "",

    @Attribute
    val graphOsGraphName: String = "",
) {
  constructor(id: ApolloKotlinService.Id, graphOsGraphName: String) : this(id.toString(), graphOsGraphName)

  // API key is not stored as an attribute, but via PasswordSafe
  var graphOsApiKey: String?
    @Transient
    get() = PasswordSafe.instance.getPassword(credentialAttributesForService(id))
    @Transient
    set(value) {
      PasswordSafe.instance.setPassword(credentialAttributesForService(id), value)
    }

  val apolloKotlinServiceId: ApolloKotlinService.Id
    @Transient
    get() = ApolloKotlinService.Id.fromString(id)!!

  private fun credentialAttributesForService(id: String): CredentialAttributes {
    return CredentialAttributes(generateServiceName("Apollo/Service", id))
  }
}

data class ProjectSettingsStateImpl(
    override var automaticCodegenTriggering: Boolean = true,
    override var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean = false,
    override var contributeConfigurationToGraphqlPlugin: Boolean = true,
    override var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration> = emptyList(),
    override var telemetryInstanceId: String = "",
    override var apolloKotlinServices: List<ApolloKotlinService> = emptyList(),
) : ProjectSettingsState


val Project.projectSettingsState get(): ProjectSettingsState = service<ProjectSettingsService>()
