This folder contains several groups of GraphQL definitions we use during codegen:

* builtins.graphqls: the official built-in defintions such as [built-in scalars](https://spec.graphql.org/draft/#sec-Scalars.Built-in-Scalars), [built-in directives](https://spec.graphql.org/draft/#sec-Type-System.Directives.Built-in-Directives) or [introspection definitions](https://spec.graphql.org/draft/#sec-Schema-Introspection.Schema-Introspection-Schema).
* link.graphqls: the [core schemas](https://specs.apollo.dev/link/v1.0/) definitions.
* apollo-${version}.graphqls: the client directives supported by Apollo Kotlin. Changes are versioned at https://github.com/apollographql/specs. Changing them requires a new version and a PR.

