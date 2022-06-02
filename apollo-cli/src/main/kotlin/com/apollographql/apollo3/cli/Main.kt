package com.apollographql.apollo3.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

private class MainCommand: CliktCommand() {
  override fun run() {
    println("apollo-cli is experimental and might change in backward incompatible ways")
  }
}

fun main(args: Array<String>) {
  MainCommand()
      .subcommands(DownloadSchemaCommand())
      .subcommands(PublishSchemaCommand())
      .main(args)
}
