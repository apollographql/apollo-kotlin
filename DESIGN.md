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
