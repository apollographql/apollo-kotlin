//@formatter:off
package com.example

import com.apollographql.apollo.api.Optional
import com.example.generated.type.AddressInput
import com.example.generated.type.PersonInput

fun addNameToArgs() {
  PersonInput(
      firstName = Optional.present("John"),
      Optional.present("Doe"),
      Optional.present(42),
      Optional.present(AddressInput(street = Optional.present("street"))),
  )

  PersonInput(
      Optional.Present("John"),
      Optional.present("Doe"),
  )

  PersonInput(
      Optional.Absent,
      Optional.absent(),
      Optional.present(42),
  )

  PersonInput(
      Optional.Absent,
      Optional.absent(),
      getAge(),
  )
}

fun convertToBuilder() {
  PersonInput(
      firstName = Optional.present("John"),
      Optional.present("Doe"),
      Optional.present(42),
      Optional.present(AddressInput(street = Optional.present("street"))),
  )

  PersonInput(
      Optional.Present("John"),
      Optional.present("Doe"),
  )

  PersonInput(
      Optional.Absent,
      Optional.absent(),
      Optional.present(42),
  )

  PersonInput(
      Optional.Absent,
      Optional.absent(),
      getAge(),
  )
}


fun getAge(): Optional<Int?> {
  return Optional.Present(42)
}
