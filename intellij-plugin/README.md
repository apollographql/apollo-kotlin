# Apollo IntelliJ Plugin

> Work in progress!

[![Version](https://img.shields.io/jetbrains/plugin/v/20645.svg)](https://plugins.jetbrains.com/plugin/20645)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20645.svg)](https://plugins.jetbrains.com/plugin/20645)

<!-- Plugin description -->

This plugin for Android Studio and IntelliJ helps you work with the
[Apollo Kotlin](https://github.com/apollographql/apollo-kotlin) GraphQL library.

Note: this plugin is still in early development and is not yet ready for production use.

### Features
- Automatic code generation: models are re-generated whenever GraphQL files change
- Integration with the [GraphQL IntelliJ Plugin](https://plugins.jetbrains.com/plugin/8097-js-graphql): the structure of the Apollo project is automatically contributed, so there is no need to create a `graphql.config.yml` / `.graphqlconfig` file
- Migration helpers:
  - Apollo Android 2.x → Apollo Kotlin 3.x
  - Apollo Kotlin 3.x → Apollo Kotlin 4.x (WIP)
  - `compat` codegen → `operationBased` codegen
- More to come!

<!-- Plugin description end -->

## Installation

The plugin is supported on:

* IntelliJ 2022.2 and above
* Android Studio 2022.2 (Flamingo) and above

#### Installation instructions:

<kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙</kbd>️ > <kbd>Manage Plugin
Repositories</kbd> > <kbd>+</kbd> > <kbd>https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/intellij-plugin/snapshots/plugins.xml</kbd>

Then:

<kbd>Marketplace</kbd> > <kbd>Search for "Apollo GraphQL"</kbd> > <kbd>Install</kbd>
