package com.example

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Optional.Absent
import com.example.generated.type.FindUserInput

fun oneOfConstructor() {
  FindUserInput()

  FindUserInput(
      email = Optional.present("a@a.com"),
      name = Optional.present("John"),
  )

  FindUserInput(
      email = Optional.present("a@a.com")
  )

  val absentEmail: Absent = Optional.absent()
  FindUserInput(
      email = absentEmail
  )
}

fun oneOfBuilder() {
  FindUserInput.Builder()
      .email("a@a.com")
      .name("John")
      .build()

  FindUserInput.Builder()
      .email("a@a.com")
      .build()

  FindUserInput.Builder()
      .email(null)
      .build()

  val someNullableEmail: String? = null
  FindUserInput.Builder()
      .email(someNullableEmail)
      .build()
}
