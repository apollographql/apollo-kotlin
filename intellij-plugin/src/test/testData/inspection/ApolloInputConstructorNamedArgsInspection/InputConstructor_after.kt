//@formatter:off
package com.example

import com.apollographql.apollo.api.Optional
import com.example.generated.type.AddressInput
import com.example.generated.type.PersonInput

fun addNameToArgs() {
  PersonInput(
      firstName = Optional.present("John"),
      lastName = Optional.present("Doe"),
      age = Optional.present(42),
      address = Optional.present(AddressInput(street = Optional.present("street"))),
  )

  PersonInput(
      firstName = Optional.Present("John"),
      lastName = Optional.present("Doe"),
  )

  PersonInput(
      firstName = Optional.Absent,
      lastName = Optional.absent(),
      age = Optional.present(42),
  )

  PersonInput(
      firstName = Optional.Absent,
      lastName = Optional.absent(),
      age = getAge(),
  )
}

fun convertToBuilder() {
  PersonInput.Builder()
.firstName("John")
.lastName("Doe")
.age(42)
.address(AddressInput(street = Optional.present("street")))
.build()

  PersonInput.Builder()
.firstName("John")
.lastName("Doe")
.build()

  PersonInput.Builder()
.age(42)
.build()

  PersonInput.Builder()
.age(getAge().getOrNull())
.build()
}


fun getAge(): Optional<Int?> {
  return Optional.Present(42)
}
