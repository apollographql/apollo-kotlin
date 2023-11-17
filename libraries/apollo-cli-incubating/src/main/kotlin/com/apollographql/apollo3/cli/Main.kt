package com.apollographql.apollo3.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintCompletionMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.lang.reflect.InvocationTargetException
import kotlin.system.exitProcess

private class MainCommand(name: String) : CliktCommand(name = name) {
  init {
    subcommands(DownloadSchemaCommand())
    subcommands(PublishSchemaCommand())
  }

  override fun run() {
    System.err.println("apollo-cli is experimental and might change in backward incompatible ways")
  }
}

private class MyCompletionCommand : CliktCommand(name = "generate-completion") {
  val shell by option().required()
  val name by option().required()

  override fun run() {
    // See https://github.com/ajalt/clikt/issues/355
    val command = MainCommand(name).subcommands(PlaceHolderCommand())
    command.parse(arrayOf("placeholder"))
    val clazz = Class.forName("com.github.ajalt.clikt.completion.CompletionGenerator")!!
    val instanceField = clazz.declaredFields.single { it.name == "INSTANCE" }
    val instance = instanceField.get(clazz)
    val method = clazz.declaredMethods.single { it.name == "throwCompletionMessage" }
    try {
      method.invoke(instance, command, shell)
    } catch (e: InvocationTargetException) {
      val cause = e.cause
      if (cause is PrintCompletionMessage) {
        println(cause.message)
        exitProcess(0)
      }
      throw e
    }
  }
}

private class PlaceHolderCommand : CliktCommand(name = "placeholder") {
  override fun run() {
    println("This command is only present for technical reasons but doesn't do anything")
  }
}

fun main(args: Array<String>) {
  MainCommand("apollo-cli")
      .subcommands(MyCompletionCommand())
      .main(args)
}

