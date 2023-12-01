package com.apollographql.apollo3.ast.internal

/**
 * This file contains several groups of GraphQL definitions we use during codegen:
 *
 * - builtins.graphqls: the official built-in defintions such as [built-in scalars](https://spec.graphql.org/draft/#sec-Scalars.Built-in-Scalars), [built-in directives](https://spec.graphql.org/draft/#sec-Type-System.Directives.Built-in-Directives) or [introspection definitions](https://spec.graphql.org/draft/#sec-Schema-Introspection.Schema-Introspection-Schema).
 * - link.graphqls: the [core schemas](https://specs.apollo.dev/link/v1.0/) definitions.
 * - apollo-${version}.graphqls: the client directives supported by Apollo Kotlin. Changes are versioned at https://github.com/apollographql/specs. Changing them requires a new version and a PR.
 */
internal val kotlinLabsDefinitions = """
  ""${'"'}
  Marks a field or variable definition as optional or required
  By default Apollo Kotlin generates all variables of nullable types as optional, in compliance with the GraphQL specification,
  but this can be configured with this directive, because if the variable was added in the first place, it's usually to pass a value
  Since: 3.0.0
  ""${'"'}
  directive @optional(if: Boolean = true) on FIELD | VARIABLE_DEFINITION

  ""${'"'}
  Marks a field as non-null. The corresponding Kotlin property will be made non-nullable even if the GraphQL type is nullable.
  When used on an object definition in a schema document, `fields` must be non-empty and contain a selection set of fields that should be non-null
  When used on a field from an executable document, `fields` must always be empty

  Setting the directive at the schema level is usually easier as there is little reason that a field would be non-null in one place
  and null in the other
  Since: 3.0.0
  ""${'"'}
  directive @nonnull(fields: String! = "") on OBJECT | FIELD

  ""${'"'}
  Attach extra information to a given type
  Since: 3.0.0
  ""${'"'}
  directive @typePolicy(
    ""${'"'}
    a selection set containing fields used to compute the cache key of an object. Order is important.
    ""${'"'}
    keyFields: String! = "",
    ""${'"'}
    a selection set containing fields that shouldn't create a new cache Record and should be
    embedded in their parent instead. Order is unimportant.
    ""${'"'}
    embeddedFields: String! = "",
    ""${'"'}
    a selection set containing fields that should be treated as Relay Connection fields. Order is unimportant.
    Since: 3.4.1
    ""${'"'}
    connectionFields: String! = ""
  ) on OBJECT | INTERFACE | UNION

  ""${'"'}
  Attach extra information to a given field
  Since: 3.3.0
  ""${'"'}
  directive @fieldPolicy(
    forField: String!,
    ""${'"'}
    a list of arguments used to compute the cache key of the object this field is pointing to.
    The list is parsed as a selection set: both spaces and comas are valid separators.
    ""${'"'}
    keyArgs: String! = "",
    ""${'"'}
    (experimental) a list of arguments that vary when requesting different pages.
    These arguments are omitted when computing the cache key of this field.
    The list is parsed as a selection set: both spaces and comas are valid separators.
    Since: 3.4.1
    ""${'"'}
    paginationArgs: String! = ""
  ) repeatable on OBJECT

  ""${'"'}
  Indicates that the given field, argument, input field or enum value requires
  giving explicit consent before being used.
  Since: 3.3.1
  ""${'"'}
  directive @requiresOptIn(feature: String!) repeatable
  on FIELD_DEFINITION
      | ARGUMENT_DEFINITION
      | INPUT_FIELD_DEFINITION
      | ENUM_VALUE

  ""${'"'}
  Use the specified name in the generated code instead of the GraphQL name.
  Use this for instance when the name would clash with a reserved keyword or field in the generated code.
  This directive is experimental.
  Since: 3.3.1
  ""${'"'}
  directive @targetName(name: String!)
  on OBJECT
      | INTERFACE
      | ENUM
      | ENUM_VALUE
      | UNION
      | SCALAR
      | INPUT_OBJECT
""".trimIndent()

