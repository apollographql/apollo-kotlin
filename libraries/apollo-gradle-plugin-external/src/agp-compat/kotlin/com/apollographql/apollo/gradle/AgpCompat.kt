package com.apollographql.apollo.gradle

@EmbeddedGradleSymbol
interface AgpCompat {
  fun compileSdk(): String?
  fun targetSdk(): Int?
  fun minSdk(): Int?

  fun onComponent(filter: ComponentFilter, block: (AgpComponent) -> Unit)

  val version: String
}

@EmbeddedGradleSymbol
enum class ComponentFilter {
  All,
  Test,
  Main
}

@EmbeddedGradleSymbol
interface AgpComponent {
  /**
   * The name of the component ("prodDebug", "demoRelease", etc..)
   */
  val name: String

  /**
   * The wrapped component, BaseVariant for AGP8, Component for
   */
  val wrappedComponent: Any
}