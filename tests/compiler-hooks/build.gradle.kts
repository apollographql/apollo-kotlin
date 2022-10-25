import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.hooks.AddInternalCompilerHooks
import com.apollographql.apollo3.compiler.hooks.DefaultApolloCompilerKotlinHooks
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")

  // Note: using the external plugin here to be able to reference KotlinPoet classes
  id("com.apollographql.apollo3.external")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("kotlin.reflect"))
}

apollo {
  service("addinternal") {
    packageName.set("hooks.addinternal")
    compilerKotlinHooks.set(listOf(AddInternalCompilerHooks(".*NodeQuery")))
  }

  service("defaultnullvalues") {
    packageName.set("hooks.defaultnullvalues")
    compilerKotlinHooks.set(listOf(DefaultNullValuesHooks()))
  }

  service("typenameinterface") {
    packageName.set("hooks.typenameinterface")
    compilerKotlinHooks.set(listOf(TypeNameInterfaceHooks("hooks.typenameinterface.HasTypeName")))
  }

  service("prefixnames") {
    packageName.set("hooks.prefixnames")
    compilerKotlinHooks.set(listOf(PrefixNamesHooks("GQL")))
  }

  service("capitalizeenumvalues") {
    packageName.set("hooks.capitalizeenumvalues")
    compilerKotlinHooks.set(listOf(CapitalizeEnumValuesHooks()))
  }
}

/**
 * Adds a default `null` value to data class fields that are nullable.
 */
class DefaultNullValuesHooks : DefaultApolloCompilerKotlinHooks() {
  override val version = "DefaultNullValuesHooks.0"

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
class TypeNameInterfaceHooks(private val interfaceName: String) : DefaultApolloCompilerKotlinHooks() {
  override val version = "TypeNameInterfaceHooks.0{$interfaceName}"

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
            addSuperinterface(ClassName.bestGuess(interfaceName))
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
class PrefixNamesHooks(private val prefix: String) : DefaultApolloCompilerKotlinHooks() {
  override val version = "PrefixNamesHooks.0{$prefix}"

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
        ClassName(it.packageName, prefix + it.simpleName)
      } else {
        ClassName(it.packageName, it.simpleNames.mapIndexed { idx, s -> if (idx == it.simpleNames.lastIndex) s else prefix + s })
      }
    }
  }
}


/**
 * Make generated enum values uppercase.
 */
class CapitalizeEnumValuesHooks : DefaultApolloCompilerKotlinHooks() {
  override val version = "CapitalizeEnumValuesHooks.0"

  override fun postProcessFileSpec(fileSpec: FileSpec): FileSpec {
    return fileSpec
        .toBuilder()
        .apply {
          members.replaceAll { member ->
            if (member is TypeSpec && member.isEnum) {
              member.toBuilder()
                  .apply {
                    val capitalizedEnumConstants = enumConstants.mapKeys { (key, _) ->
                      key.toUpperCase()
                    }
                    enumConstants.clear()
                    enumConstants.putAll(capitalizedEnumConstants)

                    // knownValues is in the companion object
                    typeSpecs.replaceAll { typeSpec ->
                      typeSpec.toBuilder()
                          .apply {
                            funSpecs.replaceAll { funSpec ->
                              if (funSpec.name == "knownValues") {
                                funSpec.toBuilder()
                                    .clearBody()
                                    .addStatement("return arrayOf(%L)", capitalizedEnumConstants.keys.filterNot { it == "UNKNOWN__" }.joinToString())
                                    .build()
                              } else {
                                funSpec
                              }
                            }
                          }
                          .build()
                    }
                  }
                  .build()
            } else {
              member
            }
          }
        }
        .build()
  }
}
