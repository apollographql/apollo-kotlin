# Codegen Models

In order to provide type safety, Apollo Kotlin generates Kotlin models from your queries. The fields you're querying will be accessible in the Kotlin models. Fields that are not queried will not be accessible. For simple queries, the mapping of GraphQL queries to Kotlin models is relatively natural. GraphQL objects are mapped to Kotlin data classes and GraphQL fields to Kotlin properties.

For more complex queries, involving [merged fields](https://spec.graphql.org/draft/#sec-Field-Selection-Merging) and/or [fragments](https://spec.graphql.org/draft/#sec-Validation.Fragments) on different interfaces/union types, this mapping becomes more complicated as polymorphic types are needed. In order to accommodate different needs, Apollo Kotlin supports three different codegen modes:

* **responseBased**: the Kotlin models map the received json.
* **operationBased**: the Kotlin models map the sent operation.
* **experimental_operationBasedWithInterfaces**: based on **operationBased**, but expose more type information. 
* **compat**: for compatibility with Apollo Kotlin 2.x

`responseBased` will generate interfaces to access the models in a more polymorphic way. It will also store each merged field exactly once and have more efficient json parsing. That comes at the price of more generated code. 

`operationBased` will generate less code but will use more memory and expose less type information.

`experimental_operationBasedWithInterfaces` same as `operationBased` but generates interfaces for selection sets that contain fragments to make it easier to use `when` statements. 


# responseBased codegen

responseBased codegen generates models matching 1:1 the Json response. Fragments are generated as interfaces.

Because it generates both `interfaces` and `classes` for the models, and because all the named fragment fields are pulled in a local  model, the responseBased codegen usually generates more code. Paradoxically, it uses less runtime memory because each json field is stored only once instead of duplicating the merged fields like in operationBased models.

Thanks to this 1:1 mapping, the parsers don't have to rewind in the json response and they can stream the response directly into the models. These parsers are named "streaming parsers" and have multiple advantages:

* They save some memory: they don't have to buffer the whole network response in an intermediate `HashMap`
* They save cpu cycles: because they don't have to write and read the intermediate `HashMap`, the parsing is slightly more efficient
* Most importantly, they can amortize building the models during network I/O: once the last byte of data is received, the models are ready to be consumed.

### Named fragments

Named fragments are generated as an interface that is then implemented by the models:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ...droidDetails 
  }
}
fragment droidDetails on Droid {
  primaryFunction
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
interface Hero(val id: String)
class Droid(override val id: String, override val primaryFunction: String): Hero, DroidDetails
class OtherHero(override val id: String):Hero

// DroidDetails.kt
interface DroidDetails(val primaryFunction: String)
</pre></td>
</tr>
</table>

The different response shapes are implemented as different classes implementing a base `Hero` interface. Unlike with operationBased models, all fields of a given shape are reachable on a single model if cast to the correct interface/class.

The `DroidDetails` interface is generated in a separate file because it can be reused in other queries.

### Inline fragments

Inline fragments are merged with other fields in the different models:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ... on Droid {
      name
    }
  } 
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
interface Hero(val id: String)
class Droid(override val id: String, override val name: String): Hero
class OtherHero(override val id: String):Hero
</pre></td>
</tr>
</table>

No specific interface/class is generated for inline fragments because their fields are accessible from the shapes by construction.

There is no separate `OnDroid` class as it's merged in the `Droid` one.

### Merged fields

With responseBased codegen, inline and named fragments contribute their fields to the same models. If there are merged fields, they will happen only once in the model tree. They can appear in multiple classes if there are polymorphic fields but they're guaranteed to appear only once in the tree:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    name
    ... on Droid {
      name
    }
    ...droidDetails
  }
}
fragment droidDetails on Droid {
  name
}

</pre></td>
<td><pre lang="kotlin">
// name is queried in three different positions but will appear either in the Droid shape or
// in the OtherHero shape:

// If the response is a Droid
(data.hero as Droid).name
// If the response is of another type
(data.hero as OtherHero).name

