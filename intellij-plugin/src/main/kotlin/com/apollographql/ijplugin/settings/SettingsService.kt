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
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Transient

@Service(Service.Level.PROJECT)
@State(
    name = "com.apollographql.ijplugin.settings.SettingsState",
    storages = [Storage("apollo.xml")]
)
class SettingsService(private val project: Project) : PersistentStateComponent<SettingsStateImpl>, SettingsState {
  private val _state = SettingsStateImpl()

  override fun getState(): SettingsStateImpl {
    return _state
  }

  override fun loadState(state: SettingsStateImpl) {
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

  private var lastNotifiedSettingsState: SettingsState? = null
  private fun notifySettingsChanged() {
    if (lastNotifiedSettingsState != _state) {
      lastNotifiedSettingsState = _state.copy()
      project.messageBus.syncPublisher(SettingsListener.TOPIC).settingsChanged(_state)
    }
  }

  init {
    // Automatically enable the "Frameworks / Apollo Kotlin" support in the GraphQL plugin's settings
    if (!hasEnabledGraphQLPluginApolloKotlinSupport) {
      project.service<GraphQLSettings>().setApolloKotlinSupportEnabled(true)
      hasEnabledGraphQLPluginApolloKotlinSupport = true
    }
  }
}

interface SettingsState {
  var automaticCodegenTriggering: Boolean
  var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean
  var contributeConfigurationToGraphqlPlugin: Boolean
  var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration>
}

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

data class SettingsStateImpl(
    override var automaticCodegenTriggering: Boolean = true,
    override var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean = false,
    override var contributeConfigurationToGraphqlPlugin: Boolean = true,
    override var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration> = emptyList(),
) : SettingsState


val Project.settingsState get(): SettingsState = service<SettingsService>()
