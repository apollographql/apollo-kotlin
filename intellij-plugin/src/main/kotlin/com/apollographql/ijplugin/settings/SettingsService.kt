package com.apollographql.ijplugin.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.lang.jsgraphql.GraphQLSettings
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute

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

  override var serviceConfigurations: List<ServiceConfiguration>
    get() = _state.serviceConfigurations
    set(value) {
      _state.serviceConfigurations = value
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
  var serviceConfigurations: List<ServiceConfiguration>
}

data class ServiceConfiguration(
    @Attribute
    val serviceName: String = "",
) {
  // API key is not stored as an attribute, but via PasswordSafe
  val apiKey: String?
    get() = PasswordSafe.instance.getPassword(credentialAttributesForService(serviceName))
}

data class SettingsStateImpl(
    override var automaticCodegenTriggering: Boolean = true,
    override var hasEnabledGraphQLPluginApolloKotlinSupport: Boolean = false,
    override var contributeConfigurationToGraphqlPlugin: Boolean = true,
    override var serviceConfigurations: List<ServiceConfiguration> = emptyList(),
) : SettingsState

fun credentialAttributesForService(serviceName: String): CredentialAttributes {
  return CredentialAttributes(generateServiceName("Apollo/Service", serviceName))
}


val Project.settingsState get(): SettingsState = service<SettingsService>()
