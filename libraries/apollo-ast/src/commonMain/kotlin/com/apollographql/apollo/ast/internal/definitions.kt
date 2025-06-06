package com.apollographql.apollo.ast.internal

/**
 * This file contains several groups of GraphQL definitions we use during codegen:
 *
 * - builtins.graphqls: the official built-in defintions such as [built-in scalars](https://spec.graphql.org/draft/#sec-Scalars.Built-in-Scalars), [built-in directives](https://spec.graphql.org/draft/#sec-Type-System.Directives.Built-in-Directives) or [introspection definitions](https://spec.graphql.org/draft/#sec-Schema-Introspection.Schema-Introspection-Schema).
 * - link.graphqls: the [core schemas](https://specs.apollo.dev/link/v1.0/) definitions.
 * - apollo-${version}.graphqls: the client directives supported by Apollo Kotlin. Changes are versioned at https://github.com/apollographql/specs. Changing them requires a new version and a PR.
 */
internal val kotlinLabsDefinitions_0_3 = """
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
      A selection set containing fields used to compute the cache key of an object. 
      Nested selection sets are currently not supported. Order is important. 
      
      Key fields can be defined on interfaces. In that case, the key fields apply to all sub-types and sub-types are not allowed to define their own key fields.
      If a type implements multiple interfaces with keyfields, the keyfields must match across all interfaces with keyfields.
      
      The key fields are automatically added to the operations by the compiler.
      Aliased key fields are not recognized and the compiler adds a non-aliased version of the field if that happens.
      If a type is queried through an interface/union, this may add fragments.

      For an example, this query:
      
      ```graphql
      query {
        product {
          price
        }
      }
      ```
      
      is turned into this one after compilation: 
      
      ```graphql
      query {
        product {
          ... on Book {
            isbn
          }
          ... on Movie {
            id
          }
          price
        }
      }
      ```
      
      ""${'"'}
      keyFields: String! = "",
      ""${'"'}
      (experimental) a selection set containing fields that shouldn't create a new cache Record and should be
      embedded in their parent instead. Order is unimportant.
      ""${'"'}
      embeddedFields: String! = "",
      ""${'"'}
      (experimental) a selection set containing fields that should be treated as [Relay Connection](https://relay.dev/graphql/connections.htm) fields.
      Order is unimportant.
      This works in conjunction with `ConnectionMetadataGenerator` and `ConnectionRecordMerger` which must be configured on the `ApolloStore`.
      Since: 3.4.1
      ""${'"'}
      connectionFields: String! = ""
  ) on OBJECT | INTERFACE
  
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

internal val kotlinLabsDefinitions_0_4 = """
""${'"'}
Marks a field or variable definition as optional or required
By default Apollo Kotlin generates all variables of nullable types as optional, in compliance with the GraphQL specification,
but this can be configured with this directive, because if the variable was added in the first place, it's usually to pass a value
Since: 3.0.0
""${'"'}
directive @optional(if: Boolean = true) on FIELD | VARIABLE_DEFINITION

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
    (experimental) a selection set containing fields that shouldn't create a new cache Record and should be
    embedded in their parent instead. Order is unimportant.
    ""${'"'}
    embeddedFields: String! = "",
    ""${'"'}
    (experimental) a selection set containing fields that should be treated as [Relay Connection](https://relay.dev/graphql/connections.htm) fields.
    Order is unimportant.
    This works in conjunction with `ConnectionMetadataGenerator` and `ConnectionRecordMerger` which must be configured on the `ApolloStore`.
    Since: 3.4.1
    ""${'"'}
    connectionFields: String! = ""
) on OBJECT | INTERFACE

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
/**
 * These definitions are publicly part of `kotlin_labs` but to avoid conflicting with user imports
 * they are imported under a different foreign schema name by the Apollo compiler.
 *
 * Ideally, we move all configuration to `.graphqls` files and we can remove that.
 */
internal val compilerOptions_0_0 = """
""${'"'}
Configure the Apollo compiler to map the given scalar to the given class.
""${'"'}
directive @map(
  ""${'"'}
  The fully qualified type name to map the scalar to. 
  Simple generic types without variance or wildcards are also supported. 
  
  Examples: 
    - `java.util.Date`
    - `kotlin.collections.Map<kotlin.String, java.util.Date>`
  ""${'"'}
  to: String!, 

  ""${'"'}
  A fully qualified expression referencing the adapter used to adapt to/from the type
  or inline property type, or `null` to specify the adapter at runtime.
   
  Examples:
    - `com.apollographql.adapter.datetime.KotlinxInstantAdapter`
    - `com.example.MyAdapter()`
  ""${'"'}
  with: String = null, 
  
  ""${'"'}
  If non null, contains the name of the property used to wrap/unwrap the inline class.
  [to] must be an inline class.
  
  Only used in Kotlin codegen.
  ""${'"'}  
  inlineProperty: String = null
) on SCALAR

""${'"'}
Built-in types known at compile time. Apollo Kotlin knows the adapters for those types.
""${'"'}
enum BuiltIn { String, Boolean, Int, Long, Float, Double }

""${'"'}
Use the given builtin type for this scalar.
""${'"'}
directive @mapTo(
  ""${'"'}
  The built-in type to use for this scalar.
  ""${'"'}
  builtIn: BuiltIn!, 
  ""${'"'}
  Whether to generate a wrapper inline class for this scalar.
  ""${'"'}
  inline: Boolean! = true
) on SCALAR
"""


