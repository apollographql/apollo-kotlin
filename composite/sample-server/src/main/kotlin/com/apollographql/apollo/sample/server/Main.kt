package com.apollographql.apollo.sample.server

import com.apollographql.apollo.sample.server.DefaultApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
  runApplication<DefaultApplication>(*args)
 // go to http://localhost:8080/playground for playground
}

