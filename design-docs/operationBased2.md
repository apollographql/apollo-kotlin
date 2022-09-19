## Example 1

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    species
  }
}
</pre></td>
<td><pre lang="kotlin">
public data class Data(public val animal: Animal?)
data class Animal(val species: String)
</pre></td>
</tr>
</table>

## Example 2

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    # Field type = Inline fragment type
    ... on Animal {
      species
    }
  }
}
</pre></td>
<td><pre lang="kotlin">
public data class Data(public val animal: Animal?)
data class Animal(val onAnimal: OnAnimal)
</pre></td>
</tr>
</table>

## Example 3

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  cat {
    # Field type Cat is subtype of inline fragment
    ... on Animal {
      species
    }
  }
}
</pre></td>
<td><pre lang="kotlin">
public data class Data(public val cat: Cat?)
data class Cat(val onAnimal: OnAnimal)
</pre></td>
</tr>
</table>

## Example 4

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    ... on Cat {
      species
    }
  }
}
</pre></td>
<td><pre lang="kotlin">
public data class Data(public val animal: IAnimal?)

sealed interface Animal { val onCat: Animal.OnCat? }
data class CatAnimal(val onCat: Animal.OnCat): Animal
data class OtherAnimal(val onCat: Animal.OnCat?): Animal
</pre></td>
</tr>
</table>

## Example 5

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    ... CatFragment
  }
}
fragment CatFragment on Cat {
  species
}
</pre></td>
<td><pre lang="kotlin">
public data class Data(public val animal: IAnimal?)

sealed interface Animal { val catFragment: CatFragment? }
data class CatAnimal(val catFragment: CatFragment): Animal
data class OtherAnimal(val catFragment: CatFragment?): Animal

</pre></td>
</tr>
</table>

## Example 6

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    species
    ... on WarmBlooded {
      temperature
    }
    ... on Pet {
      name
    }
  }
}
</pre></td>
<td><pre lang="kotlin">
public data class Data(public val animal: IAnimal?)

// Interface Model
sealed interface Animal { 
  val onWarmBlooded: Animal.OnWarmBlooded?
  val onPet: Animal.OnPet?
}
// 3 TypeSet Models
data class WarmBloodedAnimal(
  val onWarmBlooded: Animal.OnWarmBlooded
  val onPet: Animal.OnPet?  
): Animal
data class PetAnimal(
  val onWarmBlooded: Animal.OnWarmBlooded?
  val onPet: Animal.OnPet
): Animal
data class WarmBloodedPetAnimal(
  val onWarmBlooded: Animal.OnWarmBlooded
  val onPet: Animal.OnPet
): Animal
// Fallback Model
data class OtherAnimal(
  val onWarmBlooded: Animal.OnWarmBlooded?
  val onPet: Animal.OnPet?  
): Animal

</pre></td>
</tr>
</table>


## Example 7

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    ... on Animal {
      species
      # Nested WarmBlooded
      ... on WarmBlooded {
        temperature
      }
      # Nested Pet
      ... on Pet {
        name
      }
    }
  }
}
</pre></td>
<td><pre lang="kotlin">

// animal is not generated as polymorphic model because the `... on Animal` inline fragment type condition is always satisfied
public data class Data(public val animal: Animal?)
data class Animal(val onAnimal: OnAnimal)

public sealed interface OnAnimal { 
  val species: String,
  val onWarmBlooded: OnWarmBlooded?,
  val onPet: OnPet?
}
data class WarmBloodedOnAnimal(
  val species: String, 
  val onWarmBlooded: OnWarmBlooded,
  val onPet: OnPet?
): OnAnimal
data class PetOnAnimal(
  val species: String,
  val onWarmBlooded: OnWarmBlooded?,
  val onPet: OnPet,
): OnAnimal
data class WarmBloodedPetOnAnimal(
  val species: String,
  val onWarmBlooded: OnWarmBlooded,
  val onPet: OnPet,
): OnAnimal
data class OtherOnAnimal(
  val species: String,
  val onWarmBlooded: OnWarmBlooded?,
  val onPet: OnPet?,
): OnAnimal
</pre></td>
</tr>
</table>

## Example 8

<table>
<tr><th>GraphQL</th><th>Kotlin</th></tr>
<tr>
<td><pre lang="graphql">
query TestOperation {
  animal {
    ... AnimalFragment
  }
}
fragment AnimalFragment on Animal {
  species
  ... on WarmBlooded {
    temperature
  }
  ... on Pet {
    name
  }
}
</pre></td>
<td><pre lang="kotlin">

public data class Data(public val animal: Animal?)
data class Animal(val animalFragment: AnimalFragment)

public sealed interface AnimalFragment { 
  val species: String,
  val onWarmBlooded: OnWarmBlooded?,
  val onPet: OnPet?
}
data class WarmBloodedAnimalFragment(
  val species: String, 
  val onWarmBlooded: OnWarmBlooded,
  val onPet:OnPet?
): AnimalFragment
data class PetAnimalFragment(
  val species: String,
  val onWarmBlooded: OnWarmBlooded?,
  val onPet: OnPet,
): AnimalFragment
data class WarmBloodedPetAnimalFragment(
  val species: String,
  val onWarmBlooded: OnWarmBlooded,
  val onPet: OnPet,
): AnimalFragment
data class OtherAnimalFragment(
  val species: String,
  val onWarmBlooded: OnWarmBlooded?,
  val onPet: OnPet?,
): AnimalFragment
</pre></td>
</tr>
</table>

