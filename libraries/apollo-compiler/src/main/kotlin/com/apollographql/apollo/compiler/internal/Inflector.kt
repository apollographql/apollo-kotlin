package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import java.util.regex.Pattern

/**
 * Various string transformation utilities. Transforms singular words to plural and folder paths to packages
 *
 * The singularization methods were heavily based off ruby on rails <link>https://github.com/rails/rails/blob/master/activesupport/lib/active_support/inflections.rb</link>
 */

internal fun String.singularize(): String {
  if (uncountable.contains(this.lowercase())) return this

  if (exclude.contains(this.lowercase())) return this

  val capitalized = first().isUpperCase()
  val irregular = irregular.firstOrNull { this.lowercase() == it.component2() }
  if (irregular != null) return irregular.component1().let {
    if (capitalized) it.capitalizeFirstLetter() else it
  }

  val rule = singularizationRules.lastOrNull { it.component1().matcher(this).find() }
  if (rule != null) {
    return rule.component1().matcher(this).replaceAll(rule.component2())
  }

  return this
}

private val singularizationRules = listOf(
    "s$" to "",
    "(ss)$" to "$1",
    "([ti])a$" to "$1um",
    "(^analy)(sis|ses)$" to "$1sis",
    "([^f])ves$" to "$1fe",
    "(hive)s$" to "$1",
    "(tive)s$" to "$1",
    "([lr])ves$" to "$1f",
    "([^aeiouy]|qu)ies$" to "$1y",
    "(s)eries$" to "$1eries",
    "(m)ovies$" to "$1ovie",
    "(x|ch|ss|sh)es$" to "$1",
    "^(m|l)ice$" to "$1ouse",
    "(bus)(es)?$" to "$1",
    "(o)es$" to "$1",
    "(shoe)s$" to "$1",
    "(cris|test)(is|es)$" to "$1is",
    "^(a)x[ie]s$" to "$1xis",
    "(octop|vir)(us|i)$" to "$1us",
    "(alias|status)(es)?$" to "$1",
    "^(ox)en/$" to "$1",
    "(vert|ind)ices$" to "$1ex",
    "(matr)ices$" to "$1ix",
    "(quiz)zes$" to "$1",
    "(database)s$" to "$1"
).map { Pattern.compile(it.first, Pattern.CASE_INSENSITIVE) to it.second }

private val irregular = listOf(
    "person" to "people",
    "man" to "men",
    "child" to "children",
    "sex" to "sexes",
    "move" to "moves",
    "zombie" to "zombies",
    "goose" to "geese"
)

private val uncountable = listOf(
    "equipment",
    "information",
    "rice",
    "money",
    "species",
    "series",
    "fish",
    "sheep",
    "jeans",
    "police"
)

private val exclude = listOf("data")
