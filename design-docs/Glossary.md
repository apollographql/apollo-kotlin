# Glossary

The [GraphQL Spec](https://spec.graphql.org/draft/) does a nice job of defining common terms like `Field`, `SelectionSet`, etc. but here are a few other concepts that the library deals with, and their definition.

### Response shape

This is the shape of the actual json as returned by the server. A given query can have multiple shapes depending on the different type conditions at each field. While selection sets are in the GraphQL domain, Response shapes are in the Json domain and different selection sets can have the same response shape (http://spec.graphql.org/draft/#SameResponseShape())

### Raw type

The raw type is the named type without any list/nonnull wrapper types

### Leaf type

A leaf type that doesn't contain fields or input fields. It's either a scalar or an enum


### Composite type

A type that contains subfields. It is an interface, object or union type


### Concrete type

Synonym for object type


### Possible types

Given a type condition, all the concrete types that can satisfy this type condition.


### typeSet

A set of type conditions from nested fragments and inline fragments. A type set can be abstract if no possible type will implement that exact type set. It is concrete else.

Example:

```
{
    animal {
        species
        ... on Pet {
            name
        }
        ... on WarmBlooded {
            temperature
        }
    }
}
```

The actual type sets depend on the different types. 

```
type Lion implements Animal & WarmBlooded
type Cat implements Animal & WarmBlooded & Pet

- [Animal, Pet, WarmBlooded] => Cat (concrete)
- [Animal, WarmBlooded] => Lion (concrete)
// To access `species` in a polymotphic way and account for new types being added
- [Animal] => (abstract)
```

Abstract type sets can generate both an interface and an implementation. If you have a Turtle pet 🐢:

```
type Lion implements Animal & WarmBlooded
type Cat implements Animal & WarmBlooded & Pet
type Turtle implements Animal & Pet
type Panther implements Animal & WarmBlooded

- [Animal, Pet, WarmBlooded] => Cat (concrete)
- [Animal, WarmBlooded] => [Lion, Panther] (concrete)
// To access `species` in a polymotphic way and account for new types being added
- [Animal] => (abstract)
// Will be generated as both an implementation (for the Turtle possible type) and also as an interface to access common Pet fields with Cat
- [Animal, Pet] => [Turtle] (concrete)
```

### parentType

the type condition of the enclosing fragment if any or else the type of the parent field 

Example:

```
{
    animal {
        # parentType: Animal
        ... on Pet {
            # parentType: Pet
            ...warmBlooded {
                # parentType: WarmBlooded
                temperature
            }
        }
    }
}
```

### Polymorphic field

A field that can take several shapes

### Record

A shallow map of a response object. Nested objects in the map values are replaced by a cache reference to another Record.  

### Cache key

A unique identifier for a Record.
By default it is the path formed by all the field keys from the root of the query to the field referencing the Record.
To avoid duplication the Cache key can also be computed from the Record contents, usually using its key fields.

### Field key

A key that uniquely identifies a field within a Record. By default composed of the field name and the arguments passed to it.

### Key fields

Fields that are used to compute a Cache key for an object.

### Pagination arguments

Field arguments that control pagination, e.g. `first`, `after`, etc. They should be omitted when computing a field key so different pages can be merged into the same field.