// Because both Droid and OtherHero inherit from Hero and name is queried directly on hero,
// name is also accessible from the interface.
// This specific example is contrived because accessing this way is always easier but it
// shows the multiple possibilities.
data.hero.name
</pre></td>
</tr>
</table>

### Accessors

Because casting breaks the typing flow, and the subclasses are not easily discoverable, the codegen will generate accessors to casts to various shapes/fragments.

Named fragments use the name of the fragments:

```kotlin
data.hero.droidDetails().primaryFunction
```

Shapes use `asNameOfTheShape()`:

```
data.hero.asDroid().primaryFunction
```

Inline fragments don't have accessors because they are merged into the different shapes.


# operationBased models

operationBased models match 1:1 the structure of the operation document. Fragments spreads and inline fragments are generated as their own classes. Merged fields are stored multiple times, once each time they are queried.

### Named fragments

Named fragments are generated as separated/reusable data classes, and a corresponding field with the same name as the fragment:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ...droidDetails 
  }
}
fragment droidDetails on Droid {
  primaryFunction
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
class Hero(val id: String, val droidDetails: DroidDetails?)
// DroidDetails.kt
class DroidDetails(val primaryFunction: String)
</pre></td>
</tr>
</table>

`Hero.droidDetails` is nullable because it will only be set if the returned `hero` is a `Droid`.

The `DroidDetails` class is generated in a separate file because it can be reused in other queries.

### Inline fragments

Inline fragments are generated as local data classes and a corresponding field:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ... on Droid {
      name
    }
  } 
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
class Hero(val id: String, val onDroid: OnDroid?)
class OnDroid(val name: String)
</pre></td>
</tr>
</table>

`Hero.onDroid` is nullable because it will only be set if the returned `hero` is a `Droid`.

The `OnDroid` class is generated in the same file as the query because it cannot be reused in other queries.

### Merged fields

Merged fields are readable from different paths:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    name
    ... on Droid {
      name
    }
  } 
}
</pre></td>
<td><pre lang="kotlin">
// name is stored twice and can be accessed through different
// paths
data.hero.name
data.hero.onDroid?.name
</pre></td>
</tr>
</table>

# experimental_operationBasedWithInterfaces models

experimental_operationBasedWithInterfaces codegen is a variant of operationBased which allows to leverage Kotlin `when` statements and make some fragments non-null depending on what case they are in.

#### Why another operationBased?

With operationBased model, handling all possible types of a model relies on a succession of `if` statements: 

For example query below,
```graphql
query TestOperation {
  something {
    ... on Type1 {
      #...
    }
    ... on Type2 {
      #...
    }
    ... on Type3 {
      #...
    }
  }
}
```

Handling `something` code will look like the following:

```kotlin
when {
  something.onType1 != null -> {
    // Handle Type1
  }
  something.onType2 != null -> {
    // Handle Type2
  }
  something.onType3 != null -> {
    // Handle Type3
  }
}
```

#### Why this code treacherous?
* It is difficult to write when there are many possible types. 
* It is difficult to maintain when there is a new possible type. 

With experimental_operationBasedWithInterfaces, it generates a `sealed interface` and `classes` for model with multiple possible types. So such model can be handled with [sealed interface and when statement](https://kotlinlang.org/docs/sealed-classes.html#sealed-classes-and-when-expression)

Handling `something` code will become like this
```kotlin
when (something) {
  is Type1Something -> {
    // Handle Type1
  }
  is Type2Something -> {
    // Handle Type2
  }
  is Type3Something -> {
    // Handle Type3
  }
}
```
#### Benefits
* Compiler can help generate code to handle all possible types
* Compiler can check if all possible types is handled

### Named fragments

Named fragments are generated as separated/reusable data classes, and a corresponding field with the same name as the fragment:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ...droidDetails 
  }
}
fragment droidDetails on Droid {
  primaryFunction
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
sealed interface IHero(val id: String, val droidDetails: DroidDetails?)
data class DroidHero(override val id: String, override val droidDetails: DroidDetails): IHero
data class Hero(override val id: String, override val droidDetails: DroidDetails?): IHero

// DroidDetails.kt
class DroidDetails(val primaryFunction: String)
</pre></td>
</tr>
</table>

The different possible types are implemented as different classes implementing a base `IHero` interface.

The response will be a `DroidHero` when returned `hero` is a `Droid`. In this case, `IHero.droidDetails` is always set, so it overrides as a non-null field.

The response will be a `Hero` when returned `hero` is not a `Droid`.

The `DroidDetails` interface is generated in a separate file because it can be reused in other queries.

### Inline fragments

Inline fragments are generated as local data classes and a corresponding field:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ... on Droid {
      name
    }
  } 
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
sealed interface IHero(val id: String, val onDroid: Hero.OnDroid?)
data class DroidHero(override val id: String, override val onDroid: Hero.OnDroid): IHero
data class Hero(override val id: String, override val onDroid: Hero.OnDroid?): IHero

