package com.apollostack.compiler

import com.apollostack.api.Query
import com.squareup.javapoet.ClassName
import java.util.*

object ClassNames {
  val STRING: ClassName = ClassName.get(String::class.java)
  val LIST: ClassName = ClassName.get(List::class.java)
  val COLLECTIONS: ClassName = ClassName.get(Collections::class.java)
  val ARRAYS: ClassName = ClassName.get(Arrays::class.java)
  val QUERY: ClassName = ClassName.get(Query::class.java)
}