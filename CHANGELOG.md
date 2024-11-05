Change Log
==========

# Version 4.1.0

_2024-11-04_

## Ktor 3.0.0

Version 4.1.0 updates usages of Ktor from 2.3.11 to 3.0.0:

* If you are using `apollo-runtime-js` or `apollo-debug-server-jvm`, you need to update your app to Ktor 3.0.0+ at the same time as updating to Apollo 4.1.0 (`apollo-debug-server-android` is unaffected).
* If you are using the deprecated `apollo-mockserver` or `apollo-ktor-support` from this repo, you need to update to the [new coordinates](https://www.apollographql.com/docs/kotlin/migration/4.0#moved-artifacts).

All other cases are unaffected. In particular, `apollo-runtime` on Android and iOS uses OkHttp and NsUrlConnection respectively and is not impacted by the Ktor update.

You can read more details in the [pull request](https://github.com/apollographql/apollo-kotlin/pull/6214).

### New media type: `application/graphql-response+json`

`application/graphql-response+json` is a new media type being introduced by the [GraphQL over HTTP draft](https://graphql.github.io/graphql-over-http/draft/). It allows differentiating a valid GraphQL response from an error JSON response that could be transmitted by a cache or proxy in the HTTP chain. 

If your server uses `application/graphql-response+json` and returns non-2xx response, Apollo Kotlin will now parse those responses and expose `data` and `errors` instead of returning an `ApolloHttpException` before.

### K2 support for the IntelliJ plugin

The IntelliJ plugin is now compatible with K2 (#6150)

## 👷‍♂️ All changes

* [all] Update kotlinpoet to 2.0.0 (#6215)
* [all] Update to Ktor 3 (#6214)
* [all] Remove `apollo-mockserver` and `apollo-ktor-support` (#6215)
* [all] Remove mockserver as a dependency of apollo-testing-support (#6218)
* [ast] Do not escape slashes in single quoted strings (#6190)
* [runtime] Add support for application/graphql-response+json (#6170)
* [runtime] Do not call experimental webSocket() method re-entrently (#6197)
* [debug server] Remove Ktor dependency from apollo-debug-server JVM (#6224)
* [codegen] Do not add internal to private members (#6213)
* [codegen] Fix name clash in data builder names when two types differ only by their case (#6195)
* [gradle plugin] Allow null in KSP arguments for Apollo Compiler Plugins (#6200)
* [gradle plugin] Do not log the full introspection JSON (#6199)
* [gradle plugin] Deprecate TargetLanguage.KOTLIN_1_5 (#6193)
* [IJ Plugin] Make the cache viewer understand the blob db format (#6187)
* [IJ Plugin] Bump IJ Platform Gradle Plugin to 2.0.1 (#6185)
* [IJ Plugin] Migrate to the K2 compatible Analysis API (#6150)
* [IJ Plugin] Schedule the GraphQL configuration reload early (#6228)
* [IJ Plugin] Rename related generated code when renaming an Operation/Fragment (#6227)
* [IJ Plugin] Only highlight the name of unused operations, rather than the whole operation (#6226)
# Version 4.0.1

_2024-10-01_

This release contains a handful of bug fixes and improvements. 

## ⚙️ Add `ApolloCompilerPlugin.schemaListener()`

The [compiler plugins](https://www.apollographql.com/docs/kotlin/advanced/compiler-plugins) API has been extended to allow listening to schema changes.\
This can be used to have plugins generate code based on the schema. To do this, implement the [`schemaListener`](https://www.apollographql.com/docs/kotlin/kdoc/apollo-compiler/com.apollographql.apollo.compiler/-apollo-compiler-plugin/schema-listener.html)
function on your plugin:
    
```kotlin
class MyCompilerPlugin() : ApolloCompilerPlugin {
  @ApolloExperimental
  override fun schemaListener(): SchemaListener {
    return object : SchemaListener {
      override fun onSchema(schema: Schema, outputDirectory: File) {
        // Generate some code inside outputDirectory based on schema
      }
    }
  }
}
```

## 🚀 Allow mapping scalars to simple generic types

Previously, to [map a scalar](https://www.apollographql.com/docs/kotlin/essentials/custom-scalars) to a generic type, you had to use a `typealias`. 
Now, simple generic types are accepted, and common types like `List` and `String` don't need to be fully qualified:

```kotlin
mapScalar("MassList", "List<com.example.Measure<com.example.Mass>>", "com.example.MassListAdapter")
```

## 💙 Contributors

Many thanks to @ebrattli, @agrosner and @cvb941 for their contributions to this release 💙!

## 👷 All changes

* [gradle-plugin] Deprecate `"operationOutput"` and `./gradlew downloadApolloSchema` (#6097)
* [gradle-plugin] Use `registerJavaGeneratingTask` (#6149)
* [apollo-ast] Add GQLDocument.validate(SchemaValidationOptions) to allow adding external schemas. (#6164)
* [compiler] Add ApolloCompilerPlugin.schemaDocumentListener() (#6165)
* [compiler] Pass schema to ApolloCompilerPlugin.schemaListener (#6166)
* [compiler] Avoid enum value clashing with the getter `field` (#6093)
* [compiler] Fix a few additional instances of %L used instead of %N (#6117)
* [compiler] Escape properties in input builder function body (#6116)
* [compiler] Provide a more descriptive error message when a resolution of a specific `ResolverKey` fails (#6136)
* [compiler] Deprecate `@nonnull` (#6152)
* [compiler] Allow mapping scalars to simple generic types (#6158)
* [tooling] Allow to shutdown SchemaDownloader (#6091)
* [tooling] Remove warning (#6092)
* [WebSockets] connectionParams -> connectionPayload (#6103)
* [WebSockets] add NetworkTransport.closeConnection() (#6105)
* [BREAKING][runtime] Change NetworkMonitor to expose a StateFlow directly (#6119)
* [runtime] Add 'OfflineException' as a cause when using failFastIfOffline (#6104)
* [apollo-api] Fix reading `JsonNumber` in `MapJsonReader` (#6141)
* [cache] Allow to store `JsonNumber` in `Record` (#6139)
* [cache] Fix cascade SQL cache remove loops forever on cyclic references (#6137)
* [IJ Plugin] Bump pluginUntilBuild to 242 and pluginSinceBuild to 241 (#6111)
* [IJ Plugin] Add ApolloOneOfGraphQLViolationInspection (#6125)
* [IJ Plugin] Improve performance of ApolloGraphQLConfigFilePresentInspection and ApolloGraphQLConfigFilePresentAnnotator (#6126)
* [IJ Plugin] Fix high latency field inspection (#6142)
* [IJ Plugin] Correctly name Open In items (#6151)
* [infra] Bump Kotlin to 2.0.10 (#6107)
* [infra] Bump Kotlin to 2.0.20 (#6131)
* [infra] Bump develocity (#6128)
* [infra] Update Apollo Execution (#6138)
* [infra] Bump develocity (#6144)
* [infra] Allow compiling the project with Java22 (#6145)

# Version 4.0.0

_2024-07-29_

Apollo Kotlin 4 is a maturity release. It focuses on tooling, stability and making the library more maintainable, so it can evolve smoothly for the many years to come.

While most of the core APIs stayed the same, Apollo Kotlin 4 contains a few binary breaking changes. To account for that, and in order to be more future-proof, we changed the package name to `com.apollographql.apollo`.

You'll need to replace all the `com.apollographql.apollo3` with `com.apollographql.apollo` in your codebase.

* Migration guide: https://www.apollographql.com/docs/kotlin/v4/migration/4.0.
* Evolution policy: https://www.apollographql.com/docs/kotlin/v4/essentials/evolution.
* Blog post: coming soon.

Thanks again to everyone who provided feedback during the alphas/betas.

### Main changes

* New package name ([migration guide](https://www.apollographql.com/docs/kotlin/v4/migration/4.0))
* Moved artifacts ([Apollo Kotlin galaxy documentation page](https://www.apollographql.com/docs/kotlin/v4/advanced/galaxy))
* Improved error handling ([migration guide](https://www.apollographql.com/docs/kotlin/v4/migration/4.0#fetch-errors-do-not-throw))
* Android Studio/IntelliJ Plugin ([installation instructions](https://www.apollographql.com/docs/kotlin/v4/testing/android-studio-plugin))
* Multiplatform GraphQL parser ([doc](https://www.apollographql.com/docs/kotlin/v4/advanced/apollo-ast))
* Apollo Compiler Plugin API ([doc](https://www.apollographql.com/docs/kotlin/v4/advanced/compiler-plugins))
* WasmJs support
* NetworkMonitor API ([doc](https://www.apollographql.com/docs/kotlin/v4/advanced/network-connectivity))
* Experimental WebSocket API ([doc](https://www.apollographql.com/docs/kotlin/v4/advanced/experimental-websockets))
* Experimental support for `@oneOf` ([GraphQL RFC](https://github.com/graphql/graphql-spec/pull/825))
* Experimental support for `@semanticNonNull` and `@catch` ([doc](https://www.apollographql.com/docs/kotlin/v4/advanced/nullability))

### Changes against 3.8.5

* [#5984](https://github.com/apollographql/apollo-kotlin/pull/5984) - Add doNotStoreOnDisk()
* [#5982](https://github.com/apollographql/apollo-kotlin/pull/5982) - RetryOnNetworkErrorInterceptor should be configurable
* [#5971](https://github.com/apollographql/apollo-kotlin/pull/5971) - writeOperation/writeFragment no longer publish updates by default
* [#5966](https://github.com/apollographql/apollo-kotlin/pull/5966) - Move apollo-execution out of the main repo
* [#5952](https://github.com/apollographql/apollo-kotlin/pull/5952) - [IJ Plugin] Cache GraphQL project config
* [#5946](https://github.com/apollographql/apollo-kotlin/pull/5946) - Sub-protocol not included when opening websocket [4.0.0-beta6]
* [#5933](https://github.com/apollographql/apollo-kotlin/pull/5933) - [IJ/AS plugin] Internal error: NullPointerException
* [#5929](https://github.com/apollographql/apollo-kotlin/pull/5929) - Unnecessary AndroidX Multidex library included
* [#5922](https://github.com/apollographql/apollo-kotlin/pull/5922) - Apply com.apollographql.apollo3 plugin will break the dependencies in Kotlin Multiplatform according to IDEA
* [#5917](https://github.com/apollographql/apollo-kotlin/pull/5917) - Enabling apollo metadata generation for multi-module codegen causes build cache misses
* [#5901](https://github.com/apollographql/apollo-kotlin/pull/5901) - A request with any enum having rawValue = null hangs forever
* [#5899](https://github.com/apollographql/apollo-kotlin/pull/5899) - [IJ Plugin] UI for 'Go to declaration' is too wide
* [#5896](https://github.com/apollographql/apollo-kotlin/pull/5896) - [IJ Plugin] Cache viewer icon has wrong color in "new ui" theme
* [#5887](https://github.com/apollographql/apollo-kotlin/pull/5887) - [IJ Plugin] Warn when .graphqlrc files are present
* [#5885](https://github.com/apollographql/apollo-kotlin/pull/5885) - Cronet request lifecycle not behaving correctly with Apollo.
* [#5884](https://github.com/apollographql/apollo-kotlin/pull/5884) - [IJ/AS plugin] Internal error: NullPointerException
* [#5834](https://github.com/apollographql/apollo-kotlin/pull/5834) - SQL cache is unusably slow
* [#5833](https://github.com/apollographql/apollo-kotlin/pull/5833) - Make apollo-mockserver a separate repository
* [#5832](https://github.com/apollographql/apollo-kotlin/pull/5832) - MegaIssue: Independant versioning
* [#5827](https://github.com/apollographql/apollo-kotlin/pull/5827) - [IJ Plugin] Incorrect error when repeating "@semanticNonNullField"
* [#5819](https://github.com/apollographql/apollo-kotlin/pull/5819) - "Could not read normalized cache" in AS plugin
* [#5808](https://github.com/apollographql/apollo-kotlin/pull/5808) - Support for tree shaking in the __Schema.possibleTypes()
* [#5801](https://github.com/apollographql/apollo-kotlin/pull/5801) - ClassCastException is thrown when building an ApolloClient using a builder in KotlinJS
* [#5799](https://github.com/apollographql/apollo-kotlin/pull/5799) - isFromCache is potentially confusing
* [#5796](https://github.com/apollographql/apollo-kotlin/pull/5796) - Make it impossible to pass as input some type which was generated only to preserve forwards compatibility but was not meant to be used as input
* [#5795](https://github.com/apollographql/apollo-kotlin/pull/5795) - WasmJs support for apollo-adapters
* [#5781](https://github.com/apollographql/apollo-kotlin/pull/5781) - Codegen: rework how compiled field arguments are generated
* [#5777](https://github.com/apollographql/apollo-kotlin/pull/5777) - ApolloClient.Builder.okHttpClient() returns null instead of this
* [#5775](https://github.com/apollographql/apollo-kotlin/pull/5775) - Lazy version of okHttpCallFactory?
* [#5771](https://github.com/apollographql/apollo-kotlin/pull/5771) - [IJ Plugin] Crash when navigating to GraphQL operation via margin marker
* [#5768](https://github.com/apollographql/apollo-kotlin/pull/5768) - [IJ/AS plugin] Internal error: NullPointerException
* [#5757](https://github.com/apollographql/apollo-kotlin/pull/5757) - ApolloParseException is wrapping SocketException, StreamResetException
* [#5753](https://github.com/apollographql/apollo-kotlin/pull/5753) - [IJ/AS plugin] Internal error: NullPointerException
* [#5745](https://github.com/apollographql/apollo-kotlin/pull/5745) - Run subscriptions like queries with IDE plugins
* [#5738](https://github.com/apollographql/apollo-kotlin/pull/5738) - [IJ Plugin] Tweak navigation from GraphQL to generated code
* [#5727](https://github.com/apollographql/apollo-kotlin/pull/5727) - [Intellij Plugin] Truncate "go to" data
* [#5723](https://github.com/apollographql/apollo-kotlin/pull/5723) - Remove sendApqExtensions and sendDocument from MutableExecutionOptions
* [#5715](https://github.com/apollographql/apollo-kotlin/pull/5715) - Make NetworkMonitor work without androidx.startup
* [#5714](https://github.com/apollographql/apollo-kotlin/pull/5714) - Upgrade IJ platform minVersion
* [#5713](https://github.com/apollographql/apollo-kotlin/pull/5713) - Make benchmarks a composite build
* [#5712](https://github.com/apollographql/apollo-kotlin/pull/5712) - generateServiceApolloSources task fails with nondescript NullPointerException when type extension references unknown key field
* [#5697](https://github.com/apollographql/apollo-kotlin/pull/5697) - Websocket won't reopen on iOS
* [#5667](https://github.com/apollographql/apollo-kotlin/pull/5667) - Experimental @defer support does not work with AutoPersistedQueryInterceptor
* [#5659](https://github.com/apollographql/apollo-kotlin/pull/5659) - Use StreamingNSURLSessionHttpEngine by default on Apple
* [#5648](https://github.com/apollographql/apollo-kotlin/pull/5648) - Megaissue: improvements to WebSockets
* [#5647](https://github.com/apollographql/apollo-kotlin/pull/5647) - [IJ Plugin] Try the new IntelliJ Platform Gradle Plugin 2.0
* [#5641](https://github.com/apollographql/apollo-kotlin/pull/5641) - Introspection is broken in the beta
* [#5616](https://github.com/apollographql/apollo-kotlin/pull/5616) - [IJ Plugin] Send telemetry only for projects using Apollo
* [#5575](https://github.com/apollographql/apollo-kotlin/pull/5575) - [IJ/AS plugin] Internal error: Throwable
* [#5568](https://github.com/apollographql/apollo-kotlin/pull/5568) - Disallow @typePolicy on unions
* [#5507](https://github.com/apollographql/apollo-kotlin/pull/5507) - 🧩 [IJ Plugin] Remove client only directives before sending the query to the server
* [#5500](https://github.com/apollographql/apollo-kotlin/pull/5500) - [IJ Plugin] v3 -> v4 Migration: add @link imports for used kotlin_labs directives
* [#5481](https://github.com/apollographql/apollo-kotlin/pull/5481) - [IJ/AS plugin] Internal error: ClassCastException
* [#5468](https://github.com/apollographql/apollo-kotlin/pull/5468) - Publish apollo-cli
* [#5455](https://github.com/apollographql/apollo-kotlin/pull/5455) - Build fails after schema file rename when Gradle configuration cache enabled
* [#5449](https://github.com/apollographql/apollo-kotlin/pull/5449) - [gradle-plugin] download{Service}SchemaFromIntrospection fails
* [#5431](https://github.com/apollographql/apollo-kotlin/pull/5431) - Support @oneOf for Input Objects
* [#5415](https://github.com/apollographql/apollo-kotlin/pull/5415) - Compiler plugin API + classloader isolation
* [#5413](https://github.com/apollographql/apollo-kotlin/pull/5413) - [IJ plugin] Automatically import certain directives
* [#5379](https://github.com/apollographql/apollo-kotlin/pull/5379) - Allow the cache viewer to sort items more "intelligently" when showing cache entries that have a number at the end
* [#5374](https://github.com/apollographql/apollo-kotlin/pull/5374) - [IJ Plugin] Inspection to warn when using input types constructors
* [#5372](https://github.com/apollographql/apollo-kotlin/pull/5372) - [IJ plugin] Normalized cache: reload button for file caches
* [#5345](https://github.com/apollographql/apollo-kotlin/pull/5345) - [RFC] Remove X-APOLLO-... custom headers
* [#5342](https://github.com/apollographql/apollo-kotlin/pull/5342) - [IJ/AS plugin] Internal error: IllegalArgumentException
* [#5338](https://github.com/apollographql/apollo-kotlin/pull/5338) - Unexpected behavior of @include directive on a fragment
* [#5337](https://github.com/apollographql/apollo-kotlin/pull/5337) - [RFC] Error handling -- @catch & partial data
* [#5331](https://github.com/apollographql/apollo-kotlin/pull/5331) - [IJ plugin] Quality of life improvements
* [#5329](https://github.com/apollographql/apollo-kotlin/pull/5329) - [IJ plugin] Sometimes db files are not in the list from "Pull from device"
* [#5312](https://github.com/apollographql/apollo-kotlin/pull/5312) - useV3ExceptionHandling should populate data even if errors are present
* [#5311](https://github.com/apollographql/apollo-kotlin/pull/5311) - Warnings are surfaced during build when fragments with params are used in queries
* [#5299](https://github.com/apollographql/apollo-kotlin/pull/5299) - [IJ/AS plugin] Cache viewer
* [#5266](https://github.com/apollographql/apollo-kotlin/pull/5266) - [IJ/AS plugin] Internal error: PluginException
* [#5261](https://github.com/apollographql/apollo-kotlin/pull/5261) - [IJ/AS plugin] Internal error: PluginException
* [#5241](https://github.com/apollographql/apollo-kotlin/pull/5241) - Apollo Parse Exception - failed to parse
* [#5239](https://github.com/apollographql/apollo-kotlin/pull/5239) - [IJ/AS plugin] Analytics
* [#5235](https://github.com/apollographql/apollo-kotlin/pull/5235) - [IJ/AS Plugin] Use JetBrains Marketplace for weekly snapshots instead of Repsy
* [#5233](https://github.com/apollographql/apollo-kotlin/pull/5233) - NullPointerException for Request with single-quote character
* [#5230](https://github.com/apollographql/apollo-kotlin/pull/5230) - useV3ExceptionHandling should not throw ApolloGraphQLException
* [#5224](https://github.com/apollographql/apollo-kotlin/pull/5224) - generateServiceApolloSources crashes if a fragment definition references itself
* [#5221](https://github.com/apollographql/apollo-kotlin/pull/5221) - Implicit task dependency not working
* [#5220](https://github.com/apollographql/apollo-kotlin/pull/5220) - [IDE Plugin] Sandbox Button Does Not Carry Over Fragments from other Modules
* [#5217](https://github.com/apollographql/apollo-kotlin/pull/5217) - Retrying a subscription does not renew the id and may cause an error on the server because the id is already used
* [#5213](https://github.com/apollographql/apollo-kotlin/pull/5213) - Gradle crash when @typePolicy defined for nonexistant field.
* [#5207](https://github.com/apollographql/apollo-kotlin/pull/5207) - Add options to not generate data classes
* [#5200](https://github.com/apollographql/apollo-kotlin/pull/5200) - Subscriptions: support SUBSCRIPTION_SCHEMA_RELOAD
* [#5186](https://github.com/apollographql/apollo-kotlin/pull/5186) - Cache: Variable defaultValues are not taken into account for cache keys
* [#5173](https://github.com/apollographql/apollo-kotlin/pull/5173) - [IJ/AS Plugin] Middle click on Fragment definition overrides GraphQL Go to usages.
* [#5172](https://github.com/apollographql/apollo-kotlin/pull/5172) - [IJ/AS plugin] Ignore id field in Unused field inspection
* [#5171](https://github.com/apollographql/apollo-kotlin/pull/5171) - Consider migrating generated code to use Enum.entries instead of Enum.values() for Kotlin 1.9 and onwards
* [#5159](https://github.com/apollographql/apollo-kotlin/pull/5159) - Increase the maximum JSON nesting level, or make it customisable
* [#5112](https://github.com/apollographql/apollo-kotlin/pull/5112) - :app:generateStorefrontApolloSources Variables used in the query are warned as unused.
* [#5066](https://github.com/apollographql/apollo-kotlin/pull/5066) - 🧩 [IJ/AS plugin] "Find unused fields" inspection
* [#5057](https://github.com/apollographql/apollo-kotlin/pull/5057) - [IJ plugin] Quick fix for expensive field: add @defer
* [#5040](https://github.com/apollographql/apollo-kotlin/pull/5040) - [IJ/AS plugin] Operation renaming improvement
* [#5039](https://github.com/apollographql/apollo-kotlin/pull/5039) - [IJ/AS plugin] Migration helper for the v4 multi-module syntax
* [#5035](https://github.com/apollographql/apollo-kotlin/pull/5035) - Android Studio Plugin - Navigate to Query Gutter Icon Missing
* [#5033](https://github.com/apollographql/apollo-kotlin/pull/5033) - [IJ plugin] "Go to declaration" doesn't offer graphql target when used on an import alias
* [#5028](https://github.com/apollographql/apollo-kotlin/pull/5028) - [IJ/AS plugin] Feature: Apollo Studio field insights
* [#5000](https://github.com/apollographql/apollo-kotlin/pull/5000) - Un-minimized query in comment has parse errors
* [#4977](https://github.com/apollographql/apollo-kotlin/pull/4977) - [IJ/AS plugin] Show errors when mixing .graphql / .graphqls file contents
* [#4942](https://github.com/apollographql/apollo-kotlin/pull/4942) - [IJ/AS plugin] Don't crash when ToolingModel method are not present
* [#4931](https://github.com/apollographql/apollo-kotlin/pull/4931) - False positives on unused(?) input fields, Apollo: Use of deprecated input field {{ name }}
* [#4925](https://github.com/apollographql/apollo-kotlin/pull/4925) - Task downloadFooApolloSchemaFromIntrospection fails to comply with configuration cache
* [#4921](https://github.com/apollographql/apollo-kotlin/pull/4921) - Support for AGP 8.2.0
* [#4920](https://github.com/apollographql/apollo-kotlin/pull/4920) - apollo-ast: support merging definitions without validation
* [#4919](https://github.com/apollographql/apollo-kotlin/pull/4919) - [IJ/AS plugin] Navigation from code to GQL definition
* [#4889](https://github.com/apollographql/apollo-kotlin/pull/4889) - [IJ/AS plugin] Support both AS stable and IJ stable
* [#4858](https://github.com/apollographql/apollo-kotlin/pull/4858) - Remove initRuntimeIfNeeded() in Project
* [#4805](https://github.com/apollographql/apollo-kotlin/pull/4805) - With more than one subscription and network off for long duration, webSocketReopenWhen can take unexpectedly long to establish connection when network is back on.
* [#4797](https://github.com/apollographql/apollo-kotlin/pull/4797) - Cannot use GQLDocument.toUtf8() on documents that include the extend keyword
* [#4784](https://github.com/apollographql/apollo-kotlin/pull/4784) - Fragments are skipped when the if condition is a variable with a false default value
* [#4775](https://github.com/apollographql/apollo-kotlin/pull/4775) - WebSockets: allow changing the serverUrl of WebSocketNetworkTransport
* [#4761](https://github.com/apollographql/apollo-kotlin/pull/4761) - IntelliJ plugin description, link to terms, etc.
* [#4760](https://github.com/apollographql/apollo-kotlin/pull/4760) - [IJ/AS plugin] Make the GQL plugin setting to handle Apollo directives always checked
* [#4759](https://github.com/apollographql/apollo-kotlin/pull/4759) - Automatic "compat" -> "operationBased" migration
* [#4747](https://github.com/apollographql/apollo-kotlin/pull/4747) - generateOptionalOperationVariables setting is ignored.
* [#4744](https://github.com/apollographql/apollo-kotlin/pull/4744) - Use Apollo Kotlin inside Apollo Kotlin
* [#4732](https://github.com/apollographql/apollo-kotlin/pull/4732) - External interfaces for JS interop
* [#4728](https://github.com/apollographql/apollo-kotlin/pull/4728) - Poor Performance of Kotlin/JS
* [#4711](https://github.com/apollographql/apollo-kotlin/pull/4711) - RFC: v4 error handling
* [#4710](https://github.com/apollographql/apollo-kotlin/pull/4710) - RFC: v4 package name
* [#4701](https://github.com/apollographql/apollo-kotlin/pull/4701) - Kotlin/Wasm
* [#4669](https://github.com/apollographql/apollo-kotlin/pull/4669) - Unit testing error resolving class
* [#4625](https://github.com/apollographql/apollo-kotlin/pull/4625) - [IJ/AS plugin] Support for multiple schemas according to configuration
* [#4623](https://github.com/apollographql/apollo-kotlin/pull/4623) - [IJ/AS plugin v3
* [#4622](https://github.com/apollographql/apollo-kotlin/pull/4622) - [IJ/AS plugin] Migration helpers v3
* [#4621](https://github.com/apollographql/apollo-kotlin/pull/4621) - [IJ/AS plugin] Automatic codegen invocation
* [#4620](https://github.com/apollographql/apollo-kotlin/pull/4620) - [IJ/AS plugin] Project setup
* [#4619](https://github.com/apollographql/apollo-kotlin/pull/4619) - [IJ/AS plugin] Initial release umbrella ticket
* [#4576](https://github.com/apollographql/apollo-kotlin/pull/4576) - Android (JAVA)Apollo subscription only onConnected() is called
* [#4574](https://github.com/apollographql/apollo-kotlin/pull/4574) - js client requests can fail due to inclusion of apollo specific headers
* [#4542](https://github.com/apollographql/apollo-kotlin/pull/4542) - Allow supplying something other than suspending functions to core builders
* [#4530](https://github.com/apollographql/apollo-kotlin/pull/4530) - The POM for com.apollographql.apollo3:apollo-rx3-support-java:jar:3.7.1 is missing, no dependency information available
* [#4519](https://github.com/apollographql/apollo-kotlin/pull/4519) - Update to SQLDelight 2
* [#4518](https://github.com/apollographql/apollo-kotlin/pull/4518) - Enforce validation of operation directives
* [#4516](https://github.com/apollographql/apollo-kotlin/pull/4516) - 🐘 Gradle config: mandate service name
* [#4504](https://github.com/apollographql/apollo-kotlin/pull/4504) - Allow custom SqlDriver to support multi-platform encryption
* [#4416](https://github.com/apollographql/apollo-kotlin/pull/4416) - enable configuration cache
* [#4350](https://github.com/apollographql/apollo-kotlin/pull/4350) - maven-publish doesn't play well with -apollo modules generated when generateApolloMetadata is used
* [#4325](https://github.com/apollographql/apollo-kotlin/pull/4325) - [Umbrella issue] modernize build system
* [#4283](https://github.com/apollographql/apollo-kotlin/pull/4283) - [Umbrella issue] Better Java support in Apollo Kotlin
* [#4205](https://github.com/apollographql/apollo-kotlin/pull/4205) - Validation for custom schema directives
* [#4171](https://github.com/apollographql/apollo-kotlin/pull/4171) - 🧹 4.0 cleanups
* [#4160](https://github.com/apollographql/apollo-kotlin/pull/4160) - 🐜 Multiplatform AST parser
* [#4150](https://github.com/apollographql/apollo-kotlin/pull/4150) - Deprecate dispose() methods and instead implement okio.Closeable
* [#4062](https://github.com/apollographql/apollo-kotlin/pull/4062) - Deprecate ApolloCompositeException with Error level and use suppressed exceptions instead
* [#4003](https://github.com/apollographql/apollo-kotlin/pull/4003) - ApolloCall execute / toFlow / exception handling improvements
* [#3890](https://github.com/apollographql/apollo-kotlin/pull/3890) - Passing executionContext to the platform engine
* [#3751](https://github.com/apollographql/apollo-kotlin/pull/3751) - Automatic "codegenModels" migration
* [#3733](https://github.com/apollographql/apollo-kotlin/pull/3733) - @nonull could have surprising effects in error cases
* [#3694](https://github.com/apollographql/apollo-kotlin/pull/3694) - [umbrella issue] Java Runtime
* [#3283](https://github.com/apollographql/apollo-kotlin/pull/3283) - Trigger a compilation error on name clashes
* [#3152](https://github.com/apollographql/apollo-kotlin/pull/3152) - Provide module information (JPMS)
* [#3143](https://github.com/apollographql/apollo-kotlin/pull/3143) - Implement toString for InputTypes
* [#2823](https://github.com/apollographql/apollo-kotlin/pull/2823) - Cache and connection restore for Subscription
* [#2783](https://github.com/apollographql/apollo-kotlin/pull/2783) - ApolloParseException caused by SocketTimeoutError while reading response body
* [#2765](https://github.com/apollographql/apollo-kotlin/pull/2765) - Subscription switched to DISCONNECTED
* [#2673](https://github.com/apollographql/apollo-kotlin/pull/2673) - [Compiler] Validate operation directives
* [#2520](https://github.com/apollographql/apollo-kotlin/pull/2520) - Feature request: Resilient Parsing
* [#2079](https://github.com/apollographql/apollo-kotlin/pull/2079) - Ktlint should not warn on generated Apollo code
* [#1692](https://github.com/apollographql/apollo-kotlin/pull/1692) - On the fly code generation
* [#650](https://github.com/apollographql/apollo-kotlin/pull/650) - Create Android Studio Plugin


# Version 4.0.0-rc.2

_2024-07-22_

We're on the road to v4 with this second release candidate which includes a few minor tweaks as well as one new feature.

Note: in v4.0.0-rc.1 `apollo-debug-server` and `apollo-tooling` depended on `com.apollographql.apollo3` artifacts. This is no longer the case with `v4.0.0-rc.2`,
where all dependencies are under the `com.apollographql.apollo` group id.

## Add memoryCacheOnly (#6049)

`.memoryCacheOnly(true)` can be set on calls to read and store records in the memory cache only even when a persistent cache is configured.
This can be useful in scenarios where long term storage isn't needed and performance is important.

* [gradle-plugin] Use a better warning for Service.schemaFile users (#6036)
* [all] Update apollo-kotlin-execution (#6040)
* [IJ Plugin] Add com.apollographql.apollo:apollo-api-jvm to dependencySupport (#6039)
* [all] Update to Gradle 8.9 (#6056)
* [gradle-plugin] Undeprecate schemaFile (#6055)
* [gradle-plugin] Disable service-loading `ApolloCompilerPlugin` (#6059)

# Version 3.8.5

_2024-07-10_

Version 3.8.5 is a maintenance release. New developments happen in the 4.x versions.

* [all] Bump okio to 3.9.0 (#5868)
* [runtime] Adding checks for json end_document in http transport (#5894)
* [runtime] Adding checks for json end_document in http batching interceptors (#5892)
* [codegen] Fix mapping of the builtin Float scalar type (#6047)
* [normalized-cache] SqlNormalizedCacheFactory make sqldriver public to support Sqlcipher data encryption. (#5972)

# Version 4.0.0-rc.1

_2024-07-08_

## New package name & evolution policy

We changed the package name from `com.apollographql.apollo3` to `com.apollographql.apollo` for version 4. This is a safe default for the many years to come and works well with our new [evolution policy](https://www.apollographql.com/docs/kotlin/v4/essentials/evolution).

If you are updating from version 3 or an alpha/beta version 4, you need to replace all `com.apollographql.apollo3` with `com.apollographql.apollo`

## Apollo galaxy

As part of this release, some of the non-core artifacts have been moved to separate coordinates and GitHub repositories. Moving forward, this will allow us to iterate faster on those artifacts while keeping the core ones more maintainable.

Some of the existing symbols are kept as deprecated to ease the transition (like `MockServer` for an example). Others (the `-incubating` ones) have been removed, and you need to update them now.

You can read more in the [migration guide](https://go.apollo.dev/ak-moved-artifacts).

## All changes

* [BREAKING][all] Change package name to `com.apollographql.apollo`
* [BREAKING][all] Remove incubating compose support (#5987)
* [BREAKING][all] Remove apollo-cli (#5986)
* [BREAKING][all] Remove incubating normalized cache (#5985)
* [BREAKING][all] Nullability: Move nullability directives to v0.4 (#6002)
* [BREAKING][all] Suffix ApolloStore write/publish overloads with `Sync` to avoid them taking precedence over their suspend counterparts (#5976)
* [NEW][cache] SqlNormalizedCacheFactory make sqldriver public to support Sqlcipher data encryption. (#5973)
* [NEW][runtime] Add ApolloClient.Builder.retryOnErrorInterceptor (#5989)
* [adapters] Deprecate apollo-adapters (#6012)
* [🐘gradle-plugin] Rename the multi-module configurations (#6027)
* [IJ Plugin] Tweak cache name display for Apollo 3, 4, and incubating (#6026)
* [compiler] remove unused argument to scalarAdapterInitializer() (#5996)
* [java] Use published version of the Java support libs (#5991)
* [runtime] Deprecate apollo engine ktor and publish engine tests (#5988)
* [mpp-utils] Cleanup mpp utils (#5980)
* [CI] use `gradle/actions/setup-gradle` instead of `gradle-build-action` (#5981)
* [infra] Update to develocity API (#5967)
* [incubating cache] Add a CacheKeyApolloResolver based on ApolloResolver (incubating) (#5970)
* [mockserver] Robustify TCP server (#5968)
* [runtime] adding checks for json end_document in http batching interceptors (#5893)
* [IJ plugin] Cache ApolloKotlinService into project settings (#5962)
* [IJ plugin] Avoid a ConcurrentModificationException occurring in conjunction to the IJ Platform Gradle plugin. (#5959)
* [websockets] Send Sec-WebSocket-Protocol (#5948)
* [mockserver] Deprecate com.apollographql.apollo3.mockserver.MockServer (#5943)

# Version 4.0.0-beta.7

_2024-06-05_

## K2

#5931: This version is built with K2. You will need Kotlin 1.9+ on the JVM to build it and Kotlin 2.0 for other platforms.

## ApolloCompilerPluginProvider

#5865: `ApolloCompilerPluginProvider` is introduced to allow passing arguments to compiler plugins. See the [compiler plugins documentation](https://go.apollo.dev/ak-compiler-plugins) for more details. 

## 👷‍ All changes
* [testing] Use com.apollographql.mockserver.MockServer (#5939)
* [testing] Simplify our implementation of runTest (#5935)
* [testing] Prepare apollo-testing-support to new MockServer (#5934)
* [runtime] Remove multidex library (#5930)
* [all] Bump languageVersion/apiVersion to Kotlin 2.0 (#5931)
* [codegen] fix mapping of the builtin Float type (#5928)
* [IJ Plugin] Add inspection to warn about the presence of a GraphQL config file (#5908)
* [codegen] Add a null-check to java enum safeValueOf (#5904)
* [gradle-plugin] Remove eager configuration (#5923)
* [gradle-plugin] sort input files (#5919)
* [IJ Plugin] Suppress GraphQLDuplicateDirective for certain directives (#5910)
* [adapters] Add KtorHttpUrlAdapter (#5915)
* [IJ Plugin] Add a "new UI" compatible icon (#5906)
* [IJ Plugin] Operation and fragment definition rename (#5912)
* [IJ Plugin] Add @link directives to extra.graphqls during v3->v4 migration (#5909)
* [IJ Plugin] Remove GraphQL IJ plugin issue workaround (#5907)
* [cache] Pagination: use "field key" instead of "field name" (#5898)
* [IJ Plugin] Improve the presentation of GraphQL elements when navigating to them (#5900)
* [IJ Plugin] Include subfolders when contributing GraphQL config (#5871)
* [runtime] Remove existing interceptors from ApolloClient.Builder before adding new ones (#5858)
* [codegen] Add ApolloCompilerPluginProvider  (#5865)
* [runtime] Clear current ApolloStore related interceptors when calling `.store()` on builder (#5857)
* [cache] Call through to loadRecords from the MemoryCache to the SQL one (#5848)
* [runtime] deprecate operationIdGenerator (#5850)

# Version 3.8.4

_2024-04-29_

Version 3.8.4 is a maintenance release with one bug fix and 2 performance improvements. New developments happen in the 4.x versions.

* [java-runtime] Fix `ApolloClient.Builder.okHttpClient()` returns null instead of this (#5860)
* [normalized-cache] Use a single transaction when using MemoryCache chaining (#5861)
* [normalized-cache] Call through to loadRecords from the MemoryCache to the SQL one (#5863)

# Version 4.0.0-beta.6

_2024-04-23_

## SQL cache performance improvements

If you're using a chained memory + SQL cache, #5840 makes sure cache writes are wrapped in a transaction, making them much faster.

## Apollo Compiler Plugins

`Plugin` is renamed to `ApolloCompilerPlugin`. There is a new `documentTransform` API as well as other fixes. More details in the [compiler plugins documentation](https://www.apollographql.com/docs/kotlin/v4/advanced/compiler-plugins).

## Experimental WebSockets

A new `.websocket` package is available that makes it easier to retry WebSockets and handle errors. More details and migration guide in the [experimental websockets documentation](https://www.apollographql.com/docs/kotlin/v4/advanced/experimental-websockets).

## ApolloIdlingResource is deprecated

We recommend using reactive patterns to test your UI instead. See [this article about ways to do so](https://medium.com/androiddevelopers/alternatives-to-idling-resources-in-compose-tests-8ae71f9fc473).

## Removed androidx.startup dependency

androidx.startup was introduced in beta.5 but is problematic for unit tests and other cases. beta.6 removes that dependency. More details in the [network connectivity documentation](https://www.apollographql.com/docs/kotlin/v4/advanced/network-connectivity).

##  WasmJS support for apollo-adapter

You can see Wasm in action at https://wasm.confetti-app.dev/ 

##  Threading changes

In a effort to minimize the number of thread switches, the whole request is now run in the same dispatcher. See [Threading.md](https://github.com/apollographql/apollo-kotlin/blob/39d76630277476004bbaed0e3a897feb5a959084/design-docs/Threading.md) for more details.

## Contributors 💙

Many thanks to @joreilly, @ychescale9 and @japhib for their contributions to this release 💙!

## 👷‍ All changes
* [normalized-cache]: use a single SQL transaction when using MemoryCache chaining (#5840)
* [compiler] expose apollo-ast as an api dependency (#5838)
* [compiler] Rename `Plugin` to `ApolloCompilerPlugin` and add error message for bad configurations (#5821)
* [IJ Plugin] Fix pulling file from device not working on AS Koala (#5822)
* [compiler] Add `@ApolloEnumConstructor` and make enum as sealed class Unknown constructor opt-in (#5813)
* [runtime] Move ApolloParseException to ApolloNetworkException (#5816)
* [normalized-cache] Let isFromCache be about the ApolloResponse (#5805)
* [compiler] Add DocumentTransform API (#5809)
* [idling-resource] Deprecate ApolloIdlingResource (#5817, #5764)
* [runtime] Share the default OkHttpBuilder (#5811)
* [runtime] [BREAKING] change `isOnline` to a suspend fun
* [adapters] Support Kotlin/Wasm for apollo-adapters (#5803)
* [all] Bump Kotlin to 2.0.0-RC1 (#5802)
* [Codegen] Add CompiledArgumentDefinition (#5797, #5837)
* [runtime] Merge experimental WebSocketNetworkTransport in apollo-runtime (#5790)
* [normalized-cache] Cache pagination: add FieldNameGenerator and EmbeddedFieldsProvider (#5772)
* [runtime] Support configuring `ApolloClient` with lazily initialized `Call.Factory`. (#5784)
* [runtime] fix ApolloClient.Builder.okHttpClient() returns null instead of this (#5778)
* [normalized-cache] Fix variable coercion in lists. Absent variables are coerced to null (#5773)
* [IJ Plugin] Fix an NPE (#5770)
* [runtime] Simplify ApolloCall (#5765)
* [runtime] remove `androidx.startup` dependency (#5761, #5720)
* [compiler] Bump kotlin_labs definitions to v0.3 (#5762)
* [Pagination] Support nodes in Connection types (#5754)
* [compiler] Directive validation is now enforced by default (#5758)
* [cache] Make ApolloStore.publish() suspend (#5755)
* [runtime] Change the dispatcher earlier in the chain (#4319)
* [IJ Plugin] Add an advanced setting to include generated code references in GraphQL "Go To Declaration" (#5743)
* [IJ Plugin] Fix presentation of Kotlin elements when navigating to them from GraphQL (#5739)
* [IJ Plugin] Consider all Gradle projects recursively (#5734)
* [runtime] Deprecate ApolloClient.Builder.addInterceptors() (#5733)
* [all] use jdk-release (#5731)
* [http-cache] Ignore IOException when calling ApolloHttpCache.remove (#5729)
* [IJ plugin] Bump platformVersion and pluginSinceBuild from 232 to 233 (#5726)
* [runtime] add ApolloClient.failFastIfOffline (#5725)
* [all] Introduce "filesystem" sourceSet and use okio 3.9.0 (#5719)
* [runtime] Do not use Ktor in Js HttpEngine, use fetch directly instead (#5702)

# Version 3.8.3

_2024-03-20_

Version 3.8.3 is a maintenance release with two new convenience APIs and a Kotlin update. New developments happen in the 4.x versions.

Note: because Apollo Kotlin now depends on kotlin-stdlib version 1.9, you need the 1.8+ Kotlin compiler to compile your project.

* [all] update Kotlin to 1.9 (#5412)
* [runtime] Add `ApolloClient.Builder(ApolloHttpCache)` (#5638) (#5640)
* [runtime] Allow `buildPostBody` to write operation extensions (#5631)
* [runtime] compose support: Catch exceptions and expose them in `.exception` (#5018)
* [http-cache] Ignore `IOException` when calling `ApolloHttpCache.remove` (#5730)
* [all] Add deprecations on symbols that are getting removed in v4 (#5746)

# Version 4.0.0-beta.5

_2024-03-12_

Main changes:

* **Apollo compiler plugins**: The GraphQL compiler now has APIs that you can use to customize the generated code. This can be used for changing visibility of some symbols, renaming them or more generally customizing the output for any advanced use cases. Moving forward, Apollo compiler plugins are the preferred way to customize operation IDs as well as package names and both `PackageNameGenerator` and `OperationOutputGenerator` are deprecated. See the [documentation page about Apollo compiler plugins](https://www.apollographql.com/docs/kotlin/v4/advanced/compiler-plugins) for more details.
* **Reduced lock contention in apollo-normalized-cache-incubating**: the incubating normalized cache now uses lock-free memory structures inspired by guava and [MobileNativeFoundation/Store](https://github.com/MobileNativeFoundation/Store/). We have seen improvements by up to 20% in some scenarios. Please share your numbers if you notice any positive (or negative) change. 
* **Nullability directives**: The version of the supported nullability directives was bumped from 0.1 to 0.3 (See [apollographql/specs#42](https://github.com/apollographql/specs/pull/42) and [apollographql/specs#48](https://github.com/apollographql/specs/pull/48)). If you are using `@semanticNonNull` or `@catch` you should bump your `@link` directives to use `0.3`. See the [nullability documentation page](https://www.apollographql.com/docs/kotlin/v4/advanced/nullability) for more details.
* **New snapshot repository for the IntelliJ/Android Studio plugin**: The repository to use for the weekly snapshots has changed. You can now use `https://go.apollo.dev/ij-plugin-snapshots` to get the latest weekly snapshots. ([#5600](https://github.com/apollographql/apollo-kotlin/pull/5600))
* **Multi-version KDoc**: The [published KDoc](https://www.apollographql.com/docs/kotlin/kdoc/index.html) now includes both v3 and v4 versions.

Many thanks to @ribafish, @molundb, @mboyd1993, @rohandhruva and @baconz for their help in this release 💙!

## 👷‍ All changes
* [mockserver] Add MockServer.enqueueError() and MockServer.assertNoRequest() ([#5694](https://github.com/apollographql/apollo-kotlin/pull/5694))
* [runtime] Implement NetworkMonitor for apple platforms ([#5691](https://github.com/apollographql/apollo-kotlin/pull/5691))
* [runtime] Add `NetworkMonitor` ([#5690](https://github.com/apollographql/apollo-kotlin/pull/5690))
* [runtime] Add `ApolloClient.retryOnError(Boolean)` ([#5685](https://github.com/apollographql/apollo-kotlin/pull/5685))
* [websockets-network-transport-incubating] Publish apollo-websocket-network-transport-incubating ([#5693](https://github.com/apollographql/apollo-kotlin/pull/5693))
* [normalized-cache-incubating] Use Store Cache and merge optimistic cache with Memory cache ([#5651](https://github.com/apollographql/apollo-kotlin/pull/5651))
* [runtime] Fix ApolloClient.Builder if the Builder is mutated by the caller after calling build() ([#5683](https://github.com/apollographql/apollo-kotlin/pull/5683))
* [websockets-network-transport-incubating] Introduce incubating WebSocketNetworkTransport ([#5678](https://github.com/apollographql/apollo-kotlin/pull/5678))
* [websockets-network-transport-incubating] Introduce incubating WebSocketEngine ([#5676](https://github.com/apollographql/apollo-kotlin/pull/5676))
* [runtime] Don't assume a single emission in AutoPersistedQueryInterceptor ([#5677](https://github.com/apollographql/apollo-kotlin/pull/5677))
* [runtime] Use expect funs instead of expect classes for DefaultHttpEngine ([#5672](https://github.com/apollographql/apollo-kotlin/pull/5672))
* [runtime] Fix JS websocket throws an ISE on error on Safari and Firefox ([#5670](https://github.com/apollographql/apollo-kotlin/pull/5670))
* [runtime] Use the streaming HttpEngine by default on Apple ([#5671](https://github.com/apollographql/apollo-kotlin/pull/5671))
* [intellij-plugin] Only send telemetry for Apollo Kotlin projects ([#5663](https://github.com/apollographql/apollo-kotlin/pull/5663))
* [gradle-plugin] Use Gradle normalization instead of ours ([#5636](https://github.com/apollographql/apollo-kotlin/pull/5636))
* [execution] Fix converting int and floats to their Kotlin value ([#5637](https://github.com/apollographql/apollo-kotlin/pull/5637))
* [runtime] Add ApolloClient.Builder(ApolloHttpCache) ([#5638](https://github.com/apollographql/apollo-kotlin/pull/5638))
* [gradle-plugin] Use com.android.lint Gradle rules ([#5639](https://github.com/apollographql/apollo-kotlin/pull/5639))
* [all] Update coroutines to 1.8.0 ([#5626](https://github.com/apollographql/apollo-kotlin/pull/5626))
* [runtime] Allow buildPostBody to write operation extensions ([#5630](https://github.com/apollographql/apollo-kotlin/pull/5630))
* [compiler] Add support for @catch on fieldDefinitions, interfaces and objects ([#5623](https://github.com/apollographql/apollo-kotlin/pull/5623))
* [all] Bump Kotlin to 2.0.0-Beta4 ([#5624](https://github.com/apollographql/apollo-kotlin/pull/5624))
* [normalized-cache-incubating] Cache lock changes ([#5608](https://github.com/apollographql/apollo-kotlin/pull/5608))
* [rx-support] Keep rx-support as DeprecationLevel.Error ([#5610](https://github.com/apollographql/apollo-kotlin/pull/5610))
* [gradle-plugin] Add dependsOn(dependencyNotation, bidirectional) ([#5606](https://github.com/apollographql/apollo-kotlin/pull/5606))
* [gradle-plugin] Fix a regression in alwaysGenerateTypesMatching where all types would be generated by default ([#5605](https://github.com/apollographql/apollo-kotlin/pull/5605))
* [️compiler] Add Apollo compiler plugin API ([#5604](https://github.com/apollographql/apollo-kotlin/pull/5604), [#5599](https://github.com/apollographql/apollo-kotlin/pull/5599), [#5591](https://github.com/apollographql/apollo-kotlin/pull/5591), [#5589](https://github.com/apollographql/apollo-kotlin/pull/5589), [#5588](https://github.com/apollographql/apollo-kotlin/pull/5588), [#5582](https://github.com/apollographql/apollo-kotlin/pull/5582), [#5573](https://github.com/apollographql/apollo-kotlin/pull/5573), [#5561](https://github.com/apollographql/apollo-kotlin/pull/5561), [#5560](https://github.com/apollographql/apollo-kotlin/pull/5560), [#5557](https://github.com/apollographql/apollo-kotlin/pull/5557), [#5556](https://github.com/apollographql/apollo-kotlin/pull/5556), [#5554](https://github.com/apollographql/apollo-kotlin/pull/5554), [#5516](https://github.com/apollographql/apollo-kotlin/pull/5516), [#5589](https://github.com/apollographql/apollo-kotlin/pull/5589))
* [intellij-plugin] Publish the IJ plugin snapshots to the JetBrain Marketplace ([#5600](https://github.com/apollographql/apollo-kotlin/pull/5600))
* [runtime] HTTP Headers: remove `X-APOLLO-OPERATION-NAME`, `X-APOLLO-OPERATION-ID` and the multipart boundary ([#5533](https://github.com/apollographql/apollo-kotlin/pull/5533))
* [gradle-plugin] use Worker API and ServiceLoader ([#5590](https://github.com/apollographql/apollo-kotlin/pull/5590))
* [gradle-plugin] deprecate schemaFile and sourceFolder ([#5581](https://github.com/apollographql/apollo-kotlin/pull/5581))
* [gradle-plugin] configuration cache and lazy properties for schema files ([#5580](https://github.com/apollographql/apollo-kotlin/pull/5580))
* [️compiler] Track semanticNonNull spec ([#5577](https://github.com/apollographql/apollo-kotlin/pull/5577))
* [gradle-plugin] bump minimum required Gradle version to 8.0 ([#5579](https://github.com/apollographql/apollo-kotlin/pull/5579))
* [ast] Validate repeatable directives ([#5574](https://github.com/apollographql/apollo-kotlin/pull/5574))
* [compiler] Don't automatically add key fields to union selections ([#5562](https://github.com/apollographql/apollo-kotlin/pull/5562))
* [runtime] Fix disabling batching by default ([#5552](https://github.com/apollographql/apollo-kotlin/pull/5552))
* [gradle-plugin] Select all types in pre-introspection query ([#5547](https://github.com/apollographql/apollo-kotlin/pull/5547))
* [normalized-cache-api] Remove unnecessary suspend from ApolloStore functions ([#5541](https://github.com/apollographql/apollo-kotlin/pull/5541))
* [all] One more step towards K2 but blocked on https://youtrack.jetbrains.com/issue/KT-21846 ([#5536](https://github.com/apollographql/apollo-kotlin/pull/5536))
* [all] Target Java17 for Android .aars and Java11 for apollo-gradle-plugin.jar ([#5534](https://github.com/apollographql/apollo-kotlin/pull/5534))
* [compiler] Remove old `generateAsInternal` code ([#5526](https://github.com/apollographql/apollo-kotlin/pull/5526))
* [compiler] Lock down apollo-compiler API ([#5524](https://github.com/apollographql/apollo-kotlin/pull/5524))
* [normalized-cache-sqlite] Use windowSizeBytes argument of AndroidSqliteDriver ([#5523](https://github.com/apollographql/apollo-kotlin/pull/5523))
* [intellij-plugin] Strip Apollo client directives before executing operations ([#5517](https://github.com/apollographql/apollo-kotlin/pull/5517))
* [execution] Fix converting GraphQL Float values to Kotlin ([#5511](https://github.com/apollographql/apollo-kotlin/pull/5511))
* [intellij-plugin] Don't show a visible task with progress bar while fetching the Apollo Conf ([#5501](https://github.com/apollographql/apollo-kotlin/pull/5501))
* [intellij-plugin] Inspection: missing directive import ([#5494](https://github.com/apollographql/apollo-kotlin/pull/5494))
* [intellij-plugin] Use recent version of slf4j to avoid a classloader issue ([#5495](https://github.com/apollographql/apollo-kotlin/pull/5495))
* [normalized-cache-sqlite] Allow custom SupportSQLiteOpenHelper.Callback in the SqlNormalizedCacheFactory ([#5488](https://github.com/apollographql/apollo-kotlin/pull/5488))
* [mockserver] Allow to set the content-type of String responses ([#5489](https://github.com/apollographql/apollo-kotlin/pull/5489))
* [debug-server] Start LocalServerSocket on background thread, and handle exception ([#5493](https://github.com/apollographql/apollo-kotlin/pull/5493))
* [cli-incubating] Publish apollo-cli-incubating again ([#5486](https://github.com/apollographql/apollo-kotlin/pull/5486))
* [intellij-plugin] Don't suggest v4 migration until it is stable ([#5477](https://github.com/apollographql/apollo-kotlin/pull/5477))
* [tooling] bump version of Apollo to `4.0.0-beta.3` ([#5452](https://github.com/apollographql/apollo-kotlin/pull/5452))
* [debug-server] Don't crash if ApolloDebugServerInitializer is not run (e.g. in unit tests) ([#5484](https://github.com/apollographql/apollo-kotlin/pull/5484))
* [intellij-plugin] Add debugging logs around the cache viewer ([#5475](https://github.com/apollographql/apollo-kotlin/pull/5475))
* [debug-server] Debug server: don't crash when a client has no caches ([#5479](https://github.com/apollographql/apollo-kotlin/pull/5479))
* [gradle-plugin] add apollo.deps ([#5460](https://github.com/apollographql/apollo-kotlin/pull/5460))

# Version 4.0.0-beta.4

_2023-12-12_

## ✨ Initial Wasm support (#5458)

This release adds initial support for WebAssembly by adding the `wasmJs` target.

Executing queries/mutations is working but this target is
experimental ([Kotlin/Wasm](https://kotlinlang.org/docs/wasm-overview.html) is Alpha) and has multiple limitations:

- No WebSockets
- No caching
- No support for WASI or NodeJS

## 🪲 Bug fix

Downloading a schema from introspection (`./gradlew downloadServiceSchemaFromIntrospection`) got broken in the previous
release (#5449) and is now fixed.

## 👷‍ All changes

* [compiler] validate operation directives & enforce presence of the nullability directive definitions by @martinbonnin
  in https://github.com/apollographql/apollo-kotlin/pull/5443
* [build] Bump okio version by @martinbonnin in https://github.com/apollographql/apollo-kotlin/pull/5447
* [build] Bump uuid version by @martinbonnin in https://github.com/apollographql/apollo-kotlin/pull/5448
* Fix 2nd step introspection by @BoD in https://github.com/apollographql/apollo-kotlin/pull/5451
* Add wasmJs target by @martinbonnin in https://github.com/apollographql/apollo-kotlin/pull/5458
* Add validation to check schema definitions are compatible with the bundled ones by @BoD
  in https://github.com/apollographql/apollo-kotlin/pull/5444

# Version 4.0.0-beta.3

_2023-12-05_

Many thanks @chris-hatton and @sdfgsdfgd for contributing this version 💙

## 🧩 IDE plugin: in-memory cache support

The normalized cache viewer can now display the contents of your in-memory cache. To do so, it relies on a
debug server that you need to run in your debug builds:

```kotlin

val apolloClient = ApolloClient.Builder()
    .serverUrl("https://example.com/graphql")
    .build()

if (BuildConfig.DEBUG) {
  ApolloDebugServer.registerApolloClient(apolloClient)
}
```

You can read more about it [in the "Apollo debug server" documentation page](https://www.apollographql.com/docs/kotlin/v4/testing/apollo-debug-server).

## Experimental `@catch` and `@semanticNonNull` support

`@catch` makes it possible to model GraphQL errors as `FieldResult` Kotlin classes giving you inline access to errors:

```graphql
query GetUser {
  user {
    id
    # map name to FieldResult<String?> instead of stopping the parsing
    name @catch
    avatarUrl
  }
}
```

`@semanticNonNull` is a better `@nonnull`. `@semanticNonNull` makes it possible to mark a field as null only on error. The matching Kotlin property is then generated as non-null:

```graphql
# mark User.name as semantically non-null
extend type User @semanticNonNull(field: "name")
```

Both those directives are experimental and require opt-in of the [nullability directives](https://github.com/apollographql/specs/pull/36)

You can read more about them [in the "handle nullability" documentation page](https://www.apollographql.com/docs/kotlin/v4/advanced/nullability).

## Experimental `@oneOf` support

`@oneOf` introduces input polymorphism to GraphQL:

```graphql
input PetInput @oneOf {
  cat: CatInput
  dog: DogInput
  fish: FishInput
}

input CatInput { name: String!, numberOfLives: Int }
input DogInput { name: String!, wagsTail: Boolean }
input FishInput { name: String!, bodyLengthInMm: Int }

type Mutation {
  addPet(pet: PetInput!): Pet
}
```

With `@oneOf`, only one of `cat`, `dog` or `fish` can be set. 

`@oneOf` support is automatically enabled if your schema has the `@oneOf` directive definition.

You can read more about it [in the `@oneOf` RFC](https://github.com/graphql/graphql-spec/pull/825)

## 👷‍ All changes

* [all] `@oneOf` support (#5394, #5388)
* [all] `@catch` and `@semanticNonNull` support (#5405)
* [all] Take default values into account when computing field cache keys (#5384)
* [all] Bump Kotlin to 2.0.0-Beta1 (#5373)

* [IJ Plugin] Add 'Input class constructor issue' inspection (#5427)
* [IJ plugin] Update v3->v4 migration following API tweaks (#5421)
* [IJ Plugin] Report invalid oneOf input object builder uses (#5416)
* [IJ Plugin] Add inspection for @oneOf input type constructor invocation (#5395)
* [IJ plugin] Make the refresh button work with all normalized cache sources. (#5400)
* [IJ Plugin] Cache viewer: take numbers into account in key sorting (#5396)
* [IJ Plugin] Bump pluginUntilBuild to 233 (#5377)
* [IJ Plugin] Telemetry: don't use a libraries changed listener (#5361)
* [IJ Plugin] Cache viewer: add cache size to selector (#5357)
* [IJ plugin] Use apollo-debug-server to retrieve normalized caches (#5348)
* [IJ plugin] Cache viewer: "Pull from Device" modal dialog instead of action menu (#5333)
* [IJ Plugin] Fix v3->v4 migration with ApolloCompositeException (#5330)

* [runtime] remove some of the ApolloResponse.Builders (#5426)
* [runtime] Add a few symbols as ERROR deprecation for the migration (#5422)
* [runtime] Add executeV3 + toFlowV3 (#5417)
* [runtime] Revive dataAssertNoErrors (#5419)
* [runtime] Allow no data and no exception in case of GraphQL errors (#5408)
* [runtime] Expose ExecutionContext to HttpEngine and add OkHttp helpers (#5383)
* [runtime] Improve deprecation messages (#5411)
* [runtime] Go back to just one Adapter class (#5403)
* [runtime] Fix Optional.getOrElse (#5399)

* [compiler] Remove kotlin-labs-0.1 directives (#5404)
* [compiler] Throw a better exception than NullPointerException if a value is missing (#5402)
* [compiler] ExecutableValidationResult is returned by validateAsExecutable() and cannot be @ApolloInternal (#5406)
* [compiler] Rework the IrType hierarchy (#5392)
* [compiler] remove CCN (#5387)
* [compiler] Remove antlr (#5336)
* [compiler] Tweak description javadoc, there is no need to use the same escaping as Kotlin (#5424)

* [mockserver] Support setting port for Apollo MockServer (#5389)
* [mockserver] Add WebSocket support to the MockServer (#5334)
* [tools] Implement 2-step introspection (#5371)
* [apollo-execution] Allow to pass arguments to the root types (#5352)
* [apollo-ksp] Initial support for interfaces (#5351)
* [apollo-debug-server] Add apollo-debug-server (#5353)

# Version 4.0.0-beta.2

_2023-10-23_

We're continuing to progress towards the stable release of Apollo Kotlin v4 with this 2nd beta, which contains a few bug
fixes and a new normalized cache viewer in the IDE plugin.

This is a great time to try out the new version and report any issues you might find!

## 🧩 IDE plugin: normalized cache viewer

The IDE plugin now has a graphical tool to inspect a normalized cache database. It lets you browse the records and see
their contents.
This is useful to debug cache issues, or to understand how the normalized cache works.

The tool is available from `View` | `Tool Windows` | `Apollo Normalized Cache`.

More information about the plugin can be
found [here](https://www.apollographql.com/docs/kotlin/v4/testing/android-studio-plugin).

## 👷‍ All changes

* Fragment variables: fix false warning about unused variables (#5290)
* Fix reading fragment with include directives from the cache (#5296)
* Fix partial data throwing with useV3ExceptionHandling and normalized cache (#5313)
* Unbreak benchmarks (#5284)
* Bump to gradle 8.4 and IJGP 1.16.0 (#5286)
* Add apollo-execution and apollo-ksp (#5281)
* [IJ plugin] Normalized cache viewer: UI (#5298)
* Fix build (#5301)
* Bump to Kotlin 1.9.20-RC (#5300)
* [IJ plugin] Telemetry: networking (#5285)
* [IJ plugin] Cache viewer: record quick filter (#5302)
* Update release script to update versions in IJ plugin (#5303)
* MockServer API cleanup (#5307)
* Remove some warnings (#5308)
* Use the default hierarchy template (#5309)
* [IJ plugin] Cache viewer: open/read db file (#5306)
* [IJ plugin] Cache viewer: add back/forward buttons, and copy action (#5310)
* Fix NSURL tests on recent apple OSes (#5315)
* [IJ plugin] Cache viewer: pull file from attached devices (#5314)
* Make more of MockServer common code, only abstract the socket part (#5316)

# Version 4.0.0-beta.1

_2023-10-02_

The first beta of the next major version of Apollo Kotlin is here!

While there still may be a few API changes before the stable release, we are getting close and this is a great time to try out the new version and report any issues you might find!

## 💙️ External contributors

Many thanks to @baconz and @hbmartin for their awesome contributions to this release! 

## ❗️ Schema Nullability Extensions (#5191)

The GraphQL community [is working hard at making it easier to work with nullability in GraphQL](https://github.com/graphql/client-controlled-nullability-wg/). 

In Apollo Kotlin, it is now possible to change the nullability of fields _and_ list elements at the schema level using schema extensions. This is useful if you believe the schema made a field nullable for error reasons only and you don't want to handle those errors. In these cases, the whole query will return as an error.

Given the following SDL:
```graphql
# schema.graphqls
type Query {
  random: Int
  list: [String]
  required: Int!
}
```

You can extend it like so:
```graphql
# extra.graphqls
extend type Query {
  # make random non-nullable
  random: Int!
  # make list and list items non-nullable
  list: [String!]!
  # make required nullable
  required: Int
  # add a new field
  new: Float
}
```

## 📜️ Code generation

### `generateMethods` option to control which model methods are generated (#5212)

By default all Kotlin models, operations, fragments, and input objects are generated as data classes. This means that the Kotlin compiler will
auto-generate `toString`, `equals` `hashCode`, `copy` and `componentN`. If you don't think you need all of those
methods, and/or you are worried about the size of the generated code, you can now choose which methods to generate with the `generateMethods` option:

```kotlin
apollo {
  service("service") {
    // Generates equals/hashCode
    generateMethods.set(listOf("equalsHashCode"))
    // Also generates toString, equals, and hashcode
    generateMethods.set(listOf("equalsHashCode", "toString"))
    // Only generates copy
    generateMethods.set(listOf("copy"))
    // Generates data classes (the default)
    generateMethods.set(listOf("dataClass"))
  }
}
```

### Other codegen tweaks

* `Enum.values()` is no longer recommended when using Kotlin 1.9+ and the generated code now uses `entries` instead (#5208)
* Deprecation warnings in generated code are suppressed (#5242)

## 🧩 IntelliJ plugin

* You can now suppress reported unused fields, by adding a comment on the field, or by configuring a regex in the settings (#5195, #5197)
* Opening an operation in Sandbox now includes all referenced fragments (#5236)

## 🪲 Bug fixes 

* Detect cyclic fragment references (#5229)
* Fix `Optional<V>.getOrThrow()` when V is nullable (#5192)
* `useV3ExceptionHandling` only throws when there are no errors populated (#5231)
* Tweak the `urlEncode` algorithm (#5234)
* Add a validation for adding `keyFields` on non-existent fields (#5215)
* Fix logging when the response body is a single line (#5254)

## 👷‍ All changes

* [Infra] Count tests in CI (#5181)
* Test: remove flake  (#5167)
* Use compilations instead of multiple mpp targets for java codegen tests (#5164)
* [IJ plugin] Add fragment usages when going to fragment declaration (#5189)
* Add `mergeExtensions` and `toFullSchemaGQLDocument` (#5162)
* Fix `Optional<V>.getOrThrow()` when V is nullable (#5192)
* Schema Nullability Extensions (#5191)
* [IJ plugin] Add inspection suppressor to allow suppression on fields (#5195)
* Add PQL support to `registerOperations {}`  (#5196)
* Add WebSocketMockServer and tests for WebSocketEngine  (#5187)
* [IJ plugin] Add options to ignore fields when reporting unused fields (#5197)
* Unbreak benchmarks (#5202)
* Bump uuid and okio (#5204)
* [IJ plugin] Update references to 4.0.0-alpha.2 to 4.0.0-alpha.3 (#5205)
* Use entries instead of values() when using Kotlin 1.9 (#5208)
* Add `generateMethods` options to control which methods are generated on "data" classes (#5212)
* Add a validation for adding keyFields on non-existent fields (#5215)
* Engine tests: use compilations to share logic between ktor/default engines (#5216)
* Skip Dokka during development (#5219)
* Introduce JsonReader.toApolloResponse (#5218)
* Add tests for empty objects in last chunk (#5223)
* useV3ExceptionHandling should only throw when there are no errors populated (#5231)
* Tweak the urlencode algorithm (#5234)
* [IJ plugin] Gather referenced fragments when opening in Apollo Sandbox (#5236)
* Kotlin 1.9.20-Beta (#5232)
* Suppress Kotlin warnings in generated code (#5242)
* Add Optional.getOrElse(value) (#5243)
* Add Error.Builder() (#5244)
* Add APOLLO_RELOCATE_JAR and APOLLO_JVM_ONLY (#5245)
* Detect cyclic fragment references (#5229)
* [IJ plugin] Telemetry: collect properties (#5246)
* Bump kotlin to 1.9.20-Beta2 (#5249)
* [IJ plugin] Telemetry: settings and opt-out dialog (#5247)
* [IJ plugin] Telemetry: add IDE/plugin related properties and events (#5250)
* Fix cyclic fragment detection (#5252)
* [IJ plugin] Add an ErrorReportSubmitter (#5253)
* Logging a single line response body by @hbmartin in https://github.com/apollographql/apollo-kotlin/pull/5254
* [IJ plugin] Schedule send telemetry (#5256)
* Allow MapJsonReader to read non-Map instances (#5251)
* [IJ plugin] Fix a crash when loading plugin (#5260)
* Tweaks for K2 (#5259)
* Update apollo published (#5263)

# Version 4.0.0-alpha.3

_2023-08-08_

A lot of additions to the [IntelliJ plugin](https://www.apollographql.com/docs/kotlin/v4/testing/android-studio-plugin) as well as a new GraphQL parser, a new Ktor multiplatform engine and more!

## 💙 External contributors 💙

Apollo Kotlin wouldn't be where it is today without the awesome feedback, discussions and contributions from community members. Specifically in this release, we want to give a huge THANK YOU to: @Emplexx, @sonatard, @yt8492, @mayakoneval, @Meschreiber, @pcarrier and @ashare80

## 🧩 IntelliJ plugin 

### 👓 Unused field inspection (#5069)

The IntelliJ plugin now detects unused fields in your queries and greys them out:

https://github.com/apollographql/apollo-kotlin/assets/372852/6a573a78-4a07-4294-8fa5-92a9ebb02e6c

## v4 migration

The IntelliJ plugin can migrate most of your codebase to v4. To try it out, go to:

`Tools` -> `Apollo` -> `Migrate to Apollo Kotlin 4`

Because Kotlin is such a rich language and we can't account for all possible ways to configure your build, you might have to do some manual tweaks after the migration. But the plugin should handle most of the repetitive tasks of migrating.

### ☁️ Schema download (#5143)

If you configured introspection, you can now download your schema directly from IntelliJ

<img src="https://github.com/apollographql/apollo-kotlin/assets/372852/3a15f587-b057-4df5-82c0-2e0b0247c203" width=250/>

### 📖 documentation

The IntelliJ plugin now has its [own dedicated documentation page](https://www.apollographql.com/docs/kotlin/v4/testing/android-studio-plugin). 

Consult it to find out everything you can do with the plugin as well as installation instructions.


## 🌳 Multiplatform Apollo AST (#5047)

Apollo AST, the GraphQL parser powering Apollo Kotlin is now a manually written recursive descent parser, compared to an automatically generated [Antlr](https://www.antlr.org/) parser before. 
[Benchmarks](https://github.com/apollographql/apollo-kotlin/pull/5047) show a x2 to x3 speed improvement and the parser also now supports all platforms Apollo Kotlin supports.

## ❗❓ Initial Client Controlled Nullability (CCN) support (#5118)

[Client Controlled Nullability (CCN)](https://github.com/graphql/graphql-wg/blob/main/rfcs/ClientControlledNullability.md) is a GraphQL specification RFC aiming at making it easier to work with GraphQL in type safe languages like Kotlin and Swift.

To use CCN, use the `!` and `?` CCN modifiers in your queries:

```graphql
query GetUser {
  user {
    id
    # name is required to display the user
    name!
    # phoneNumber is optional
    phoneNumber?
  }
}
```

The RFC is still in early stages and requires server support. The API and final shape of the RFC might still change. By adding support in Apollo Kotlin, we're hoping to unblock potential users and gather real life feedbacks helping the proposal move forward.

## 📡 Ktor engine (#5142)

Apollo Kotlin now ships a `apollo-engine-ktor` that you can use to replace the default HTTP and WebSocket engines of ApolloClient. To use it, add `apollo-engine-ktor` to your dependencies:

```kotlin
dependencies {
  implementation("com.apollographql.apollo3:apollo-engine-ktor")
}
```

And configure your client to use it:

```kotlin
val apolloClient = ApolloClient.Builder()
    .serverUrl("https://example.com/graphql")
    .httpEngine(KtorHttpEngine())
    .webSocketEngine(KtorWebSocketEngine())
    .build()
```

## 👷 `generateInputBuilders` (#5146)

For Kotlin codegen, Apollo Kotlin relied on constructors with [default arguments](https://kotlinlang.org/docs/functions.html#default-arguments). While this works well in most cases, default arguments lack the ability to distinguish between `null` and `absent` meaning you have to wrap your values in `Optional` before passing them to your constructor. If you had a lot of values, it could be cumbersome:

```kotlin
val input = SignupMemberInput(
    dob = Utils.changeDateFormat(user.dobMMDDYYYY, "MM/dd/yyyy", "yyyy-MM-dd"),
    firstName = Optional.Present(user.firstName),
    lastName = user.lastName,
    ssLast4 = Optional.Present(user.ssnLastFour),
    email = user.email,
    cellPhone = Optional.Present(user.phone),
    password = user.password,
    acceptedTos = true,
    formIds = Optional.Present(formIds),
    medium = Optional.Present(ConsentMediumEnum.android)
)
```

To generate Kotlin Builders, set `generateInputBuilders` to true in your Gradle file:

```kotlin
apollo {
  service("api") {
    packageName.set("com.example")

    generateInputBuilders.set(true)
  }
}
```

With Builders, the same above code can be written in a more fluent way:

```kotlin
val input = SignupMemberInput.builder().apply {
    dob(Utils.changeDateFormat(user.dobMMDDYYYY, "MM/dd/yyyy", "yyyy-MM-dd"))
    firstName(user.firstName)
    lastName(user.lastName)
    ssLast4(user.ssnLastFour)
    email(user.email)
    cellPhone(user.phone)
    password(user.password)
    acceptedTos(true)
    formIds(formIds)
    medium(ConsentMediumEnum.ANDROID)
}.build()
```


## 👷‍ All changes

* [IJ plugin] Update platformVersion to 223 (#5166)
* [runtime] Add Ktor Engine (#5142)
* [tests] Add a test for field names that have the same name as an enum type (#5158)
* [api] Remove limitation on the JSON nesting. If the JSON is way too nested, an OutOfMemory exception will happen (#5161)
* [ast] add HasDirectives to all things with directives (#5140)
* [compiler] add a test for types named `Object` (#5156)
* [ast] Add a special comment to disable the GraphQL intelliJ plugin inspection (#5154)
* [IJ plugin] Inspection to suggest adding an introspection block (#5152)
* [compiler] Better KDoc escape (#5155)
* [ast] Allow explicit CCN syntax (#5148)
* [compiler] Add generateInputBuilders (#5146)
* [infra] Update KotlinPoet (#5147)
* [infra] update Gradle to 8.3-rc-3 (#5149)
* [IJ plugin] Add a Download Schema action (#5143)
* [runtime] Remove ChannelWrapper (#5145)
* [IJ Plugin] Suggest Apollo 4 migration from version catalog and build.gradle.kts dependencies (#5141)
* [IJ plugin] v3->v4 migration: add `useV3ExceptionHandling(true)` to `ApolloClient.Builder()`. (#5135)
* [IJ plugin] Improve compat->operationBased migration (#5134)
* [IJ plugin] Avoid a crash caught in inspection (#5139)
* [ast] add GQLNamed and GQLDescribed on all types that have a name or a description (#5127)
* [ast] Omit scalar definitions from SDL (#5132)
* [infra] Use SQLDelight 2.0.0 (#5133)
* [IJ Plugin] Don't report redefinitions of built-in types as errors (#5131)
* [IJ plugin] v3 -> v4 migration: enum capitalization (#5128)
* [ast] Initial CCN support (#5118)
* [infra] Make generateSourcesDuringGradleSync opt-in now that we have the IJ plugin (#5117)
* [compose] Catch ApolloException in toState and watchAsState (#5116)
* [IJ plugin] v3 -> v4 migration: Gradle conf (#5114)
* [compiler] validate query/mutation/subscription directives (#5113)
* [cache] Move serialization outside of cache lock (#5101)
* [infra] Use compose 1.5.0 stable (#5108)
* [compiler] Fix "no schema found" error message (#5106)
* [IJ plugin] v3 -> v4 migration: deprecations/renames, part 2 (#5109)
* [infra] More Apollo AST APIs (#5104)
* [infra] Apollo AST: add start/end instead of endColumn/endLine (#5103)
* [IJ plugin] v3 -> v4 migration: deprecations/renames (#5099)
* [ast] fix column computation of block strings (#5102)
* [benchmarks] Add macrobenchmarks (#5100)
* [IJ plugin] Migrate to Apollo Kotlin 4: dependencies (#5097)
* [IJ plugin] Support IJ platform 232 (#5095)
* [infra] bump ijgp (#5091)
* [infra] remove a bunch of build workarounds (#5090)
* [4.0 cleanups] Remove DefaultImpls everywhere (#5088)
* [infra] Remove golatac (#5086)
* [infra] Use SQLDelight 2.0.0-rc02 and AGP 8.0.0 (#5085)
* [ast] Switch back the parser to Strings and add more tests (#5078)
* [benchmarks] add graphql-java benchmark (#5077)
* [infra] Use Kotlin 1.9.0 (#4997)
* [ast] Turn into a mpp module and move jmh benchmark to an integration test (#5072)
* [IJ/AS Plugin] Add unused operation and unused field inspections (#5069)
* [ast] add endLine/endColumn (#5064)
* [ast] Add more tests and rewrite the lexer to use bytes instead of strings (#5063)
* [IJ plugin] Add a test for ApolloFieldInsightsInspection (#5062)
* [IJ plugin] Add an "enclose in @defer fragment" quick fix for slow field inspection (#5061)
* [IJ plugin] Add "Schema in .graphql file" inspection (#5059)
* [ast] Add multiplatform apollo-ast (#5047)
* [IJ plugin] Distinguish Apollo v3 and v4 (#5056)

# Version 4.0.0-alpha.2

_2023-06-30_

## 🧩Navigate to GraphQL

You can now navigate from Kotlin both GraphQL or Generated code:

https://github.com/apollographql/apollo-kotlin/assets/3974977/acd4bf34-db35-4442-a7af-b6151701620c

See [blog the blog post](https://www.apollographql.com/blog/mobile/kotlin/announcing-the-apollo-kotlin-plugin-for-android-studio-and-intellij-idea/) and [installation instructions](https://github.com/apollographql/apollo-kotlin/blob/main/intellij-plugin/README.md#installation-instructions) for more information.

## 🧩Field insights

You can now connect the IJ/AS plugin to your GraphOS account. If your backend is configured to report field traces, the plugin will display a warning for expensive fields that may be slow to fetch.

![Field insights](https://github.com/apollographql/apollo-kotlin/assets/3974977/efb9e813-bcc4-4b0f-99ed-f32257c28b14)

You can configure it in your IJ/AS settings `Languages & Frameworks` -> `GraphQL` -> `Apollo Kotlin`. See also [#5048](https://github.com/apollographql/apollo-kotlin/pull/5048) for a video of the setup.

## 🌐`@jsExport`

`responseBased` codegen can now generate models annotated with [@jsExport](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-js-export/). This allows to fetch your response using faster JS-only APIs and cast them to the generated models. See [the JS interoperability documentation](https://www.apollographql.com/docs/kotlin/v4/advanced/js-interop#jsexport) for more information. Many thanks to @baconz for the deep dive 💙

## 👷‍ All changes

* [IJ plugin] FieldInsights (#5034)
* [IJ plugin] Keep navigation working when using import aliases for operations and fragments (#5041)
* [IJ plugin] Make navigation more robust when gql elements are lowercase (#5037)
* Refactor custom scalar adapters (#4905)
* [IJ plugin] Add an 'Open in Apollo Sandbox' action (#5022)
* [IJ plugin] Update v3 version and codegen wording (#5015)
* add encodeDefaults to introspection schema serializer (#5016)
* Better error message for Kotlin objects that cannot be converted to JSON (#5011)
* Http cache: do not cache mutations (#5014)
* [IJ plugin] Find usages GQL -> Kotlin (#5006)
* [IJ plugin] Add GQL -> generated Kotlin navigation (#4999)
* [IJ plugin] Override GraphQL icons (#4980)
* add `Service.operationManifestFormat` (#4981)
* [WebSockets] Fix fan out in websocket network transport (#4972)
* Throw inside flow in MapTestNetworkTransport (#4982)
* [IJ plugin] Add navigation to type declaration (cmd shift b) (#4978)
* [IJ plugin] Add navigation to input types / fields (#4968)
* Fix jsExport Gradle annotation mismatch (#4975)
* Generate response based code with jsExport (#4907)
* [IJ plugin] Navigate to enum declaration (#4965)
* remove Built-By attribute in generated jars (#4961)

# Version 3.8.2

_2023-05-25_

A maintenance release with bugfixes, mostly around WebSockets and subscriptions as well as a LocalTime adapter and options to work with operation manifests.

Huge THANK YOU to @baconz, @AlexHartford, @Oleur for the love they put in WebSocket contributions as well as @Jephuff for their first contribution 💙 .

## 👷‍ All changes

* add `Service.operationManifestFormat` (#4981)
* WebSockets: Fix fan out in websocket network transport (#4972)
* Test: Throw inside flow in MapTestNetworkTransport (#4982)
* Doc: Add Optional.Absent in documentation (#4979)
* Doc: clarify that operationBased codegen is the recommendation (#4966)
* AST: use existing as the base for copying enum values (#4943)
* Cache: Fix deleting records in the SqlNormalizedCache with cascade-true (#4938)
* Compiler: Fix deprecated input field usage false positive (#4935)
* Doc: Clarify KDoc of `watch()` (#4914)
* WebSockets: allow changing the serverUrl of WebSocketNetworkTransport (#4885)
* WebSockets: accept connectionPayload lambda instead of static auth (#4855)
* Add LocalTime adapter for Java and Kotlin (#4829)
* Doc: Add a section about the operationBased codegen (3.x) (#4940)

# Version 4.0.0-alpha.1

_2023-05-16_

This release is the first alpha of the next major version of Apollo Kotlin: 4.0.0. 

This version is under development, but we want to give you a preview of what's coming, and get your early feedback. Please see the [roadmap](https://github.com/apollographql/apollo-kotlin/blob/main/ROADMAP.md) for more details about the release plan.

While version 3 changed a lot of APIs compared to version 2, version 4 should be mostly compatible with version 3. Version 4 will even keep the same package name in order to keep the number of changes low.

The error handling has changed but besides that, the version should be mostly compatible. Please consult the [migration guide](https://www.apollographql.com/docs/kotlin/v4/migration/4.0) for all the details.

We would love to hear your feedback on this release. Please report any issues, questions, ideas, or comments on the [issue tracker](https://github.com/apollographql/apollo-kotlin/issues).

## 🛠️ Android Studio / IntelliJ plugin

See [this page](https://github.com/apollographql/apollo-kotlin/tree/main/intellij-plugin) for installation instructions and more information.

This plugin for Android Studio and IntelliJ helps you work with Apollo Kotlin. It provides the following features:

- Automatic code generation
- Integration with the [GraphQL IntelliJ Plugin](https://plugins.jetbrains.com/plugin/8097-js-graphql)
- Navigation to GraphQL definitions
- Helpers to migrate your project to the latest version

## ⚡️ `ApolloResponse.exception` for error handling

Error handling is an important aspect of a client library and we found it could benefit from some changes.

In particular we are moving away from throwing exceptions:
- This improves dealing with Flows as they will no longer terminate on errors
- It helps grouping all error handling (network, GraphQL, caching) in the same area of your code

To that effect, there is now an `ApolloResponse.exception : ApolloException` property, which will be present when a network error or cache miss have occurred, or when GraphQL errors are present:

```kotlin
val data = response.data
when {
  data != null -> {
    println("The server returned data: $data")
  }
  else -> {
    // An error happened, check response.exception for more details or just display a generic error 
    when (response.exception) {
      is ApolloGraphQLException -> // do something with exception.errors
      is ApolloHttpException -> // do something with exception.statusCode
      is ApolloNetworkException -> TODO()
      is ApolloParseException -> TODO()
      else -> // generic error
    }
  }
}
```

To ease the migration to v4, the v3 behavior can be restored by calling `ApolloClient.Builder.useV3ExceptionHandling(true)`.

Feedback about this change is welcome on [issue 4711](https://github.com/apollographql/apollo-kotlin/issues/4711).

## ☕️ `apollo-runtime-java` for better Java support

As v3 has a Kotlin and Coroutines first API, using it from Java is sometimes impractical or not idiomatic. That is why in v4 we are introducing a new Java runtime, written in Java, which provides a Java friendly API. It is callback based and doesn't depend on a third-party library like Rx.

To use it in your project, instead of the usual runtime (`com.apollographql.apollo3:apollo-runtime`), use the following dependency in your `build.gradle[.kts]` file:

```kotlin
implementation("com.apollographql.apollo3:apollo-runtime-java")
```

Then you can use the `ApolloClient` class from Java:

```java
ApolloClient apolloClient = new ApolloClient.Builder()
  .serverUrl("https://...")
  .build();

apolloClient
  .query(MyQuery.builder().build())
  .enqueue(new ApolloCallback<MyQuery.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<MyQuery.Data> response) {
        System.out.prinitln(response.getData());
      }
  });
```

A few examples can be found in the [tests](https://github.com/apollographql/apollo-kotlin/tree/main/tests/java-client/src/test/java/test).

## 🔃 Multi-module: automatic detection of used types

In multi-module projects, by default, all the types of an upstream module are generated because there is no way to know in advance what types are going to be used by downstream modules. For large projects this can lead to a lot of unused code and an increased build time.

To avoid this, in v3 you could manually specify which types to generate by using `alwaysGenerateTypesMatching`. In v4 this can now be computed automatically by detecting which types are used by the downstream modules.

To enable this, add the "opposite" link of dependencies with `isADependencyOf()`.

```kotlin
// schema/build.gradle.kts
apollo {
  service("service") {
    packageName.set("schema")

    // Enable generation of metadata for use by downstream modules 
    generateApolloMetadata.set(true)

    // More options...

    // Get used types from the downstream module
    isADependencyOf(project(":feature1"))

    // You can have several downstream modules
    isADependencyOf(project(":feature2"))
  }
}
```

```kotlin
// feature1/build.gradle.kts
apollo {
  service("service") {
    packageName.set("feature1")
    
    // Get the generated schema types (and fragments) from the upstream schema module 
    dependsOn(project(":schema")) 
  }
}
```

# Version 3.8.1

_2023-04-21_

This patch release contains 2 bug fixes.

## 👷‍ All changes

* Add ignoreApolloClientHttpHeaders (#4838)
* Download introspection: handle GraphQL errors (#4861)

# Version 3.8.0

_2023-03-28_

This release adds two new artifacts that contain Jetpack compose extensions amongst other fixes.

## 💙️ External contributors

Many thanks to @slinstacart and @hbmartin for their contributions to this release!

## ✨ [New] Jetpack compose extension (#4802)

You can now use the `apollo-compose-support` artifact: 

```kotlin
// build.gradle.kts
dependencies {
  implementation("com.apollographql.apollo3:apollo-compose-support")
}
```

This artifact contains the `toState()` and `watchAsState()` extensions:

```kotlin
/**
 * A stateful composable that retrieves your data
 */
@OptIn(ApolloExperimental::class)
@Composable
fun LaunchDetails(launchId: String) {
    val response by apolloClient.query(LaunchDetailsQuery(launchId)).toState()
    val r = response
    when {
        r == null -> Loading() // no response yet
        r.exception != null -> ErrorMessage("Oh no... A network error happened: ${r.exception!!.message}")
        r.hasErrors() -> ErrorMessage("Oh no... A GraphQL error happened ${r.errors[0].message}.")
        else -> LaunchDetails(r.data!!, navigateToLogin)
    }
}

/**
 * A stateless composable that displays your data
 */
@Composable
private fun LaunchDetails(
        data: LaunchDetailsQuery.Data,
) {
  // Your UI code goes here
}
```

If you are working with paginated data, you can also add `apollo-compose-paging-support` to your dependencies:

```kotlin
// build.gradle.kts
dependencies {
  implementation("com.apollographql.apollo3:apollo-compose-paging-support")
}
```

This artifact contains a helper function to create `androidx.pagin.Pager` instances ([androix documentation](https://developer.android.com/reference/androidx/paging/Pager)):

```kotlin
@OptIn(ApolloExperimental::class)
@Composable
fun LaunchList(onLaunchClick: (launchId: String) -> Unit) {
  val lazyPagingItems = rememberAndCollectPager<LaunchListQuery.Data, LaunchListQuery.Launch>(
          config = PagingConfig(pageSize = 10),
          appendCall = { response, loadSize ->
            if (response?.data?.launches?.hasMore == false) {
              // No more pages
              null
            } else {
              // Compute the next call from the current response
              apolloClient.query(
                      LaunchListQuery(
                              cursor = Optional.present(response?.data?.launches?.cursor),
                              pageSize = Optional.present(loadSize)
                      )
              )
            }
          },
          getItems = { response ->
            // Compute the items to be added to the page from the current response
            if (response.hasErrors()) {
              Result.failure(ApolloException(response.errors!![0].message))
            } else {
              Result.success(response.data!!.launches.launches.filterNotNull())
            }
          },
  )
  
  // Use your paging items:
  if (lazyPagingItems.loadState.refresh is LoadState.Loading) {
    Loading()
  } else {
    LazyColumn {
      items(lazyPagingItems) { launch ->
        // Your UI code goes here
      }
      item {
        when (val append = lazyPagingItems.loadState.append) {
          is LoadState.Error -> // Add error indicator here 
          LoadState.Loading -> // Add loading indicator here
        }
      }
    }
  }
}
```

As always, feedback is very welcome. Let us know what you think of the feature by
either [opening an issue on our GitHub repo](https://github.com/apollographql/apollo-kotlin/issues)
, [joining the community](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
or [stopping by our channel in the KotlinLang Slack](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)(get your
invite [here](https://slack.kotl.in/)).

## ✨ [New] Gradle plugin: run codegen after gradle sync

If you import a new project or run a Gradle sync, your GraphQL models are now automatically generated so that the IDE can find the symbols and your files do not show red underlines. This takes into account Gradle up-to-date checks and it should be pretty fast. If you want to opt-out, you can do so with `generateSourcesDuringGradleSync.set(false)`:

```kotlin
apollo {
  // Enable automatic generation of models during Gradle sync (default)
  generateSourcesDuringGradleSync.set(true)

  // Or disable automatic generation of models to save on your Gradle sync times
  generateSourcesDuringGradleSync.set(false)

  service("api") {
    // Your  GraphQL configuration
  }
}
```

## 👷‍ All changes

* Allow to add HTTP headers on top of ApolloClient ones (#4754)
* Kotlin 1.8 (#4776)
* Move cache creation outside the main thread (#4781)
* Cache: ignore hardcoded @include(if: false) directives (#4795)
* Add % to reserved characters to encode in URL (#4804)
* First drop of experimental Compose support libraries (#4783)
* Consider variable default values with @skip/@include/@defer (#4785)
* Gradle plugin: run codegen after gradle sync (#4796)
* Allow custom SqlDriver (#4806)
* Multipart subscriptions (#4768, #4807, #4738)
* GQLNode.print for type extensions (#4814)

# Version 3.7.5

_2023-03-14_

This release contains a bunch of fixes and minor improvements.

Many thanks to @adubovkin and @ndwhelan for contributing to the project, and to all the people who sent feedback! 💜

## 🐛 Bug fixes

* Fix CacheFirst + emitCacheMisses(true) (#4708)
* Fix content-length for the String constructor in MockServer (#4683)
* Fix same shape validation (#4641)
* Add a `@JsName` annotation to Operation.name() (#4643)
* Remove unreachable lenient mode from JSON writer/reader (#4656)
* Remove deprecation on connectToAndroidSourceSet because alternative have issues too (#4674)
* Fail fast if trying to set browser WebSocket headers (#4676)
* Make sure the fallback type is always last (#4692)
* Fix normalizing data when using `@include` or `@skip` with default values (#4700)
* Java codegen: fix h nameclash in hashCode (#4715)

## 🔍 Deprecation warnings (#4610)

As we're starting to work on version 4.0 which will drop support for the "compat" codegen and a few other options dating from version 2, we've added in this release some deprecation warnings that will warn when they're used. If you haven't done already, now is a good time to migrate!

## 👷‍ Other changes

* Data builders: support for `@skip` and `@include` (#4645)
* SchemaDownloader: Update to download deprecated input fields (#4678)
* Include deprecated arguments and directives in introspection (#4702)
* Update JS dependencies (#4634)

# Version 3.7.4

_2023-01-13_

This release contains a handful of bug fixes and improvements.

## 👷‍ All changes

- Kotlin codegen: automatically escape 'companion' fields (#4630)
- Runtime: fix a case where APQ + cache could be misconfigured and throw an exception (#4628)
- Update KTOR to 2.2.2 (#4627)
- Allow having an empty last part in multipart (#4598)
- Add data builders for unknown interface and union types (v3) (#4613)
- Http cache: don't access the disk from the main thread in error case (#4606)

# Version 3.7.3

_2022-12-20_

This release contains a handful of bug fixes and improvements, and also discontinues the legacy JS artifacts. 

Many thanks to @StefanChmielewski and @chao2zhang for contributing to the project! 🧡

## ⚙️ Removed JS legacy artifacts (#4591)

Historically, Kotlin Multiplatform has had 2 formats of JS artifacts: [Legacy and IR](https://kotlinlang.org/docs/js-ir-compiler.html), and Apollo Kotlin has been publishing both. However, the Legacy format is about to [be deprecated with Kotlin 1.8](https://kotlinlang.org/docs/whatsnew-eap.html#kotlin-js) and moreover we've seen [issues](https://github.com/apollographql/apollo-kotlin/issues/4590) when using the Legacy artifact in the browser. That is why starting with this release, only the IR artifacts will be published. Please reach out if this causes any issue in your project.

## 👷‍ All changes

- Add `GraphQLWsProtocol.Factory.webSocketPayloadComposer` (#4589)
- Escape "Companion" in enum value names (#4558)
- Un-break Gradle configuration cache in multi module cases (#4564)
- Move computing the `alwaysGenerateTypesMatching` to execution time (#4578)
- Log deprecation warning instead of printing (#4561)
- Escape spaces when url encoding, for Apple (#4567)
- Fix providing linker flags to the Kotlin compiler with KGP 1.8 (#4573)
- Use `service {}` in all messages/docs (#4572)
- Print all duplicate types at once (#4571)
- Fix JavaPoet formatting (#4584)
- Don't publish legacy js artifacts (#4591)

# Version 3.7.2

_2022-12-05_

This patch release brings a few fixes.

Many thanks to @davidshepherd7, @chao2zhang, @agrosner, @MyDogTom, @doucheng, @sam43 and @vincentjames501, for helping improve the library! 🙏

## 🔎‍ Explicit service declaration

Apollo Kotlin can be configured to work with [multiple services](https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration/#using-multiple-graphql-apis) and have the package name, schema files location, and other options specified for each of them. When using a single service however it is possible to omit the `service` block and set the options directly in the `apollo` block - in that case, a default service named `service` is automatically defined.

While this saves a few lines, it relies on Gradle `afterEvaluate {}` block that makes the execution of the plugin less predictable and more subject to race conditions with other plugins (see [here](https://github.com/apollographql/apollo-kotlin/issues/4523#issuecomment-1324715590) for an example).

What's more, as we move more logic to build time, the name of the service is going to be used more and more in generated code. Since [explicit is better than implicit](https://peps.python.org/pep-0020/), mandating that service name sounds a good thing to do and a warning is now printed if you do not define your service name.

To remove the warning, embed the options into a `service` block:

```diff
apollo {
+ service("service") {
    packageName.set("com.example")
    // ...
+ }
}
```

## 👷‍ All changes

* Improve "duplicate type" message by using the full path of the module (#4527)
* Fix using apollo2 and apollo3 Gradle plugins at the same time  (#4528)
* Add a warning when using the default service (#4532)
* Fix Java codegen in synthetic fields when using optionals (#4533)
* Make `canBeBatched` and `httpHeaders` orthogonal (#4534)
* Fix item wrongly removed from http cache when error in subscriptions (#4537)
* Do not throw on graphql-ws errors and instead return the errors in ApolloResponse (#4540)
* graphql-ws: send pong while waiting for connection_ack (#4555)

# Version 3.7.1

_2022-11-18_

A patch release with a few fixes.

## 👷‍ All changes

* 👷Data Builders: make DefaultFakeResolver open and stateless (#4468)
* Kotlin 1.7.21 (#4511)
* Introduce HttpFetchPolicyContext (#4509)
* Fix usedCoordinates on interfaces (#4506)

Many thanks to @Holoceo, @juliagarrigos, @davidshepherd7 and @eduardb for the feedbacks 💙 

# Version 2.5.14

_2022-11-18_

A patch release to fix an issue where the ApolloCall could end up in a bad state. Many thanks to @WilliamsDHI for diving into this 💙!

## 👷‍ All changes

* update terminate and responseCallback methods to return Optional.absent() in IDLE/TERMINATED state (#4383)

# Version 3.7.0

_2022-11-08_

This version adds multiple new low level features. These new features expose a lot of API surface, and they will probably stay experimental until 4.0. Feedback is always very welcome.    

## ✨️ [new & experimental] compiler hooks API (#4474, #4026)

Compiler hooks allow you to tweak the generated models by exposing the underlying JavaPoet/KotlinPoet structures. You can use it for an example to:

- Add a 'null' default value to model arguments ([source](https://github.com/apollographql/apollo-kotlin/blob/fb51dafaa4b02a55b6926546927d176078513543/tests/compiler-hooks/build.gradle.kts#L81))
- Introduce a common interface for all models that implement `__typename` ([source](https://github.com/apollographql/apollo-kotlin/blob/fb51dafaa4b02a55b6926546927d176078513543/tests/compiler-hooks/build.gradle.kts#L136))
- Add a prefix to generated models ([source](https://github.com/apollographql/apollo-kotlin/blob/fb51dafaa4b02a55b6926546927d176078513543/tests/compiler-hooks/build.gradle.kts#L185))
- Any other thing you can think of

To do so, make sure to use the "external" version of the plugin:

```kotlin
plugins {
  // Note: using the external plugin here to be able to reference KotlinPoet classes
  id("com.apollographql.apollo3.external")
}
```

And then register your hook to the plugin:

```kotlin
apollo {
  service("defaultnullvalues") {
    packageName.set("hooks.defaultnullvalues")
    compilerKotlinHooks.set(listOf(DefaultNullValuesHooks()))
  }
}
```

## ✨️ [new & experimental] operationBasedWithInterfaces codegen (#4370)

By default, Apollo Kotlin models fragments with synthetic nullable fields. If you have a lot of fragments, checking these fields requires using `if` statements. For an example, with a query like so:

```graphql
{
  animal {
    species
    ... on WarmBlooded {
      temperature
    }
    ... on Pet {
      name
    }
    ... on Cat {
      mustaches
    }
  }
}
```

you can access data like so:

```kotlin
if (animal.onWarmBlooded != null) {
  // Cannot smart cast because of https://youtrack.jetbrains.com/issue/KT-8819/
  println(animal.onWarmBlooded!!.temperature)
}
if (animal.onPet != null) {
  println(animal.onPet!!.name) 
}
if (animal.onCat != null) {
  println(animal.onCat!!.mustaches)
}
```

Some of the combinations could be impossible. Maybe all the pets in your schema are warm blooded. Or maybe only cat is a warm blooded. To model this better and work around [KT-8819](https://youtrack.jetbrains.com/issue/KT-8819), @chalermpong implemented a new codegen that adds a base sealed interface. Different implementations contain the same synthetic fragment fields as in the default codegen except that their nullability will be updated depending the branch:

```kotlin
when (animal) {
  is WarmBloodedPetAnimal -> {
    println(animal.onWarmBlooded!!.temperature)
    println(animal.onPet!!.name)
  }
  is PetAnimal -> {
    // Some pet that is not warm blooded, e.g. a Turtle maybe?
    println(animal.onPet!!.name)
  }
  is OtherAnimal -> {
    println(animal.species)
  }
  // Note how there is no branch for Cat because it's a WarmBloodedPetAnimal
  // Also no branch for WarmBlooded animal because all pets in this (fictional) sample schema are WarmBlooded. This could be different in another schema
}
```

To try it out, add this to your Gradle scripts:

```kotlin
apollo {
  codegenModels.set("experimental_operationBasedWithInterfaces") 
}
```

Many many thanks to @chalermpong for diving into this 💙

## ✨️ [new & experimental] usedCoordinates auto detection (#4494)

By default, Apollo Kotlin only generates the types that are used in your queries. This is important because some schemas are really big and generating all the types would waste a lot of CPU cycles. In multi-modules scenarios, the codegen only knows about types that are used locally in that module. If two sibling modules use the same type and that type is not used upstream, that could lead to errors like this:

```
duplicate Type '$Foo' generated in modules: feature1, feature2
Use 'alwaysGenerateTypesMatching' in a parent module to generate the type only once
```

This version introduces new options to detect the used types automatically. It does so by doing a first pass at the GraphQL queries to determine the used type. Upstream modules can use the results of that computation without creating a circular dependency. To set up auto detection of used coordinates, configure your schema module to get the used coordinates from the feature module using the `apolloUsedCoordinates` configuration:

```kotlin
// schema/build.gradle.kts
dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  // Get the used coordinates from your feature module
  apolloUsedCoordinates(project(":feature"))
  // If you have several, add several dependencies
  apolloUsedCoordinates(project(":feature-2"))
}

apollo {
  service("my-api") {
    packageName.set("com.example.schema")
    generateApolloMetadata.set(true)
  }
}
```

And in each of your feature module, configure the `apolloSchema` dependency:

```kotlin
// feature/build.gradle.kts
dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  // Depend on the codegen from the schema
  apolloMetadata(project(":schema"))
  // But also from the schema so as not to create a circular dependency
  apolloSchema(project(":schema"))
}

apollo {
  // The service names must match
  service("my-api") {
    packageName.set("com.example.feature")
  }
}
```

## 👷‍ All changes

* Add usedCoordinates configuration and use it to automatically compute the used coordinates (#4494)
* Compiler hooks (#4474)
* 🐘 Use `registerJavaGeneratingTask`, fixes lint trying to scan generated sources  (#4486)
* Rename `generateModelBuilder` to `generateModelBuilders` and add test (#4476)
* Data builders: only generate used fields (#4472)
* Only generate used types when generateSchema is true (#4471)
* Suppress deprecation warnings, and opt-in in generated code (#4470)
* Multi-module: fail if inconsistent generateDataBuilders parameters (#4462)
* Add a decapitalizeFields option (#4454)
* Pass protocols to WebSocket constructor in `JSWebSocketEngine` (#4445)
* SQLNormalized cache: implement selectAll, fixes calling dump() (#4437)
* Java codegen: Nullability annotations on generics (#4419)
* Java codegen: nullability annotations (#4415)
* OperationBasedWithInterfaces (#4370)
* Connect test sourceSet only when testBuilders are enabled (#4412)
* Java codegen: add support for Optional or nullable fields (#4411)
* Add generatePrimitiveTypes option to Java codegen (#4407)
* Add classesForEnumsMatching codegen option to generate enums as Java enums (#4404)
* Fix data builders + multi module (#4402)
* Relocate the plugin without obfuscating it (#4376)

# Version 3.6.2

_2022-10-05_

A patch version to fix compatibility with Kotlin 1.7.20 and another fix when calling `ApolloStore.dump()` with the SQL normalized cache.

## 👷‍ All changes

* Add support for KGP 1.7.20 (#4439)
* Fix SQLNormalizedCache.dump() (#4437)
 
# Version 3.6.1

_2022-10-03_

A patch version to fix an issue with data builder and multi modules. Many thanks to @agrosner and @eduardb for catching this.

## 👷‍ All changes

* Fix data builders in multi-modules scenarios (#4402)

# Version 3.6.0

_2022-09-08_

This version brings initial support for `@defer` as well as data builders.

## 💙️ External contributors

Many thanks to @engdorm, @Goooler, @pt2121 and @StylianosGakis for their contributions!

## ✨️ [new] Kotlin 1.7 (#4314)

Starting with this release, Apollo Kotlin is built with Kotlin 1.7.10. This doesn't impact Android and JVM projects (the minimum supported version of Kotlin continues to be 1.5) but if you are on a project using Native or JS, you will need to update the Kotlin version to 1.7.0+.

## ✨️ [new] `@defer` support

`@defer` support is experimental in the Kotlin Client and currently a [Stage 2 GraphQL specification draft](https://github.com/graphql/graphql-spec/blob/main/CONTRIBUTING.md#stage-2-draft) to allow incremental delivery of response payloads. 

`@defer` allows you to specify a fragment as deferrable, meaning it can be omitted in the initial response and delivered as a subsequent payload. This improves latency for all fields that are not in that fragment. You can read more about `@defer` in the [RFC](https://github.com/graphql/graphql-wg/blob/main/rfcs/DeferStream.md) and contribute/ask question in the [`@defer` working group](https://github.com/robrichard/defer-stream-wg).

Apollo Kotlin supports `@defer` by default and will deliver the successive payloads as `Flow` items. Given the below query:

```graphql
query GetComputer {
  computer {
    __typename
    id
    ...ComputerFields @defer
  }
}

fragment ComputerFields on Computer {
  cpu
  year
  screen {
    resolution
  }
}
```

And the following server payloads:

**payload 1**:
```json
{
  "data": {
    "computer": {
      "__typename": "Computer",
      "id": "Computer1"
    }
  },
  "hasNext": true
}
```

**payload 2**:
```json
{
  "incremental": [
    {
      "data": {
        "cpu": "386",
        "year": 1993,
        "screen": {
          "resolution": "640x480"
        }
      },
      "path": [
        "computer",
      ]
    }
  ],
  "hasNext": true
}
```

You can listen to payloads by using `toFlow()`:

```kotlin
apolloClient.query(query).toFlow().collectIndexed { index, response ->
  // This will be called twice

  if (index == 0) {
    // First time without the fragment
    assertNull(response.data?.computer?.computerFields)
  } else if (index == 1) {
    // Second time with the fragment
    assertNotNull(response.data?.computer?.computerFields)
  }
}
```

You can read more about it in [the documentation](https://www.apollographql.com/docs/kotlin/fetching/defer).

As always, feedback is very welcome. Let us know what you think of the feature by
either [opening an issue on our GitHub repo](https://github.com/apollographql/apollo-kotlin/issues)
, [joining the community](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
or [stopping by our channel in the KotlinLang Slack](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)(get your
invite [here](https://slack.kotl.in/)).

## ✨️ [new] Data Builders (#4321)

Apollo Kotlin 3.0 introduced [test builders](https://www.apollographql.com/docs/kotlin/testing/test-builders/). While they are working, they have several limitations. The main one was that being [response based](https://www.apollographql.com/docs/kotlin/advanced/response-based-codegen#the-responsebased-codegen), they could generate a lot of code. Also, they required passing custom scalars using their Json encoding, which is cumbersome. 

The [data builders](https://www.apollographql.com/docs/kotlin/testing/data-builders/) are a simpler version of the test builders that generate builders based on schema types. This means most of the generated code is shared between all your implementations except for a top level `Data {}` function in each of your operation:

```kotlin
// Replace
val data = GetHeroQuery.Data {
  hero = humanHero {
    name = "Luke"
  }
} 

// With
val data = GetHeroQuery.Data {
  hero = buildHuman {
    name = "Luke"
  }
} 
```

## 👷‍ All changes

* fix registering Java scalars. Many thanks @parker for catching this. (#4375)
* Data builders (#4359, #4338, #4331, #4330, #4328, #4323, #4321)
* Add a flag to disable fieldsCanMerge validation on disjoint types (#4342)
* Re-introduce @defer and use new payload format (#4351)
* Multiplatform: add enableCompatibilityMetadataVariant flag (#4329)
* Remove an unnecessary `file.source().buffer()` (#4326)
* Always use String for defaultValue in introspection Json (#4315)
* Update Kotlin dependency to 1.7.10 (#4314)
* Remove schema and AST from the IR (#4303)

# Version 3.5.0 

_2022-08-01_

With this release, Apollo Kotlin now uses Kotlin Native's new memory model. It also contains a number of other improvements and bug fixes. 

## 💙️ External contributors

Many thanks to `@glureau` for carefully adding new watch targets ⌚💙.

## ✨️ [new] Kotlin Native: new memory manager (#4287)

Apollo Kotlin is now requiring applications to use the [new memory manager, a.k.a. new memory model](https://blog.jetbrains.com/kotlin/2021/08/try-the-new-kotlin-native-memory-manager-development-preview/). Thanks to this change, the restriction that operations had to be executed from the main thread on Apple targets is now removed. You can also use [kotlinx.coroutines.test.runTest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html). Last but not least, benchmarks seem to [indicate](https://github.com/apollographql/apollo-kotlin/pull/4287/files?w=1#diff-aead75359419ef1647be74310bd7093e4cc2d9393917d17029d5fcc5e11ce1ef) that performance is better under the new memory manager!

## ✨️ [new] `@targetName` directive (#4243)

This directive was introduced in v3.3.1 to allow overriding the name of enum values in the generated code. It has now been extended to allow configuring the generated name of Interfaces, Enums, Unions, Scalars and Input objects. This can be used to make the generated code nicer to use, or to avoid name clashes with Kotlin types (e.g. `Long`) in Kotlin Native.

## ✨️ [new] Automatic resolution of Apollo artifacts versions from the plugin version (#4279)

From now on, you no longer need to specify explicitly the versions of Apollo dependencies: if omitted, the same version as the Apollo Gradle plugin will be used. This should facilitate upgrades and avoid potential mistakes:

```kotlin
plugins {
  plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("com.apollographql.apollo3") version "3.5.0"
  }

  dependencies {
    // Replace this
    // implementation("com.apollographql.apollo3:apollo-runtime:3.5.0")
    
    // with
    implementation("com.apollographql.apollo3:apollo-runtime")
  }
}

```

## 🚧 [deprecated] `runTest` (#4292)

With the new memory model, Apollo's specific `runTest` method from `apollo-testing-support` is no longer useful and has been deprecated. If you were using it, you should now be able to use [Kotlin's `runTest`](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-test/README.md) instead, or simply `runBlocking`.


## 🚧 [breaking] Automatic detection of `type` enum values.

If you have an enum with a `type` value, this value used to name clash with the generated `type` property. This version now detects this case automatically and escapes `type` to `type_`. If you had previously used `@targetName` to workaround this issue, you can now remove it to avoid it being escaped twice:

```graphql 
# Remove this
extend enum SomeEnum {
  type @targetName(name: "type_")
}
```

## 👷‍ All changes

- Support watchosArm32 (#4260)
- Support @targetName on Interfaces, Enums, Unions, Scalars and Input objects (#4243)
- 🐘  support lazy APIs for newer AGP versions (#4264)
- Pagination: add connectionFields argument to @typePolicy (#4265)
- 🐘 Get the dependencies version from the plugin automagically (#4279)
- Automatically escape `type` in enum values (#4295)
- Fix inferred variables in both nullable and non-nullable locations (#4306)
- Native: assume New Memory Manager (#4287)
- Use internal runTest in all tests (#4292)

# Version 3.4.0

_2022-07-11_

This release contains a few important bug fixes (#4214, #4224, #4247, #4256), makes it possible to compile with Gradle
7.4 and `apollo-gradle-plugin` (#4218).

It also introduces `-incubating` cache artifacts.

## 💙️ External contributors

Many thanks to @ArjanSM, @zebehringer, @mm-kk-experiments, @mune0903, @stengvac, @elenigen, @shamsidinb,
@StylianosGakis for the awesome contributions 😃!

## ✨️ [new] `-incubating` cache artifacts.

This version introduces the below artifacts:

- `apollo-normalized-cache-incubating`
- `apollo-normalized-cache-api-incubating`
- `apollo-normalized-cache-sqlite-incubating`

These artifacts introduce new APIs to work with cache expiration and pagination (as well
as [other cache improvements](https://github.com/apollographql/apollo-kotlin/issues/2331) in the future).

These artifacts have no backward compatibility guarantees and most likely have worse performance than the non-incubating
artifacts. Documentation will be added once the API stabilize. In the short term, the best place to look for examples
are the integration tests:

- [Expiration tests](https://github.com/apollographql/apollo-kotlin/blob/363c73d89f1f0bbe773e98fbfac873c8b93b666d/tests/sqlite/src/commonTest/kotlin/ExpirationTest.kt)
- [Pagination tests](https://github.com/apollographql/apollo-kotlin/tree/d5b4fc3f12087e8a3585cd63531ccc590dee3098/tests/pagination/src/commonTest/kotlin)

_Note_: The experimental `withDates: Boolean` argument was introduced in 3.3.1 in the regular artifacts and is removed
as part of this release. Use the `-incubating` artifacts to use it.

## 👷‍ All changes

- add TrimmableNormalizedCacheFactory (#4239)
- 🚧 remove withDates (#4257)
- 🗄️ Chunk parameters in large responses (#4256)
- Fix for improper handling of JsonNumber in BufferedSinkJsonWriter (#4247)
- Incubating modules for the next gen cache (#4241)
- Pagination: fixes in FieldRecordMerger and MemoryCache (#4237)
- make it possible to reuse a File Upload (#4228)
- Persist Record arguments/metadata with the SQL Json backend (#4211)
- requestedDispatcher -> dispatcher (#4220)
- Fix test errors were emitted outside the Flow (#4224)
- Make it possible to compile with Kotlin 1.5 and apollo-gradle-plugin (#4218)
- 🏖️ Relax MapJsonReader endObject, fixes reading inline + named fragments with compat models (#4214)
- Introduce RecordMerger (#4197)
- Add @typePolicy(embeddedFields: String! = "") (#4196)

# Version 3.3.2

_2022-06-17_

This is a hot fix release that fixes a crash that could happen in the codegen when using `responseBased` codegen in a
multimodule setup. It also includes a fix for incorrect generated code when using certain reserved names in enum values.

## 👷‍ All changes

- Update to KotlinPoet `1.12.0`, fixes generating enum values whose name clashes with other symbols (#4034)
- Update to Ktor 2 (#4190)
- Fix NPE in checkCapitalizedFields (#4201)

# Version 3.3.1

_2022-06-13_

This release introduces `@typePolicy` on interface/enums, improvements on subscription error handling, and on Test
Builders. It also contains a number of other improvements and bug fixes!

## ✨️ [new] `@typePolicy` on interfaces and unions (#4131)

[The `@typePolicy` directive](https://www.apollographql.com/docs/kotlin/caching/declarative-ids#typepolicy) can now be
declared on interfaces and unions. Thank you @bubba for the contribution!

## 🔌 WebSockets / Subscriptions error handling (#4147)

An issue where `websocketReopenWhen` was not called in some cases was fixed. Also, this release
introduces `SubscriptionOperationException`. A `SubscriptionOperationException` will be thrown instead of the more
generic `ApolloNetworkError` if a subscription fails due to a specific operation error.

## 📐 Test Builders improvements and fixes

* A DslMarker was added to improve usage with nested builders (#4089)
* When calling a builder, but not assigning it to a field, an error is now thrown, preventing mistakes (#4122)
* The error message displayed when `__typename` is missing was made clearer (#4146)
* Fix: use `rawValue` instead of `name` for enums (#4121)

## ✨️ [new] ApolloClient implements Closable (#4142)

`ApolloClient` now implements `okio.Closable` so you can
use [`use`](https://square.github.io/okio/3.x/okio/okio/okio/use.html) with it. Thanks @yogurtearl for this
contribution!

## ✨️ [new] experimental `@targetName` directive on enum values (#4144)

If an enum value name is clashing with a reserved name (e.g. `type`) you can now use this directive to instruct the
codeGen to use the specified name for the value instead. This directive is experimental for now.

## ✨️ [new] experimental support for renaming directives (#4174)

As we add more client directives, the risk of nameclash with existing schema directives increases. If this happens, you
can now import Apollo client directives using `@link`:

```graphql
# extra.graphqls
extend schema @link(url: "https://specs.apollo.dev/kotlin_labs/v0.1") 
```

This adds a `@kotlin_labs__` prefix to all Apollo client directives:

```graphql
{
  hero {
    name @kotlin_labs__nonnull
  }
}
```

## 🤖 `SqlNormalizedCacheFactory` initialization on Android (#4104)

It is no longer necessary to pass a `Context` when initializing the `SqlNormalizedCacheFactory` on Android. A `Context`
is automatically provided, via [App Startup](https://developer.android.com/topic/libraries/app-startup).

```kotlin
// Before
val sqlNormalizedCacheFactory = SqlNormalizedCacheFactory(context, "apollo.db")

// After
val sqlNormalizedCacheFactory = SqlNormalizedCacheFactory("apollo.db")
```

## 📝 [new] Public API tracking

This release starts tracking the public API of all modules, including MockServer. Even if the API remains experimental,
we'll try to keep the number of breaking changes low in the future.

## 👷‍ All changes

- 🐘 publish `apollo-gradle-plugin-external` (#4078)
- publish the R8 mapping file along the relocated jar (#4085)
- Fix test directories not cleared (#4083)
- Do not use 'header' as a enum value name as it breaks the Kotlin compiler (#4086)
- 🧪 @experimental support (#4091)
- @experimental -> @requiresOptIn (#4175)
- Do not buffer entire body in Http Cache (#4076)
- ⬇️ add SchemaDownloader.download() (#4088)
- add DslMarker for test builders (#4089)
- MockServer: make MockResponse.body a Flow<ByteString> (#4096)
- Issue-3909: add ApolloResponse cache headers (#4102)
- Use rawValue instead of name for enums in test builders (#4121)
- 💧 first drop for a SQLite backend that stores when each field was last updated (#4104)
- Add Operation.Data.toJsonString() convenience function for the jvm (#4124)
- Check for unassigned fields in Test Builders (#4122)
- Add non-breaking spaces after 'return' (#4127)
- 🧶 Use a getter instead of a const val OPERATION_QUERY (#4130)
- Uploads should be read only once even when logging (#4125)
- Keep the 'interfaces' field on the JSON introspection (#4129)
- Allow @typePolicy directive on interfaces and unions (#4131)
- typePolicy on interface: exclude empty keyfields (#4140)
- Sort the type names in the list so the code gen is deterministic. (#4138)
- Use okio.Closable.close instead of dispose on ApolloClient (#4142)
- Parse the interface's interface field in introspection (#4143)
- TestBuilders: improve error message when __typename is missing (#4146)
- Do not bypass websocketReopenWhen {} (#4147)
- SDLWriter: join implemented interfaces with & instead of space (#4151)
- Escape "type" in enums and sealed classes (#4144)
- 🧰 introduce apollo-tooling and apollo-cli (#4153)
- Fix incorrect content-length in MockServer (#4162)
- Allow capitalized field names if flattenModels is true (#4154)
- 🏷️ Allow namespacing and renaming of directives (#4174)

## ❤️ External contributors

Many thanks to @tajchert, @asimonigh, @hrach, @ArjanSM, @yshrsmz, @ephemient, @bubba, @eboudrant and @yogurtearl for
contributing to this release! 🙏

# Version 3.3.0

_2022-05-04_

This is the first release with [HMPP](https://kotlinlang.org/docs/multiplatform-hierarchy.html) support. If you're using
multiplatform, updating to Kotlin 1.6.21 is strongly encouraged.

This release also brings WebSocket related improvements and other fixes!

## ✨️ [new] Hierarchical MultiPlatform Project (HMPP) (#4033)

When using Apollo Kotlin on a multiplatform project, this release is compatible with
the [hierarchical project structure](https://kotlinlang.org/docs/multiplatform-hierarchy.html), which makes it easier to
share common code among several targets. Using HMPP in your project also fixes some issues when compiling Kotlin
metadata. See https://github.com/apollographql/apollo-kotlin/issues/4019
and https://youtrack.jetbrains.com/issue/KT-51970/ for more details.

**✋ Note**: If you're using multiplatform, we strongly encourage updating to Kotlin 1.6.21. If that is not an option,
you might have issues resolving dependencies. More infos
in [this issue](https://github.com/apollographql/apollo-kotlin/issues/4095#issuecomment-1123571706).

## ✨️ [new] `WebSocketNetworkTransport.closeConnection` (#4049)

This new method can be used in conjunction
with [`reopenWhen`](https://apollographql.github.io/apollo-kotlin/kdoc/apollo-runtime/com.apollographql.apollo3/-apollo-client/-builder/web-socket-reopen-when.html)
to force a reconnection to the server. This could be useful for instance when needing to pass new auth tokens in the
headers. If you were using `subscriptionManager.reconnect()` in 2.x, `closeConnection` is a simple way to achieve the
same behaviour.

## ✨️ [new] `GraphQLWsProtocol.connectionPayload` is now a lambda (#4043)

With `GraphQLWsProtocol`, if you need to pass parameters to the connection payload, previously you would pass them as a
static map to the builder. With this change you can now pass a lambda providing them as needed. This facilitates passing
fresh auth tokens when connecting.

## ✨️ [new] Add insecure option to download schema (#4021)

You can now use the `--insecure` flag when downloading a schema
with [`downloadApolloSchema`](https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration/#downloading-a-schema)
, to bypass the certificate check, which can be useful if a server is configured with a self-signed certificate for
instance.

## 👷‍ All changes

- Add WebSocketNetworkTransport.closeConnection (#4049)
- Made connectionPayload as suspend function in GraphQLWsProtocol (#4043)
- ⚡ Ignore unknown websocket messages (#4066)
- Kotlin 1.6.21 & HMPP (#4033)
- Provide a Content-Length when using Upload (#4056)
- ☁️ add HttpRequest.newBuilder(url, method) (#4038)
- Escape enum constants (#4035)
- Fix the Moshi adapter used for OperationOutput. Moshi cannot get the type parameters from the typealias
  automagically (#4022)
- Add insecure option to download schema (#4021)
- Try to reduce allocations in MapJsonReader (#3935)
- 🔒 Deprecate BearerTokenInterceptor and provide tests and docs instead (#4068)

## ❤️ External contributors

Many thanks to @CureleaAndrei and @kdk96 for contributing to this release! 🙏

## ⚙️ Deprecations

- `BearerTokenInterceptor` was provided as an example but is too simple for most use cases, and has therefore been
  deprecated
  in this release. [This page](https://www.apollographql.com/docs/kotlin/advanced/authentication) provides more details
  about authentication.
- The previous ways of passing parameters to the connection payload with `GraphQLWsProtocol` has been deprecated (see
  above).

# Version 3.2.2

_2022-04-11_

A maintenance release to fix the `addJvmOverloads` option added in 3.2.0 as well as other fixes. If you're using APQs,
the mutations are now always send using `POST`.
See [#4006](https://github.com/apollographql/apollo-kotlin/issues/4006#issuecomment-1092628783) for details and a way to
override the behaviour if you really need to.

Many thanks to @benedict-lim, @olivierg13, @konomae and @sproctor for their contributions 💙

## 👷‍ All changes

* Use a constant for JvmOverloads to avoid a crash due to relocation (#4008)
* Always use POST for Mutations in APQs (Auto Persisted Queries) (#4011)
* Add configurable headers to WebSocketNetworkTransport (#3995)
* Handle SqlNormalizedCache merge APIs Exceptions with ApolloExceptionHandler (#4002)
* Add adapter for java.time.OffsetDateTime (#4007)
* ⏰ Add tests for date adapters (#3999)
* Fix wrong LocalDate and LocalDateTime formats in JavaTimeAdapters (#3997)

# Version 3.2.1

_2022-04-05_

This release introduces a few improvements and bug fixes.

## ✨️ [new] `ApolloCall<D>.emitCacheMisses(Boolean)` (#3980)

When observing the cache with `watch`, the behavior was to not emit cache misses at all, which may not desirable in
certain cases. With this new option, you can now choose to emit them: in that case responses will be emitted with a
null `data`.

This can be used like so:

```kotlin
apolloClient.query(query)
    .fetchPolicy(FetchPolicy.CacheOnly)
    .emitCacheMisses(true)
    .watch()
    .collect { response ->
      // response.data will be null in case of cache misses
    }
```

This is also closer to the behavior that was in place in v2. Many thanks to @mateuszkwiecinski for the insights and
raising the issue!

## ⚙️ [breaking] Allow configuration of frame types used in `SubscriptionWsProtocol` and default to Text (#3992)

When using subscriptions over WebSockets with `SubscriptionWsProtocol` (the default), the frames were sent in the binary
format. It was reported that this was not compatible with certain servers ([DGS](https://netflix.github.io/dgs)
, [graphql-java-kickstart](https://github.com/graphql-java-kickstart/graphql-spring-boot)) that are expecting text
frames. This is now fixed and the default is to send text frames.

> ⚠️ This may be a breaking change if your server expects binary frames only!

If that is the case, you can use the new `frameType` option to configure the frame type to be sent:

```kotlin
client = ApolloClient.Builder()
    .webSocketServerUrl("wss://...")
    .wsProtocol(GraphQLWsProtocol.Factory(frameType = WsFrameType.Binary))
    .build()
```

Many thanks to @Krillsson and @aviewfromspace1 for the insights and raising the issue!

## 👷‍ All changes

* Allow configuration of frame types used in SubscriptionWsProtocol and default to Text (#3992)
* add `ApolloRequest.newBuilder(operation: Operation<E>)`  (#3988)
* Add exception handlers to ApolloCacheInterceptor and SqlNormalizedCache (#3989)
* 📠 Fix some @DeprecatedSince annotations (#3983)
* 👓 add ApolloCall<D>.emitCacheMisses(Boolean) (#3980)
* ⚙️ Fix fragments on the root query type in operationBased codegen (#3973)

## ❤️ External contributors

Many thanks to @AdamMTGreenberg and @Krillsson for the contributions! 🙏

# Version 3.2.0

_2022-03-29_

💙 Thanks to @undermark5, @demoritas, @rkoron007, @akshay253101, @StylianosGakis, @Goooler, @jeffreydecker, @theBradfo,
@anderssandven and @olivierg13 for contributing to this release.

This version adds JS WebSocket support, more options to deal with `__typename` amongst other features and bugfixes.

## ✨️ [new] JS WebSocket support (#3913)

Version 3.2.0 now has WebSocket support for Javascript targets courtesy of @undermark5! This is a huge milestone and
means the JS target is now even closer to its JVM and iOS counterparts.

|  | `jvm` | Apple | `js` | `linuxX64`
| --- | :---: |:-----:|:----:| :---: |
| `apollo-api` (models)|✅|   ✅   |  ✅   |✅|
| `apollo-runtime` (network, query batching, apq, ...) |✅|   ✅   |  ✅   |🚫|
| `apollo-normalized-cache` |✅|   ✅   |  ✅   |🚫|
| `apollo-adapters` |✅|   ✅   |  ✅   |🚫|
| `apollo-normalized-cache-sqlite` |✅|   ✅   |  🚫  |🚫|
| `apollo-http-cache` |✅|  🚫   |  🚫  |🚫|

The implementation is based on the [`ws`](https://github.com/websockets/ws) library on Node and
the [`WebSocket` API](https://websockets.spec.whatwg.org//) on the browser and inspired by [Ktor](https://ktor.io/).

## ✨️ [new] Fine grained `__typename` control (#3939)

This version generates non-nullable fragments when it knows the fragment is always present:

```graphql
{
  cat {
    # Because Animal is a supertype of Cat this condition will always be true
    ... on Animal {
      species
    }
  }
}
```

In addition, it introduces a `addTypename` Gradle option to have better control over when to add the `__typename` field:

```kotlin
/**
 * When to add __typename. One of "always", "ifFragments", "ifAbstract" or "ifPolymorphic"
 *
 * - "always": Add '__typename' for every compound field
 *
 * - "ifFragments": Add '__typename' for every selection set that contains fragments (inline or named)
 * This is adding a lot more '__typename' than the other solutions and will be certainly removed in
 * a future version. If you require '__typename' explicitly, you can add it to your queries.
 * This causes cache misses when introducing fragments where no fragment was present before and will be certainly removed in
 * a future version.
 *
 * - "ifAbstract": Add '__typename' for abstract fields, i.e. fields that are of union or interface type
 * Note: It also adds '__typename' on fragment definitions that satisfy the same property because fragments
 * could be read from the cache and we don't have a containing field in that case.
 *
 * - "ifPolymorphic": Add '__typename' for polymorphic fields, i.e. fields that contains a subfragment
 * (inline or named) whose type condition isn't a super type of the field type.
 * If a field is monomorphic, no '__typename' will be added.
 * This adds the bare minimum amount of __typename but the logic is substantially more complex and
 * it could cause cache misses when using fragments on monomorphic fields because __typename can be
 * required in some cases.
 *
 * Note: It also adds '__typename' on fragment definitions that satisfy the same property because fragments
 * could be read from the cache and we don't have a containing field in that case.
 *
 * Default value: "ifFragments"
 */
```

You can read more in the
corresponding [Typename.md](https://github.com/apollographql/apollo-kotlin/blob/main/design-docs/Typename.md) design
document.

## ✨️ [new] Maven publishing for multi-module apollo metadata (#3904)

The Apollo Gradle plugin now creates a new "apollo" publication if `maven-publish` is found. This means you can now
publish the Apollo metadata to a maven repository:

```bash
# In your producer project
./gradlew publishApolloPublicationTo[SomeRepository]
``` 

Assuming your producer project is using `com.example:project:version` for maven coordinates, the Apollo metadata will be
published at `com.example:project-apollo:version`:

```kotlin
// In your consumer project
dependencies {
  implementation("com.example:project:version")
  apolloMetadata("com.example:project-apollo:version")
}
```

**Note**: There are absolutely no forward/backward compatibility guarantees for Apollo metadata yet. The Apollo version
used in the consumer **must** be the same as the one used in the producer.

## ✨️ [new] `addJvmOverloads` Gradle option (#3907)

For better Java interop, you can now opt-in `addJvmOverloads`. `addJvmOverloads` will add the `@JvmOverloads` to your
Kotlin operations:

```kotlin
@JvmOverloads
class GetHeroQuery(val id: String, val episode: Optional<Episode> = Optional.Absent) {
  // ...
}
```

Meaning you can now create a new query from Java without having to specify `episode`: `new GetHeroQuery("1002")`

## 👷‍ All changes

* 📖 Add note to tutorial about `graphql-ws` library to tutorial (#3961)
* Use ApolloCompositeException for HTTP CachePolicies (#3967)
* 🖋️ bump kotlin poet to 1.11.0 (#3970)
* Add underlying exceptions as suppressed exceptions in ApolloCompositeException (#3957)
* Add macosArm64 and macosX64 targets (#3954)
* JS Websockets: handle remote close (#3952)
* ⚙️ Introduce addTypename Gradle parameter (#3939)
* Optimize CI a bit (#3942)
* Add more field merging diagnostics (#3937)
* ⚙️ Make adapters code work without relying on having a` __typename` IrProperty (#3930)
* Add equals and hashCode implementations for models with no properties (#3928)
* 🐘 Unbreak Gradle configuration cache (#3918)
* WebSocket support for JS targets (#3913)
* 🗄️ add apolloClient.httpCache (#3919)
* ⚙️ Detect case insensitive filesystems (like MacOS default one) and rename classes when that happens (#3911)
* Fix exceptions where not caught when reading the body of a batched query (#3910)
* 🗄️ Fix writing fragments programmatically was using the wrong cache key (#3905)
* 📦 Maven publishing for Apollo metadata (#3904)
* Add addJvmOverloads Gradle option for better Java interop (#3907)
* 🐘 Fix using refreshVersions (#3898)
* add support for triple quotes escapes (#3895)
* 👷 Test Builders: Fix enums in test resolver (#3894)
* Validation: Detect missing arguments when there are no arguments at all (#3893)
* Add support for receiving multiple bodies with multipart (#3889)
* ✅ Validation: allow nullable variables in non-null locations if there is a default value (#3879)
* 🗄️ HttpCache: do not cache mutations by default (#3873)
* Chunked Transfer-Encoding support in MockServer (#3870)
* Fix -1 body length in BatchingHttpInterceptor (#3874)
* Fix issue in Java codegen where selectors returned ImmutableMapBuilder instances instead of Map (#3861)
* Make watchers subscribe to the store earlier (#3853)

# Version 3.1.0

_2022-02-07_

Version 3.1.0 introduces new APIs for testing, mapping scalars as well a redesigned cache pipeline.
It also contains bugfixes around the `@include` directives, MemoryCache and GraphQL validation amongst other changes.

## ⚙️ [breaking] Fragment package name and `useSchemaPackageNameForFragments` (#3775)

If you're using `packageNamesFromFilePaths()`, the package name of generated fragment classes has changed.

Different generated types have different package names:

* Generated types coming from operations are generated based on the operation path
* Generated types coming from the schema (input objects, custom scalars and enums) are generated based on the schema
  path

Previously, fragments were using the schema path which is inconsistent because fragments are not defined in the schema
but are executable files, like operations.

Version 3.1.0 now uses the same logic for fragments as for operations. To revert to the previous behaviour, you can
use `useSchemaPackageNameForFragments`:

```kotlin
apollo {
  useSchemaPackageNameForFragments.set(true)
}
```

This is also done automatically if you're using `useVersion2Compat()`. Moving forward, the plan is to
remove `useSchemaPackageNameForFragments` in favor of setting a custom `PackageNameGenerator`. If you have use cases
that require `useSchemaPackageNameForFragments`,
please [reach out](https://github.com/apollographql/apollo-kotlin/issues/new?assignees=&labels=%3Aquestion%3A+Type%3A+Question&template=question.md&title=)
.

## ✨ [New] `QueueTestNetworkTransport` (#3757)

3.1.0 introduces `QueueTestNetworkTransport` to test at the GraphQL layer without needing to run an HTTP server.

To use it, configure your `ApolloClient`:

```kotlin
// This uses a QueueTestNetworkTransport that will play the queued responses
val apolloClient = ApolloClient.Builder()
    .networkTransport(QueueTestNetworkTransport())
    .build()
```

You can then use the `enqueueTestResponse` extension function to specify the GraphQL responses to return:

```kotlin
val testQuery = GetHeroQuery("001")
val testData = GetHeroQuery.Data {
  hero = droidHero {
    name = "R2D2"
  }
}
apolloClient.enqueueTestResponse(testQuery, testData)
val actual = apolloClient.query(testQuery).execute().data!!
assertEquals(testData.hero.name, actual.hero.name)
```

## ✨ [New] `MockServerHandler` (#3757)

If you're testing at the HTTP layer, you can now define your own `MockServerHandler` to customize how the server is
going to answer to requests:

```kotlin
val customHandler = object : MockServerHandler {
  override fun handle(request: MockRequest): MockResponse {
    return if (/* Your custom logic here */) {
      MockResponse(
          body = """{"data": {"random": 42}}""",
          headers = mapOf("X-Test" to "true"),
      )
    } else {
      MockResponse(
          body = "Internal server error",
          statusCode = 500,
      )
    }
  }
}
val mockServer = MockServer(customHandler)
```

## ✨ [New] `FetchPolicy.CacheAndNetwork` (#3828)

Previously, `FetchPolicy`s were limited to policies that emitted at most **one** response. There was
a `executeCacheAndNetwork()` method but it felt asymmetrical. This version introduces `FetchPolicy.CacheAndNetwork` that
can emit up to two responses:

```kotlin
apolloClient.query(query)
    // Check the cache and also use the network (1 or 2 values can be emitted)
    .fetchPolicy(FetchPolicy.CacheAndNetwork)
    // Execute the query and collect the responses
    .toFlow().collect { response ->
      // ...
    }
```

## ✨ [New] `ApolloCall<D>.fetchPolicyInterceptor(interceptor: ApolloInterceptor)` (#3743)

If you need more customized ways to fetch data from the cache or more fine-grained error handling that does not come
with the built-in `FetchPolicy`, you can now use `fetchPolicyInterceptor`:

```kotlin
// An, interceptor that will only use the network after getting a successful response
val refetchPolicyInterceptor = object : ApolloInterceptor {
  var hasSeenValidResponse: Boolean = false
  override fun <D : Operation.Data> intercept(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain
  ): Flow<ApolloResponse<D>> {
    return if (!hasSeenValidResponse) {
      CacheOnlyInterceptor.intercept(request, chain).onEach {
        if (it.data != null) {
          // We have valid data, we can now use the network
          hasSeenValidResponse = true
        }
      }
    } else {
      // If for some reason we have a cache miss, get fresh data from the network
      CacheFirstInterceptor.intercept(request, chain)
    }
  }
}

apolloClient.query(myQuery)
    .refetchPolicyInterceptor(cacheOnlyInterceptor)
    .watch()
    .collect {
      //
    }
```

## ✨ [New] `Service.mapScalar` Gradle API (#3779)

You can now use `mapScalar` to specify your scalar mappings:

```kotlin
apollo {
  // Replace 
  customScalarsMapping.set(mapOf(
      "Date" to "java.util.Date"
  ))

  // With
  mapScalar("Date", "java.util.Date")
}
```

`mapScalar` also works with built-in scalar types so you can map the `ID` type to a kotlin Long:

```kotlin
apollo {
  // This requires registering an adapter at runtime with `addCustomScalarAdapter()` 
  mapScalar("ID", "kotlin.Long")
}
```

As an optimization, you can also provide the adapter at compile time. This will avoid a lookup at runtime everytime such
a scalar is read:

```kotlin
apollo {
  // No need to call `addCustomScalarAdapter()`, the generated code will use the provided adapter 
  mapScalar("ID", "kotlin.Long", "com.apollographql.apollo3.api.LongAdapter")
}
```

For convenience, a helper function is provided for common types:

```kotlin
apollo {
  // The generated code will use `kotlin.Long` and the builtin LongAdapter 
  mapScalarToKotlinLong("ID")

  // The generated code will use `kotlin.String` and the builtin StringAdapter
  mapScalarToKotlinString("Date")

  // The generated code will use `com.apollographql.apollo3.api.Upload` and the builtin UploadAdapter
  mapScalarToUpload("Upload")
}
```

## 🚧 [Changed] `convertApolloSchema` and `downloadApolloSchema` now use paths relative to the root of the project (#3773, #3752)

Apollo Kotlin adds two tasks to help to manage schemas: `convertApolloSchema` and `downloadApolloSchema`. These tasks
are meant to be used from the commandline.

Previously, paths were interpreted using the current working directory with `File(path)`. Unfortunately, this is
unreliable because Gradle might change the current working directory in some conditions (
see [Gradle#13927](https://github.com/gradle/gradle/issues/13927)
or [Gradle#6074](https://github.com/gradle/gradle/issues/6074) for an example).

With 3.1.0 and onwards, paths, will be interpreted relative to the root project
directory (`project.rootProject.file(path)`):

```
# schema is now interpreted relative to the root project directory and
# not the current working directory anymore. This example assumes there 
# is a 'app' module that applies the apollo plugin
./gradlew downloadApolloSchema \
  --endpoint="https://your.domain/graphql/endpoint" \
  --schema="app/src/main/graphql/com/example/schema.graphqls"
```

## ❤️ External contributors

Many thanks to @dhritzkiv, @mune0903, @StylianosGakis, @AchrafAmil and @jamesonwilliams for their awesome contributions!
You rock 🎸 🤘 !

## 👷 All changes

* Fix error reporting when there is a "schema.graphqls" but it doesn't contain any type definition (#3844)
* Make guessNumber read the next value only once, fixes parsing custom scalars without a custom adapter (#3839, #3836)
* Clarify need to pass client's customScalarAdapters to store methods (#3838)
* Fix null pointer exception in LruCache while trimming (#3833)
* Add FetchPolicy.CacheAndNetwork (#3828)
* Allow to specify error handling for watch() (#3817)
* Scalar mapping and adapter configuration improvements (#3779)
* Tunnel variables in CustomScalarAdapters (#3813)
* Terminate threads correctly if no subscription has been executed (#3803)
* fix validation of merged fields (#3799)
* Make `reconnectWhen` suspend and pass attempt number (#3772)
* Merge HTTP headers when batching (#3776)
* MockServer improvements and TestNetworkTransport (#3757)
* Fix calling ApolloClient.newBuilder() if the original ApolloClient used `.okHttpClient` (#3771)
* Make `convertApolloSchema` and `downloadApolloSchema` use path from the root of the project (#3773, #3752)
* fix fragment package name in multi-module scenarios (#3775)
* Make the error printer robust to unknown source locations, fixes schemas with duplicate types (#3753)
* Allow to customize the fetchPolicy with interceptors (#3743)

# Version 3.0.0

_2021-12-15_

This is the first stable release for ~Apollo Android 3~ Apollo Kotlin 3 🎉!

There is [documentation](https://www.apollographql.com/docs/android/),
a [migration guide](https://www.apollographql.com/docs/android/migration/3.0/) and a blog post coming soon (we'll update
these notes when it's out).

In a nutshell, Apollo Kotlin 3 brings:

* [coroutine APIs](https://www.apollographql.com/docs/android/essentials/queries/) for easier concurrency
* [multiplatform support](https://www.apollographql.com/docs/android/advanced/kotlin-native/) makes it possible to run
  the same code on Android, JS, iOS, MacOS and linux
* [responseBased codegen](https://www.apollographql.com/docs/android/advanced/response-based-codegen/) is a new optional
  codegen that models fragments as interfaces
* SQLite batching makes reading from the SQLite cache significantly faster
* [Test builders](https://www.apollographql.com/docs/android/advanced/test-builders/) offer a simple APIs to build fake
  models for your tests
* [The @typePolicy and @fieldPolicy](https://www.apollographql.com/docs/android/caching/declarative-ids/) directives
  make it easier to define your cache ids at compile time
* [The @nonnull](https://www.apollographql.com/docs/android/advanced/nonnull/) directive catches null values at parsing
  time, so you don't have to deal with them in your UI code

Feel free to ask questions by
either [opening an issue on our GitHub repo](https://github.com/apollographql/apollo-kotlin/issues)
, [joining the community](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
or [stopping by our channel in the KotlinLang Slack](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)(get your
invite [here](https://slack.kotl.in/)).

### Changes compared to `3.0.0-rc03`:

* Fix rewinding the Json stream when lists are involved (#3727)
* Kotlin 1.6.10 (#3723)
* Disable key fields check if unnecessary and optimize its perf (#3720)
* Added an easy way to log cache misses (#3724)
* Add a Data.toJson that uses reflection to lookup the adapter (#3719)
* Do not run the cache on the main thread (#3718)
* Promote JsonWriter extensions to public API (#3715)
* Make customScalarAdapters and subscriptionsNetworkTransport public (#3714)

# Version 3.0.0-rc03

_2021-12-13_

Compared to the previous RC, this version adds a few new convenience API and fixes 3 annoying issues.

💙 Many thanks to @ mateuszkwiecinski, @ schoeda and @ fn-jt for all the feedback 💙

## ✨ New APIs

- Make `ApolloCall.operation` public (#3698)
- Add `SubscriptionWsProtocolAdapter` (#3697)
- Add `Operation.composeJsonRequest` (#3697)

## 🪲 Bug fixes

- Allow repeated `@fieldPolicy` (#3686)
- Fix incorrect merging of nested objects in JSON (#3672)
- Fix duplicate query detection (#3699)

# Version 3.0.0-rc02

_2021-12-10_

💙 Many thanks to @michgauz, @joeldenke, @rohandhruva, @schoeda, @CoreFloDev and @sproctor for all the feedback 💙

### ⚙️ [breaking] Merge ApolloQueryCall, ApolloSubscriptionCall, ApolloMutationCall (#3676)

In order to simplify the API and keep the symmetry with `ApolloRequest<D>` and `ApolloResponse<D>`
, `ApolloQueryCall<D, E>`, `ApolloSubscriptionCall<D, E>`, `ApolloMutationCall<D, E>` are replaced with `ApolloCall<D>`.
This change should be mostly transparent but it's technically a breaking change. If you are
passing `ApolloQueryCall<D, E>` variables, it is safe to drop the second type parameter and use `ApolloCall<D>` instead.

### ✨ [New] Add `WebSocketNetworkTransport.reconnectWhen {}` (#3674)

You now have the option to reconnect a WebSocket automatically when an error happens and re-subscribe automatically
after the reconnection has happened. To do so, use the `webSocketReconnectWhen` parameter:

```kotlin
val apolloClient = ApolloClient.Builder()
    .httpServerUrl("http://localhost:8080/graphql")
    .webSocketServerUrl("http://localhost:8080/subscriptions")
    .wsProtocol(
        SubscriptionWsProtocol.Factory(
            connectionPayload = {
              mapOf("token" to upToDateToken)
            }
        )
    )
    .webSocketReconnectWhen {
      // this is called when an error happens on the WebSocket
      it is ApolloWebSocketClosedException && it.code == 1001
    }
    .build()
```

### Better Http Batching API (#3670)

The `HttpBatchingEngine` has been moved to an `HttpInterceptor`. You can now configure Http batching with a specific
method:

```kotlin
apolloClient = ApolloClient.Builder()
    .serverUrl(mockServer.url())
    .httpBatching(batchIntervalMillis = 10)
    .build()
```

### All changes:

* Add 2.x symbols (`Rx2Apollo`, `prefetch()`, `customAttributes()`, `ApolloIdlingResource.create()`) to help the
  transition (#3679)
* Add canBeBatched var to ExecutionOptions (#3677)
* Merge ApolloQueryCall, ApolloSubscriptionCall, ApolloMutationCall (#3676)
* Add `WebSocketNetworkTransport.reconnectWhen {}` (#3674)
* Move BatchingHttpEngine to a HttpInterceptor (#3670)
* Add exposeErrorBody (#3661)
* fix the name of the downloadServiceApolloSchemaFromRegistry task (#3669)
* Fix DiskLruHttpCache concurrency (#3667)

# Version 3.0.0-rc01

_2021-12-07_

This version is the release candidate for Apollo Android 3 🚀. Please try it
and [report any issues](https://github.com/apollographql/apollo-kotlin/issues/new/choose), we'll fix them urgently.

There is [documentation](https://www.apollographql.com/docs/android/) and
a [migration guide](https://www.apollographql.com/docs/android/migration/3.0/). More details are coming soon. In a
nutshell, Apollo Android 3 brings, amongst other things:

* [coroutine APIs](https://www.apollographql.com/docs/android/essentials/queries/) for easier concurrency
* [multiplatform support](https://www.apollographql.com/docs/android/advanced/kotlin-native/) makes it possible to run
  the same code on Android, JS, iOS, MacOS and linux
* [responseBased codegen](https://www.apollographql.com/docs/android/advanced/response-based-codegen/) is a new optional
  codegen that models fragments as interfaces
* SQLite batching makes reading from the SQLite cache significantly faster
* [Test builders](https://www.apollographql.com/docs/android/advanced/test-builders/) offer a simple APIs to build fake
  models for your tests
* [The @typePolicy and @fieldPolicy](https://www.apollographql.com/docs/android/caching/declarative-ids/) directives
  make it easier to define your cache ids at compile time
* [The @nonnull](https://www.apollographql.com/docs/android/advanced/nonnull/) directive catches null values at parsing
  time, so you don't have to deal with them in your UI code

Compared to `beta05`, this version changes the default value of `generateOptionalOperationVariables`, is compatible with
Gradle configuration cache and fixes a few other issues.

## ⚙️ API changes

### Optional operation variables (#3648) (breaking)

The default value for the `generateOptionalOperationVariables` config is now `true`.

What this means:

- By default, operations with nullable variables will be generated with `Optional` parameters
- You will need to wrap your parameters at the call site

For instance:

```graphql
query GetTodos($first: Int, $offset: Int) {
  todos(first: $first, offset: $offset) {
    ...Todo
  }
}
```

```kotlin
// Before
val query = GetTodosQuery(100, null)

// After
val query = GetTodosQuery(Optional.Present(100), Optional.Absent)

```

- If you prefer, you can set `generateOptionalOperationVariables` to `false` to generate non-optional parameters
  globally
- This can also be controlled on individual variables with the `@optional` directive
- More information about this can be
  found [here](https://www.apollographql.com/docs/android/advanced/operation-variables/)

We think this change will make more sense to the majority of users (and is consistent with Apollo Android v2's behavior)
even though it may be more verbose, which is why it is possible to change the behavior via
the `generateOptionalOperationVariables` config.

To keep the `beta05` behavior, set `generateOptionalOperationVariables` to false in your Gradle configuration:

```
apollo {
  generateOptionalOperationVariables.set(false)
}
```

### ApolloClient.Builder improvements (#3647)

You can now pass WebSocket related options to the `ApolloClient.Builder` directly (previously this would have been done
via `NetworkTransport`):

```kotlin
// Before
val apolloClient = ApolloClient.Builder()
    // (...)
    .subscriptionNetworkTransport(WebSocketNetworkTransport(
        serverUrl = "https://example.com/graphql",
        idleTimeoutMillis = 1000L,
        wsProtocol = SubscriptionWsProtocol.Factory()
    ))
    .build()

// After
val apolloClient = ApolloClient.Builder()
    // (...)
    .wsProtocol(SubscriptionWsProtocol.Factory())
    .webSocketIdleTimeoutMillis(1000L)
    .build()
```

### Upgrade to OkHttp 4 (#3653) (breaking)

This version upgrades OkHttp to `4.9.3` (from `3.12.11`). This means Apollo Android now requires Android `apiLevel` `21`
+. As OkHttp 3 enters end of life at the end of the year and the vast majority of devices now support `apiLevel` `21`,
we felt this was a reasonable upgrade.

## 🪲 Bug fixes

- Fixed an issue where it was not possible to restart a websocket after a network error (#3646)
- Fixed an issue where Android Java projects could not use the Apollo Gradle plugin (#3652)

## 👷 All Changes

- Update a few dependencies (#3653)
- Fix Android Java projects (#3652)
- Expose more configuration options on ApolloClient.Builder (#3647)
- Fix restarting a websocket after a network error (#3646)
- Change optional default value (#3648)
- Fix configuration cache (#3645)