// Built in scalar and introspection types from the Draft:
// - https://spec.graphql.org/draft/#sec-Scalars
// - https://spec.graphql.org/draft/#sec-Schema-Introspection.Schema-Introspection-Schema
// In theory the user needs to provide the builtin definitions because we cannot know in advance what
// version of the spec they are using neither if they have extended any of the introspection types.
// This file is a fallback to make a best-effort guess in case the user didn't provide these definitions, at the
// risk of potentially validating invalid queries.
internal val builtinsDefinitionsStr = """
  ""${'"'}
  The `Int` scalar type represents non-fractional signed whole numeric values. Int can represent values between -(2^31) and 2^31 - 1.
  ""${'"'}
  scalar Int

  ""${'"'}
  The `Float` scalar type represents signed double-precision fractional values as specified by [IEEE 754](http://en.wikipedia.org/wiki/IEEE_floating_point).
  ""${'"'}
  scalar Float

  ""${'"'}
  The `String` scalar type represents textual data, represented as UTF-8 character sequences. The String type is most often used by GraphQL to represent free-form human-readable text.
  ""${'"'}
  scalar String

  ""${'"'}
  The `Boolean` scalar type represents `true` or `false`.
  ""${'"'}
  scalar Boolean

  ""${'"'}
  The `ID` scalar type represents a unique identifier, often used to refetch an object or as key for a cache. The ID type appears in a JSON response as a String; however, it is not intended to be human-readable. When expected as an input type, any string (such as `"4"`) or integer (such as `4`) input value will be accepted as an ID.
  ""${'"'}
  scalar ID

  type __Schema {
    description: String
    types: [__Type!]!
    queryType: __Type!
    mutationType: __Type
    subscriptionType: __Type
    directives: [__Directive!]!
  }
  
  type __Type {
    kind: __TypeKind!
    name: String
    description: String
    # must be non-null for OBJECT and INTERFACE, otherwise null.
    fields(includeDeprecated: Boolean = false): [__Field!]
    # must be non-null for OBJECT and INTERFACE, otherwise null.
    interfaces: [__Type!]
    # must be non-null for INTERFACE and UNION, otherwise null.
    possibleTypes: [__Type!]
    # must be non-null for ENUM, otherwise null.
    enumValues(includeDeprecated: Boolean = false): [__EnumValue!]
    # must be non-null for INPUT_OBJECT, otherwise null.
    inputFields(includeDeprecated: Boolean = false): [__InputValue!]
    # must be non-null for NON_NULL and LIST, otherwise null.
    ofType: __Type
    # may be non-null for custom SCALAR, otherwise null.
    specifiedByURL: String
  }
  
  enum __TypeKind {
    SCALAR
    OBJECT
    INTERFACE
    UNION
    ENUM
    INPUT_OBJECT
    LIST
    NON_NULL
  }
  
  type __Field {
    name: String!
    description: String
    args(includeDeprecated: Boolean = false): [__InputValue!]!
    type: __Type!
    isDeprecated: Boolean!
    deprecationReason: String
  }
  
  type __InputValue {
    name: String!
    description: String
    type: __Type!
    defaultValue: String
    isDeprecated: Boolean!
    deprecationReason: String
  }
  
  type __EnumValue {
    name: String!
    description: String
    isDeprecated: Boolean!
    deprecationReason: String
  }
  
  type __Directive {
    name: String!
    description: String
    locations: [__DirectiveLocation!]!
    args(includeDeprecated: Boolean = false): [__InputValue!]!
    isRepeatable: Boolean!
  }
  
  enum __DirectiveLocation {
    QUERY
    MUTATION
    SUBSCRIPTION
    FIELD
    FRAGMENT_DEFINITION
    FRAGMENT_SPREAD
    INLINE_FRAGMENT
    VARIABLE_DEFINITION
    SCHEMA
    SCALAR
    OBJECT
    FIELD_DEFINITION
    ARGUMENT_DEFINITION
    INTERFACE
    UNION
    ENUM
    ENUM_VALUE
    INPUT_OBJECT
    INPUT_FIELD_DEFINITION
  }

  directive @skip(if: Boolean!) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT

  directive @include(if: Boolean!) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT

  directive @deprecated(
    reason: String = "No longer supported"
  ) on FIELD_DEFINITION | ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION | ENUM_VALUE

  directive @defer(
    label: String
    if: Boolean! = true
  ) on FRAGMENT_SPREAD | INLINE_FRAGMENT

  directive @specifiedBy(url: String!) on SCALAR
""".trimIndent()

internal val linkDefinitionsStr = """
  # https://specs.apollo.dev/link/v1.0/
  scalar Import

  enum Purpose {
    EXECUTION,
    SECURITY
  }

  directive @link(
    url: String!,
    as: String,
    import: [Import],
    for: Purpose
  ) repeatable on SCHEMA
""".trimIndent()

val nullabilityDefinitionsStr = """
""${'"'}
Indicates that a field is only null if there is a matching error in the `errors` array.
In all other cases, the field is non-null.

Tools doing code generation may use this information to generate the field as non-null.

This directive can be applied on field definitions:

```graphql
type User {
    email: String @semanticNonNull
}
```

It can also be applied on object type extensions for use in client applications that do
not own the base schema:

```graphql
extend type User @semanticNonNull(field: "email")
```

Control over list items is done using the `level` argument:

```graphql
type User {
    # friends is nullable but friends[0] is null only on errors
    friends: [User] @semanticNonNull(level: 1)
}
```

The `field` argument is the name of the field if `@semanticNonNull` is applied to an object definition.
If `@semanticNonNull` is applied to a field definition, `field` must be null.

The `level` argument can be used to indicate what level is semantically non null in case of lists.
`level` starts at 0 if there is no list. If `level` is null, all levels are semantically non null.
""${'"'}
directive @semanticNonNull(field: String = null, level: Int = null) repeatable on FIELD_DEFINITION | OBJECT

""${'"'}
Indicates that the given position stops GraphQL errors to propagate up the tree.

By default, the first GraphQL error stops the parsing and fails the whole response.
Using `@catch` recovers from this error and allows the parsing to continue.

`@catch` must also be applied to the schema definition to specify the default to
use for every field that can return an error (nullable fields).

The `to` argument can be used to choose how to recover from errors. See `CatchTo`
for more details.

The `level` argument can be used to indicate where to catch in case of lists.
`level` starts at 0 if there is no list. If `level` is null, all levels catch.
""${'"'}
directive @catch(to: CatchTo! = RESULT, level: Int = null) repeatable on FIELD | SCHEMA

enum CatchTo {
    ""${'"'}
    Map to a result type that can contain either a value or an error.
    ""${'"'}
    RESULT,
    ""${'"'}
    Map to a nullable type that will be null in the case of error.
    This does not allow to distinguish between semantic null and error but
    can be simpler in some cases.
    ""${'"'}
    NULL,
    ""${'"'}
    Do not catch and let any exception through
    ""${'"'}
    THROW
}

""${'"'}
Never throw on field errors.

This is used for backward compatibility for clients where this was the default behaviour.
""${'"'}
directive @ignoreFieldErrors on QUERY | MUTATION | SUBSCRIPTION
""".trimIndent()