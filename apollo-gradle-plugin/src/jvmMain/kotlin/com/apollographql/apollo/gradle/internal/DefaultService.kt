package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilerParams
import com.apollographql.apollo.gradle.api.Introspection
import com.apollographql.apollo.gradle.api.Service
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class DefaultService @Inject constructor(val objects: ObjectFactory, val name: String)
  : CompilerParams by objects.newInstance(DefaultCompilerParams::class.java), Service {

  override val schemaPath = objects.property(String::class.java)

  override val sourceFolder = objects.property(String::class.java)

  override val exclude = objects.listProperty(String::class.java)

  var introspection: DefaultIntrospection? = null

  override fun introspection(configure: Action<in Introspection>) {
    val introspection = objects.newInstance(DefaultIntrospection::class.java, objects)

    if (this.introspection != null) {
      throw IllegalArgumentException("there must be only one introspection block")
    }

    configure.execute(introspection)

    if (!introspection.endpointUrl.isPresent) {
      throw IllegalArgumentException("introspection must have a url")
    }

    this.introspection = introspection
  }
}
