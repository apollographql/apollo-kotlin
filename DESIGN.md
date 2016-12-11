# Apollo Android design discussion

These are some preliminary ideas for mapping GraphQL query results to Java, based on what we currently do for Swift in Apollo iOS.

## Query classes

I would expect us to generate classes for queries (and other operations). Instances of these classes represent a particular query, fully configured with the required variables.

These query objects can be used multiple times to parse a response or to load results from the cache. They can also be stored by a query manager that watches the store and reloads results when the contents change for example.

We'll have to figure out what makes sense for Java, but one option seems to be to specify a generic type parameter on the `GraphQLQuery` interface to refer to the specific `Data` type that represents results for this query. (I've used `Data` here because this corresponds to the `data` field in a GraphQL response.)

So something like this:

```graphql
query HeroAndFriendsNames($episode: Episode) {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
```

```java
interface GraphQLQuery<D> {
  D parseResponse(...);
  // etc.
}

enum Episode {
    NEWHOPE,
    EMPIRE,
    JEDI
}

class HeroAndFriendsNameQuery implements GraphQLQuery<HeroAndFriendsNameQuery.Data> {
    private Episode episode;

    public HeroAndFriendsNameQuery(Episode episode) {
        this.episode = episode;
    }

    interface Data {
        Hero hero();

        interface Hero {
            String name();
            List<Friend> friends();

            interface Friend {
                String name();
            }
        }
    }
}
```

Nested interfaces are named based on the response name, not the field type or field name. Response names are guaranteed to be unique within a selection set, and it also seems to make more semantic sense. (A field `author` would refer to `Author` for example, and not to `User`.)

Aliases map naturally onto this model as well, so there is no need to use further qualifications I think (like `R2Character`):

```graphql
query TwoHeroes {
  r2: hero {
    name
  }
  luke: hero(episode: EMPIRE) {
    name
  }
}
```

```java
class TwoHeroesQuery implements GraphQLQuery<TwoHeroesQuery.Data> {
    interface Data {
        R2 r2();
        interface R2 {
            String name();
        }
        Luke luke();
        interface Luke {
            String name();
        }
    }
}
```