data class OnDroid(name: String)
</pre></td>
</tr>
</table>

The different possible types are implemented as different classes implementing a base `IHero` interface.

The response will be a `DroidHero` when returned `hero` is a `Droid`. In this case, `IHero.onDroid` is always set, so it overrides as a non-null field.

The response will be a `Hero` when returned `hero` is not a `Droid`.

The `OnDroid` class is generated in the same file as the query because it cannot be reused in other queries.

# compat codegen

Compat codegen is an extra codegen made to be mostly compatible with 2.x. It is mainly operationBased with a few differences:

* Fragment spreads are grouped in a `.fragments` container
* Inline fragment classes also collect their parent fields
* Trivial whose typeCondition is always true (trivial inline fragments) are merged
* Models are flattened by default

### Named fragments

Named fragments are grouped in a `Fragments` container:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ...droidDetails 
  }
}
fragment droidDetails on Droid {
  primaryFunction
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
class Hero(val id: String, val fragments: Fragments) {
  class Fragments(val droidDetails: DroidDetails)
}
class DroidDetails(val primaryFunction: String)
</pre></td>
</tr>
</table>

### Inline fragments

Inline fragments collect their parent fields. In order to make that behaviour explicit, the models are named `AsFoo` instead of `OnFoo`:

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query GetHero {
  hero {
    id
    ... on Droid {
      name
    }
  } 
}
</pre></td>
<td><pre lang="kotlin">
// GetHeroQuery.kt
class Hero(val id: String, val onDroid: OnDroid?)
class AsDroid(val id: String, val name: String)
</pre></td>
</tr>
</table>

# Other considerations

### Class flattening and name clashes

Flattening the classes is useful to have shorter qualified class names. In addition to making less clutter in the code, there's an issue on MacOS where class names can't be more than 256 characters. Flattening the classes is useful but can lead to name clashes.

This typically happens if two fields have the same responseName in different parts of the operation tree. You can use field aliases to workaround the name clash:

<table>
<tr><th>With name clash</th><th>Workaround</th></tr>
<tr>
<td><pre lang="graphql">
{
  hero {
    friend {
      address {
        street
      }
    }
    father {
      # address is used twice
      address {
        city
      }
    }
  }
}
</pre></td>
<td><pre lang="graphql">
{
  hero {
    friend {
      # using an alias removes the name clash
      friendAddress: address {
        street
      }
    }
    father {
      # using an alias removes the name clash
      fatherAddress: address {
        city
      }
    }
  }
}
</pre></td>
</table>

### Naming

Because GraphQL is case sensitive the codegen attempts at making the minimum changes to the GraphQL field names. Two notable exceptions are:

* Models are capitalized because it's more idiomatic in Kotlin and also because it creates an error if both the property and its type have the same name.

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
{
  hero {
    name
  }
}
</pre></td>
<td><pre lang="kotlin">
// hero is capitalized here
class Hero(
  val name: String 
)
</pre></td>
</tr>
</table>

* Kotlin keywords are escaped by suffixing with `_`

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
{
  hero {
    null
  }
}
</pre></td>
<td><pre lang="kotlin">
class Hero(
  // null is reserved in Kotlin and escaped here
  val null_: String 
)
</pre></td>
</tr>
</table>
