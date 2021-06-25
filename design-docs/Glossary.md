# Codegen Glossary

## Glossary

A small Glossary of the terms used during codegen. The [GraphQL Spec](https://spec.graphql.org/draft/) does a nice job of defining the common terms like `Field`, `SelectionSet`, etc... so I'm not adding these terms here. But it misses some concepts that we bumped across during codegen and that I'm trying to clarify here.


### Field tree

Every GraphQL operation queries a tree of fields starting from the root. Fields can be of scalar or compound type.


### Fragment tree

Each field in the field tree contains (possibly nested) fragments and inline fragments that define a tree of fragments. This makes a GraphQL query a somewhat 2-dimensional tree or “tree of tree” with a tree of fragments at each field node.


### Field set

A selection set containing only fields and no fragments. When combined they form a field tree.


### Response shape

This is the shape of the actual json as returned by the server. A given query can have multiple shapes depending the different type conditions at each field. While Field trees are in the GraphQL domain, Response shapes are in the Json domain and different Field trees can have the same response shape (http://spec.graphql.org/draft/#SameResponseShape())


### Scalar type

A leaf type


### Composite type

A type that contains sub fields. It is an interface, object or union type


### Concrete type

Synonym for object type


### Possible types

Given a type condition, all the concrete types that can satisfy this type condition.


### Type set

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



### Parent Type

the type condition of the enclosing fragment in the fragment tree, if any or the type of the parent field if none

Example:

```
{
    animal {
        ... on Pet {
            ...warmBlooded {
                temperature
            }
        }
    }
}
```

`Pet` is the ParentType of the `warmBlooded` fragment
`WarmBlooded` is the ParentType of the `temperature` field



### Polymorphic field

A field that can take several shapes
