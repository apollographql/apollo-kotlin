import com.apollographql.apollo3.compiler.DefaultApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("kotlin.reflect"))
}

apollo {
  service("internalize") {
    packageName.set("hooks.internalize")
    compilerKotlinHooks.set(InternalHooks(setOf("NodeQuery")))
  }

  service("defaultnullvalues") {
    packageName.set("hooks.defaultnullvalues")
    compilerKotlinHooks.set(DefaultNullValuesHooks())
  }

  service("typenameinterface") {
    packageName.set("hooks.typenameinterface")
    compilerKotlinHooks.set(TypeNameInterfaceHooks("hooks.typenameinterface.HasTypeName"))
  }

  service("prefixnames") {
    packageName.set("hooks.prefixnames")
    compilerKotlinHooks.set(PrefixNamesHooks("GQL"))
  }
}

/**
 * Adds an `internal` modifier to specific operations.
 */
private class InternalHooks(internalOperations: Set<String>) : DefaultApolloCompilerKotlinHooks() {
  private val internalTypes = internalOperations + internalOperations.map { it + "_ResponseAdapter" } + internalOperations.map { it + "Selections" }

  override val version = "0"

  override fun postProcessFileSpec(fileSpec: FileSpec): FileSpec {
    println(fileSpec.name)
    return fileSpec
        .toBuilder()
        .apply {
          members.replaceAll { member ->
            if (member is TypeSpec) {
              if (member.name in internalTypes) {
                member.toBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .build()
              } else {
                member
              }
            } else {
              member
            }
          }
        }
        .build()
  }
}

/**
 * Adds a default value to data class fields that are not nullable.
 */
private class DefaultNullValuesHooks : DefaultApolloCompilerKotlinHooks() {
  override val version = "0"

  override fun postProcessFileSpec(fileSpec: FileSpec): FileSpec {
    return fileSpec
        .toBuilder()
        .apply {
          members.replaceAll { member ->
            if (member is TypeSpec) {
              member.addDefaultValueToNullableProperties()
            } else {
              member
            }
          }
        }
        .build()
  }

  private fun TypeSpec.addDefaultValueToNullableProperties(): TypeSpec {
    return toBuilder()
        .apply {
          // Only care about data classes
          if (modifiers.contains(KModifier.DATA)) {
            primaryConstructor(
                primaryConstructor!!.toBuilder()
                    .apply {
                      parameters.replaceAll { param ->
                        if (param.type.isNullable) {
                          param.toBuilder()
                              .defaultValue(CodeBlock.of("null"))
                              .build()
                        } else {
                          param
                        }
                      }
                    }
                    .build()
            )
          }

          // Recurse on nested types
          typeSpecs.replaceAll { typeSpec ->
            typeSpec.addDefaultValueToNullableProperties()
          }
        }
        .build()
  }
}

/**
 * Adds a super interface to models that expose __typename.
 */
private class TypeNameInterfaceHooks(private val interfaceName: String) : DefaultApolloCompilerKotlinHooks() {
  override val version = "1"

  override fun postProcessFileSpec(fileSpec: FileSpec): FileSpec {
    return fileSpec
        .toBuilder()
        .apply {
          members.replaceAll { member ->
            if (member is TypeSpec) {
              member.addSuperInterfaceOnType()
            } else {
              member
            }
          }
        }
        .build()
  }

  private fun TypeSpec.addSuperInterfaceOnType(): TypeSpec {
    return toBuilder()
        .apply {
          var hasTypeName = false
          propertySpecs.replaceAll { propertySpec ->
            if (propertySpec.name == "__typename") {
              hasTypeName = true
              propertySpec.toBuilder()
                  .addModifiers(KModifier.OVERRIDE)
                  .build()
            } else {
              propertySpec
            }
          }
          if (hasTypeName) {
            // XXX addSuperinterface(ClassName.bestGuess(interfaceName)) doesn't compile for an unknown reason, but this is equivalent
            superinterfaces[ClassName.bestGuess(interfaceName)] = null
          }

          // Recurse on nested types
          typeSpecs.replaceAll { typeSpec ->
            typeSpec.addSuperInterfaceOnType()
          }
        }
        .build()
  }
}

/**
 * Prefix generated class names with the specified [prefix].
 */
private class PrefixNamesHooks(private val prefix: String) : DefaultApolloCompilerKotlinHooks() {
  override val version = "0"

  override fun postProcessFileSpec(fileSpec: FileSpec): FileSpec {
    return fileSpec
        .toBuilder(name = prefix + fileSpec.name)
        .apply {
          members.replaceAll { member ->
            if (member is TypeSpec) {
              member.toBuilder(name = member.name?.let { prefix + it }).build()
            } else {
              member
            }
          }
        }
        .build()
  }

  override fun overrideResolvedType(key: ResolverKey, resolved: ClassName?): ClassName? {
    return resolved?.let {
      // Don't prefix nested types.
      // e.g. MyQuery -> PrefixMyQuery
      //      MyQuery.Data -> PrefixMyQuery.Data
      if (it.simpleNames.size == 1) {
        ClassName(it.packageName, listOf(prefix + it.simpleName))
      } else {
        ClassName(it.packageName, it.simpleNames.mapIndexed { idx, s -> if (idx == it.simpleNames.lastIndex) s else prefix + s })
      }
    }
  }
}
