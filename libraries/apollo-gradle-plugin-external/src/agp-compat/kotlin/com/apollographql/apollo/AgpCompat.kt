package com.apollographql.apollo

internal interface AgpCompat {
  fun compileSdk(): String?
  fun targetSdk(): Int?
  fun minSdk(): Int?

  fun onComponent(filter: ComponentFilter, block: (AgpComponent) -> Unit)

  val version: String
}

internal enum class ComponentFilter {
  All,
  Test,
  Main
}

internal interface AgpComponent {
  /**
   * The name of the component ("prodDebug", "demoRelease", etc..)
   */
  val name: String

  /**
   * The wrapped component, BaseVariant for AGP8, Component for
   */
  val wrappedComponent: Any
}