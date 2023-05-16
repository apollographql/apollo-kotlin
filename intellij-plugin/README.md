# Apollo IntelliJ Plugin

[![Version](https://img.shields.io/jetbrains/plugin/v/20645.svg)](https://plugins.jetbrains.com/plugin/20645)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20645.svg)](https://plugins.jetbrains.com/plugin/20645)

<!-- Plugin description -->

This plugin for Android Studio and IntelliJ helps you work with the
[Apollo Kotlin](https://github.com/apollographql/apollo-kotlin) GraphQL library.

## Features
- Automatic code generation: models are re-generated whenever GraphQL files change
- Integration with the [GraphQL IntelliJ Plugin](https://plugins.jetbrains.com/plugin/8097-js-graphql): the structure of the Apollo project is automatically contributed, so there is no need to create a `graphql.config.yml` / `.graphqlconfig` file
- Navigation from Kotlin code to GraphQL definitions
- Migration helpers:
  - Apollo Android 2.x → Apollo Kotlin 3.x
  - Apollo Kotlin 3.x → Apollo Kotlin 4.x (WIP)
  - `compat` codegen → `operationBased` codegen
- More to come!

## Compatibility

- Note: this plugin is currently under development
- Most features are intended to work with Apollo Kotlin 4.x which is currently under development
- Automatic code generation works with Apollo Kotlin 3.x and above

<!-- Plugin description end -->

The plugin is supported on:

- IntelliJ 2022.2 and above
- Android Studio 2022.2 (Flamingo) and above

## Installation instructions

<kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙</kbd>️ > <kbd>Manage Plugin
Repositories</kbd> > <kbd>+</kbd> > <kbd>https://plugins.jetbrains.com/plugins/preview/20645</kbd>

<img src="assets/instructions-1-manage-repositories.png" width="600" />

<img src="assets/instructions-2-add-repository.png" width="600" />

Then:

<kbd>Marketplace</kbd> > Search for "Apollo GraphQL" > <kbd>Install</kbd>

<img src="assets/instructions-3-search-and-install.png" width="600" />

### Snapshots

Latest development changes are available in a specific **snapshots** repository. To use it, follow the instructions
above with this repository URL
instead: <kbd>https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/intellij-plugin/snapshots/plugins.xml</kbd>.
