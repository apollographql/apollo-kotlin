@file:Suppress("UNUSED_VARIABLE", "unused")

package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.generated.AnimalsQuery
import com.example.generated.CreatePersonMutation
import com.example.generated.MyEnumQuery
import com.example.generated.fragment.ComputerFields
import com.example.generated.type.MyEnum
import com.example.generated.type.PersonInput

suspend fun main() {
  val apolloClient = ApolloClient.Builder()
      .serverUrl("https://example.com")
      .build()

  val animalsQuery = AnimalsQuery()
  val response = apolloClient.query(animalsQuery).execute()
  println(response.data!!.animals[0].name)
  println(response.data!!.animals[0].onDog?.fieldOnDogAndCat)
  println(response.data!!.animals[0].onDog?.id)

  val computerFields = ComputerFields(
      cpu = "386",
      screen = ComputerFields.Screen(resolution = "640x480"),
      releaseDate = "1992",
  )

  val myEnum = apolloClient.query(MyEnumQuery(Optional.present(MyEnum.VALUE_C))).execute().data!!.myEnum

  val personInput = PersonInput(
      lastName = Optional.Present("Smith")
  )
  val response2 = apolloClient.mutation(
      CreatePersonMutation(
          Optional.present(
              personInput
          )
      )
  ).execute()
}