internal val compilerOptions_0_1_additions = """
  ""${'"'}
  Tells the Apollo compiler to generate Data Builders
  ""${'"'}
  directive @generateDataBuilders on SCHEMA
""".trimIndent()

/**
 * Built in scalar and introspection types from the Draft
 *  - https://spec.graphql.org/draft/#sec-Scalars
 *  - https://spec.graphql.org/draft/#sec-Schema-Introspection.Schema-Introspection-Schema
 *
 * In theory the user needs to provide the builtin definitions because we cannot know in advance what
 * version of the spec they are using neither if they have extended any of the introspection types.
 *
 * TODO v5: Only keep built-in scalars as almost everything else may evolve and requiring an explicit decision from the user is safer.
 */
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
    reason: String! = "No longer supported"
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

internal val nullabilityDefinitionsStr = """
""${'"'}
Indicates that a position is semantically non null: it is only null if there is a matching error in the `errors` array.
In all other cases, the position is non-null.

Tools doing code generation may use this information to generate the position as non-null if field errors are handled out of band:

```graphql
type User {
    # email is semantically non-null and can be generated as non-null by error-handling clients.
    email: String @semanticNonNull
}
```

The `levels` argument indicates what levels are semantically non null in case of lists:

```graphql
type User {
    # friends is semantically non null
    friends: [User] @semanticNonNull # same as @semanticNonNull(levels: [0])

    # every friends[k] is semantically non null
    friends: [User] @semanticNonNull(levels: [1])

    # friends as well as every friends[k] is semantically non null
    friends: [User] @semanticNonNull(levels: [0, 1])
}
```

`levels` are zero indexed.
Passing a negative level or a level greater than the list dimension is an error.

""${'"'}
directive @semanticNonNull(levels: [Int] = [0]) on FIELD_DEFINITION

""${'"'}
Indicates that a position is semantically non null: it is only null if there is a matching error in the `errors` array.
In all other cases, the position is non-null.

`@semanticNonNullField` is the same as `@semanticNonNull` but can be used on type system extensions for services
that do not own the schema like client services:

```graphql
# extend the schema to make User.email semantically non-null.
extend type User @semanticNonNullField(name: "email")
```

The `levels` argument indicates what levels are semantically non null in case of lists:

```graphql
# friends is semantically non null
extend type User @semanticNonNullField(name: "friends")  # same as @semanticNonNullField(name: "friends", levels: [0])

# every friends[k] is semantically non null
extend type User @semanticNonNullField(name: "friends", levels: [1])

# friends as well as every friends[k] is semantically non null
extend type User @semanticNonNullField(name: "friends", levels: [0, 1])
```

`levels` are zero indexed.
Passing a negative level or a level greater than the list dimension is an error.

See `@semanticNonNull`.
""${'"'}
directive @semanticNonNullField(name: String!, levels: [Int] = [0]) repeatable on OBJECT | INTERFACE

""${'"'}
Indicates how clients should handle errors on a given position.

The `levels` argument indicates where to catch errors in case of lists:

```graphql
{
    user {
        # friends catches errors
        friends @catch { name } # same as @catch(levels: [0])

        # every friends[k] catches errors
        friends @catch(levels: [0]) { name }

        # friends as well as every friends[k] catches errors
        friends @catch(levels: [0, 1]) { name }
    }
}
```

`levels` are zero indexed.
Passing a negative level or a level greater than the list dimension is an error.

See `CatchTo` for more details.
""${'"'}
directive @catch(to: CatchTo! = RESULT, levels: [Int!]! = [0]) on FIELD

""${'"'}
Indicates how clients should handle errors on a given position by default.

Compared to `@catch`, `@catchByDefault` does not have a `level` argument and applies to all
nullable positions.

When multiple values of `catchTo` are set for a given position:
* the `@catch` value is used if set.
* else the `@catchByDefault` value is used if set on the operation/fragment.
* else the schema `catchByDefault` value is used.
""${'"'}
directive @catchByDefault(to: CatchTo!) on SCHEMA | QUERY | MUTATION | SUBSCRIPTION | FRAGMENT_DEFINITION

enum CatchTo {
    ""${'"'}
    Catch the error and map the position to a result type that can contain either
    a value or an error.
    ""${'"'}
    RESULT,
    ""${'"'}
    Catch the error and map the position to a nullable type that will be null
    in the case of error.
    This does not allow to distinguish between semantic null and error null but
    can be simpler in some cases.
    ""${'"'}
    NULL,
    ""${'"'}
    Throw the error.
    Parent positions can recover using `RESULT` or `NULL`.
    If no parent position recovers, the parsing stops.
    ""${'"'}
    THROW
}
""".trimIndent()

internal val oneOfDefinitionsStr = """
directive @oneOf on INPUT_OBJECT  
""".trimIndent()

internal val deferDefinitionsStr = """
directive @defer(
  label: String
  if: Boolean! = true
) on FRAGMENT_SPREAD | INLINE_FRAGMENT  
""".trimIndent()

internal val nonNullDefinitionStr = """
directive @nonnull(fields: String! = "") on OBJECT | FIELD
""".trimIndent()

internal val disableErrorPropagationStr = """
directive @experimental_disableErrorPropagation on QUERY | MUTATION | SUBSCRIPTION
""".trimIndent()