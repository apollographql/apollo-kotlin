This page gives some context about how __typename is added in queries.

Historically, __typename was added on most of the selection sets. While this makes working with fragments and cache easier (because typename is always available), it also created some confusion and overhead (see https://github.com/apollographql/apollo-kotlin/issues/1458).

3.0.0 limited the __typename to selection sets that contains fragments but this created other issues (see https://github.com/apollographql/apollo-kotlin/issues/3672#issuecomment-1007194022)

# Historical algorithms

## 2.x

2.x adds typename on the below selection sets:

* operation definition
* fragment definition
* field
* inline fragments if they contain other inline fragments

## 3.0.0

3.0.0 adds typename on all selection sets that contain fragments

# Issues

## parsing/data overhead

Adding `__typename` too much causes too many bytes to be transferred and CPU cycles to be lost parsing them

## fragment extraction

Extracting fragment should be transparent

Changing from

```graphql
query Foo {
  foo {
    bar
    baz
  }
}
```

to

```graphql
query Foo {
  foo { 
    # 3.0.0 automatically adds __typename here causing a cache miss if the query was stored without __typename before
    ...fooData 
  }
}

fragment fooData on Foo {
  bar
  baz
}
```

shouldn't cause any cache miss

## Cache reading (?)

```graphql
query Hero1 {
  hero {
    id
    name
  }
}
```

```graphql
query Hero2 {
  hero {
    # __typename will be added here to be distinguish between Droid/non-Droid
    id
    ... on Droid {
      name
    }
  }
}
```

Here, `Hero2` will not be readable from the cache even if `Hero1` was fetched successfully before. I'm not 100% sure how much of a problem that is.

If it is a problem, adding `__typename` should be opt-in in all cases so that it doesn't impact non-cache users.

# Proposed algorithm

Only add typename for polymorphic fields.

A field is considered polymorphic if it contains a fragment whose type condition is not a supertype of the field type.

Ex:

```graphql
type Dog implements Animal
interface Animal implements Node

{
  # This is a polymorphic field
  animal {
    # __typename will be added here
    # Dog is not a supertype of Animal
    ... on Dog {
      name
    }
  }
}

{
  # This is a monomorphic field
  dog {
    ... on Animal {
      ... on Node {
        id
      }
    }
  }
}

{
  # This is also monomorphic 
  dog {
    ... on Node {
      # Animal is not a supertype of Node but it's a supertype of Dog
      ... on Animal {
        id
      }
    }
  }
}

{
  # This field is of interface type but is also monomorphic 
  animal {
    name
  }
}
```

This is the bare minimum __typename needed for parsing. Without these cases, it's not possible to know how to parse fragments. 

When using the cache this can lead to some cache misses because __typename is missing. For an example:

First query:
```graphql
{
  animal {
    id
    species
    legs
    isEndangered
  }  
}
```

Second query:

```graphql
{
  animal {
    # this query will require __typename even though everything is in cache already
    id
    ... on Cat {
      species
    }
  }  
}
```

For this reason, we're also adding an option to add fragments on all abstract fields (fields of union or interface type). 


# Implementation notes

The tricky part is that there are 3 places that need to be in sync:

1. adding the typename in the GraphQL document
2. operationBased adapter codegen
3. responseBased adapter codegen

The code was modified so that 2. and 3. do not rely on `__typename` being accessible directly in the model. Instead, they inject code in the adapters that will look it up from the json if needed.

This is all working because 1. should always add more __typename than technically required (or at least in the polymorphic case the exact number of __typename)