I don't think we ever want `r2` and `luke` to refer to the same type here. So I don't think we need the dedupe behavior from [PR#4](https://github.com/apollostack/apollo-android/pull/4). Even if they select the same fields now, that is accidental from the perspective of the type system, so we still want the types to be separate to avoid relying on these accidental shared fields.

In GraphQL, fragments offer an explicit way to share selections.

## Fragments

```graphql
query TwoHeroes {
  r2: hero {
    ...HeroDetails
  }
  luke: hero(episode: EMPIRE) {
    ...HeroDetails
  }
}

fragment HeroDetails on Character {
  name
}
```

This would generate an interface `HeroDetails` that could be reused between selections and between operations. This is also important because it allows you to write code that isn't tied to a specific query, like a UI component that knows how to display any hero's details.

The simplest mapping may seem to be to let `R2` and `Luke` extend `HeroDetails`:

```java
class TwoHeroesQuery implements GraphQLQuery<TwoHeroesQuery.Data> {
    interface Data {
        R2 r2();
        interface R2 extends HeroDetails {}
        Luke luke();
        interface Luke extends HeroDetails {}
    }
}

interface HeroDetails {
    String name();
}
```

But this model gets a lot more complicated when nested selection sets are taken into account:

```graphql
query TwoHeroes {
  r2: hero {
    ...HeroDetails
    friends {
      appearsIn
    }
  }
  luke: hero(episode: EMPIRE) {
    ...HeroDetails
    friends {
      id
    }
  }
}

fragment HeroDetails on Character {
  name
  friends {
    name
  }
}
```

This would have to generate something like:

```java
interface R2 extends HeroDetails {
  String name();
  List<Friend> friends();
  interface Friend extends HeroDetails.Friend {
    List<Episode> appearsIn();
  }
}

interface Luke extends HeroDetails {
  String name();
  List<Friend> friends();
  interface Friend extends HeroDetails.Friend {
    String id();
  }
}

interface HeroDetails {
  String name();
  List<Friend> friends();
  interface Friend {
    String name();
  }
}
```

In Swift, this ran into limitations of the type system because types of properties in protocols (the equivalent of interfaces) are invariant. So the `List<Friend> friends()` declarations in subinterfaces would be incompatible. Even if this isn't necessarily a problem in Java, a mapping like this quickly gets complicated, and there may be simpler alternatives.

## Composition over inheritance

The solution we picked in Apollo iOS is to use composition over inheritance, where fragment models are mapped onto separate objects:

```java
interface R2 {  
  List<Friend> friends();
  interface Friend {
    List<Episode> appearsIn();
  }
  Fragments fragments();
  interface Fragments {
    HeroDetails heroDetails();
  }
}

interface Luke {
  List<Friend> friends();
  interface Friend {
    String id();
  }
  Fragments fragments();
  interface Fragments {
    HeroDetails heroDetails();
  }
}

interface HeroDetails {
  String name();
  List<Friend> friends();
  interface Friend {
    String name();
  }
}
```

There is also a more principled reason for treating fragment models as separate objects, what Relay calls *data masking*. The idea is that fragments are an abstraction mechanism that allows you to hide what data is actually requested. A parent UI component (like a list view) doesn't have to know what data a child UI component (like a list item) needs, but only refers to a fragment by name. And at run time, the parent requests a fragment model from the result and passes that to the child. That way, the child can only access data it actually requested and not accidentally rely on data specified by the parent or other child components. Even the parent cannot access the child's data without going through the fragment model.

If we follow the same approach Apollo iOS uses, these fragment models could be accessed by using something like:

`hero().fragments().heroDetails()`

## Type conditions

GraphQL allows you to specify type conditions on inline fragments to query type specific fields:

```graphql
query HeroDetails($episode: Episode) {
  hero(episode: $episode) {
    name
    ... on Human {
      height
    }
    ... on Droid {
      primaryFunction
    }
  }
}
```

One obvious way of mapping these would be to use subinterfaces for concrete object types:

```java
interface Hero {
    String name();
}

interface Hero_Human extends Hero {
    Float height();
}

interface Hero_Droid extends Hero {
    String primaryFunction();
}
```

This runs into the same complications with nested selections we've seen with fragments above however:

```graphql
query {
  hero {
    name
    friends {
      name
    }
    ... on Human {
      height
      friends {
        appearsIn
      }
    }
    ... on Droid {
      primaryFunction
      friends {
        id
      }
    }
  }
}
```

```java
interface Hero {
  String name();
  List<Friend> friends();
  interface Friend {
    String name();
  }
}

interface Hero_Human extends Hero {
  String name();
  List<Friend> friends();
  interface Friend extends Hero.Friend {
    List<Episode> appearsIn();
  }
}

interface Hero_Droid extends Hero {
  String name();
  List<Friend> friends();
  interface Friend extends Hero.Friend {
    String id();
  }
}
```

Again, we could use composition over inheritance here instead:

```java
interface Hero {
  AsHuman asHuman();
  interface AsHuman {
    String name();
    Float height();
  }
  AsHuman asDroid();
  interface AsDroid {
    String name();
    String primaryFunction();
  }
}
```

These `as<>` properties are of course nullable or could return optionals.

Named fragments can also specify type conditions:

```graphql
query TwoHeroes {
  r2: hero {
    ...HumanDetails
    ...DroidDetails
  }
  luke: hero(episode: EMPIRE) {
    ...HumanDetails
    ...DroidDetails
  }
}

fragment HumanDetails on Human {
  name
  height
}

fragment DroidDetails on Droid {
  name
  primaryFunction
}
```

In a composition model, these map naturally onto nullable/optional properties:

`hero().fragments().humanDetails()` (can be null)

## Unique nested type names

An unexpected complication in Java seems to be that nested types can never have the same simple name as an enclosing type. I'm not sure I understand the reasoning here (couldn't scoping rules or qualified names still differentiate between them?), but it is part of the language spec:

> It is a compile-time error if a class has the same simple name as any of its enclosing classes or interfaces.
http://docs.oracle.com/javase/specs/jls/se7/html/jls-8.html#jls-8.1

So unless we can find a way around this, we may have to give up on nesting and generate prefixed interface names ourselves.

