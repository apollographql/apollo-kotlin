package com.apollographql.android.compiler

import java.io.File
import java.util.regex.Pattern

/**
 * Various string transformation utilities. Transforms singular words to plural and folder paths to packages
 *
 * The singularization methods were heavily based off rogueweb <link> https://code.google.com/archive/p/rogueweb/source</link>
 */

fun String.formatPackageName(): String {
  val parts = split(File.separatorChar)
  (parts.size - 1 downTo 2)
      .filter { parts[it - 2] == "src" && parts[it] == "graphql" }
      .forEach { return parts.subList(it + 1, parts.size).dropLast(1).joinToString(".") }
  throw IllegalArgumentException("Files must be organized like src/main/graphql/...")
}

fun String.singularize(): String {
  if (uncountable().contains(this.toLowerCase())) return this

  val irregular = irregular().firstOrNull() { this.toLowerCase() == it.component2() }
  if (irregular != null) return irregular.component1()

  if (singularizationRules().find { match(it.component1(), this) } == null) return this

  val rule = singularizationRules().last { match(it.component1(), this) }
  return Pattern.compile(rule.component1(), Pattern.CASE_INSENSITIVE).matcher(this).replaceAll(rule.component2())
}

private fun singularizationRules(): List<Pair<String, String>> {
  return listOf(
      "s$" to "",
      "(s|si|u)s$" to "$1s",
      "(n)ews$" to "$1ews",
      "([ti])a$" to "$1um",
      "((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$" to "$1$2sis",
      "(^analy)ses$" to "$1sis",
      "(^analy)sis$" to "$1sis",
      "([^f])ves$" to "$1fe",
      "(hive)s$" to "$1",
      "(tive)s$" to "$1",
      "([lr])ves$" to "$1f",
      "([^aeiouy]|qu)ies$" to "$1y",
      "(s)eries$" to "$1eries",
      "(m)ovies$" to "$1ovie",
      "(x|ch|ss|sh)es$" to "$1",
      "([m|l])ice$" to "$1ouse",
      "(bus)es$" to "$1",
      "(o)es$" to "$1",
      "(shoe)s$" to "$1",
      "(cris|ax|test)is$" to "$1is",
      "(cris|ax|test)es$" to "$1is",
      "(octop|vir)i$" to "$1us",
      "(octop|vir)us$" to "$1us",
      "(alias|status)es$" to "$1",
      "(alias|status)$" to "$1",
      "^(ox)en" to "$1",
      "(vert|ind)ices$" to "$1ex",
      "(matr)ices$" to "$1ix",
      "(quiz)zes$" to "$1")
}

private fun irregular(): List<Pair<String, String>> {
  return listOf(
      "person" to "people",
      "man" to "men",
      "goose" to "geese",
      "child" to "children",
      "sex" to "sexes",
      "move" to "moves")
}

private fun uncountable(): List<String> {
  return listOf("equipment", "information", "rice", "money", "species", "series", "fish", "sheep")
}

private fun match(pattern: String, word: String): Boolean {
  return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(word).find()
}
