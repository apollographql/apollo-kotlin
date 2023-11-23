package com.example

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Optional.Absent
import com.example.generated.type.FindUserInput

fun oneOf() {
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