See [PR#5](https://github.com/apollostack/apollo-android/pull/5) for the related discussion.

## Caching and consistency management

Many apps need the ability to cache data, either in memory or in persistent storage. Although you could cache individual responses, this doesn't give you many of the benefits of a more fine grained caching model. The same piece of data may occur in multiple responses, and not only would response based caching request and store the same content twice, you also wouldn't be able to guarantee consistency between queries.

### Normalization

This is why most GraphQL clients rely on [normalized caching](http://dev.apollodata.com/core/how-it-works.html#normalize).

A useful mental model for GraphQL is that of query results as trees, that navigate a conceptual data graph (see [this blog post](https://dev-blog.apollodata.com/the-concepts-of-graphql-bc68bd819be3#.eeiz504lj).

Every piece of data in a GraphQL response can be identified by a query path from the root. In the query below, examples of query paths would be `hero.name` or `hero.friends[2].name`.

```graphql
query HeroAndFriendsNames {
  hero {
    name {
      friends {
        name
      }
    }
  }
}
```

Of course, query paths also need to take field arguments into account:

```graphql
query HeroName {
  hero(episode: JEDI) {
    name
  }
}
```

Here, the query path is `hero(episode:JEDI).name`, and that is (potentially) different from `hero(episode:EMPIRE).name`.

Using query paths as cache keys gives us a way to decompose every result tree into flat records without relying on any additional conventions. Of course, in many cases this is not enough to avoid data duplication completely.

In the Star Wars schema for instance, `hero`, `hero(episode:JEDI)` and `hero(episode:NEWHOPE)` refer to the same object (R2-D2), but these records would be cached separately. This is why schemas often include an `id` or other identifier field for types.

Relay [relies on `id` to be always present and to be globally unique (so between types)](https://facebook.github.io/relay/docs/graphql-object-identification.html#content). Apollo allows you to specify your own [`dataIdFromObject` function](http://dev.apollodata.com/react/cache-updates.html#dataIdFromObject).

### Runtime knowledge of query structure

A consequence of this model is that computing a cache key for normalized caching can't just rely on the response. Let's say you receive this result:

```json
{
  "data": {
    "r2": {
      "name": "R2-D2"
    },
    "luke": {
      "name": "Luke Skywalker"
    }
  }
}
```

In order to know the cache key for `r2` or `luke`, we need to know what field they are an alias for. In this case, `r2` should be cached under `hero`, and `luke` under `hero(episode:EMPIRE`:

```graphql
query TwoHeroes {
  r2: hero {
    name
  }
  luke: hero(episode: EMPIRE) {
    name
  }
}

```

So we need runtime knowledge of the original query structure in order to perform normalized caching. One option would be to keep a separate representation of the query structure, as JSON for example. But that means we'd have to read in and traverse that structure when we parse a response, and we already have a representation of the query structure in the form of the generated model code.

### Interleaving parsing and caching

We'll have to discuss how this translates to what we're trying to do in Apollo Android, but in Apollo iOS the solution we've converged on is to interleave parsing and caching.

Every model is able to instantiate itself from a `GraphQLResultReader` that can be used to read individual field values from either a network response or the normalized cache. When reading from a network response, it is also responsible for keeping track of the normalized records.

This means the generated models for the `TwoHeroes` query below are the only query-specific code we need at runtime to both parse and cache a response:

```swift
public struct Data: GraphQLMappable {
  public let r2: R2?
  public let luke: Luke?

  public init(reader: GraphQLResultReader) throws {
    r2 = try reader.optionalValue(for: Field(responseName: "r2", fieldName: "hero"))
    luke = try reader.optionalValue(for: Field(responseName: "luke", fieldName: "hero", arguments: ["episode": "EMPIRE"]))
  }

  public struct R2: GraphQLMappable {
    public let name: String

    public init(reader: GraphQLResultReader) throws {
      name = try reader.value(for: Field(responseName: "name"))
    }
  }

  public struct Luke: GraphQLMappable {
    public let name: String

    public init(reader: GraphQLResultReader) throws {
      name = try reader.value(for: Field(responseName: "name"))
    }
  }
}
```

This is  pretty efficient because most of the code is static and we don't need to traverse any additional structures.

Another benefit is that the same code can be used to read from the normalized cache, simply by passing in a different `reader`. The way this works is that a `GraphQLResultReader` relies on a resolver function that defines where the data comes from. That means the parsing code doesn't rely on the response structure and can also follow references and read from flat records.  

We'll have to see how this maps onto the Android deserialization and value type libraries we'd like to support. 

It seems `GraphQLResultReader` is very similar to Android's `JSONReader`, except that it allows you to read fields by name. (It keeps a stack of JSON objects, so it isn't fully streaming, but you wouldn't need the complete response in memory either.)

### Variables

We sometimes need the runtime value of a variable to compute the cache key, as in this query:

```graphql
query HeroName($episode: Episode) {
  hero(episode: $episode) {
    name
  }
}
```

This hasn't been implemented yet in Apollo iOS, but the idea is that the `reader` also gives you access to the variables: 

```swift
hero = try reader.optionalValue(for: Field(responseName: "hero", arguments: ["episode": reader.variables["episode"]]))
```

### Parsing fragments

Fragments are also treated as models and get instantiated from a `reader` as usual. But the parent will have to define and instantiate the `fragments` container as well:

```swift
public init(reader: GraphQLResultReader) throws {
  let heroDetails = try HeroDetails(reader: reader)
  fragments = Fragments(heroDetails: heroDetails)
}

public struct Fragments {
  public let heroDetails: HeroDetails
}
```

If a fragment (or inline fragment) has a type condition, we have to make sure to only parse it when the `__typename` matches:

```swift
public init(reader: GraphQLResultReader) throws {
  __typename = try reader.value(for: Field(responseName: "__typename"))
  name = try reader.value(for: Field(responseName: "name"))

  asHuman = try AsHuman(reader: reader, ifTypeMatches: __typename)
  asDroid = try AsDroid(reader: reader, ifTypeMatches: __typename)
}
```

### Cache keys depending on `__typename`

This is a really weird edge case that I've been using to validate the approach:

```graphql
query HeroParentTypeDependentField($episode: Episode) {
  hero(episode: $episode) {
    name
    ... on Human {
      friends {
        name
        ... on Human {
          height(unit: FOOT)
        }
      }
    }
    ... on Droid {
      friends {
        name
        ... on Human {
          height(unit: METER)
        }
      }
    }
  }
}

```

If the `episode` variable is `EMPIRE`, the response will include:

```json
{
  "name": "Han Solo",
  "height": 5.905512
}
```

But if it is `JEDI`, it will be:

```json
{
  "name": "Han Solo",
  "height": 1.8
}
```

The only way to know whether to cache `height` as `height(unit:FOOT)` or `height(unit:METER)` is to know the `__typename` of the hero (either `Human` or `Droid`), and you won't know this until runtime.

But because we're interleaving parsing and caching, we'll do the right thing by default without any additional effort.

## Programming model

The API you use to fetch queries should allow you to take advantage of the cache transparently. A `fetchQuery` method could allow you to specify a caching policy for example. By default, you probably want to read a query result from the cache if all data is there, but sometimes you want to make sure you always get the most recent data or have other specific policies you'd like to enforce.

But while fetching a result is a one time operation, you often want to subscribe to a query. This means you're interested not just in the current result, but in any updates to that result as well. You could expose a `watchQuery` method for example, where the callback can be invoked multiple times. The client will then make sure that every time it learns a piece of data in your result changes, you get the updated result delivered.

It can learn data has changed by using polling (you could specify a `pollingInterval`, and these request can also be batched) or by using a real time updating mechanism like GraphQL subscriptions over WebSockets.

Besides improving performance by avoiding server requests, another important benefit of normalized caching is managing consistency between queries. If the result of another query includes the same piece of data, that updates the normalized store, and will then propagate to all dependent watched queries.

`ApolloClient` has an associated `ApolloStore`, which implements a publish/subscribe mechanism for records. The store design is similar to that described in [this talk by Joe Savona](https://www.youtube.com/watch?v=1Fg_QtzI7SU). 

The idea is that the store is responsible for keeping track of dependencies, but delegates to a `Cache` to perform the actual storage. So we'd be able to plug in different cache implementations, either in memory or using different kinds of persistent key value stores.
