import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks.FileInfo
import com.apollographql.apollo3.compiler.hooks.DefaultApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.DefaultApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.hooks.internal.AddInternalCompilerHooks
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import javax.lang.model.element.Modifier

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
    @OptIn(ApolloInternal::class)
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

  service("prefixnames.kotlin") {
    packageName.set("hooks.prefixnames.kotlin")
    compilerKotlinHooks.set(listOf(PrefixNamesKotlinHooks("GQL")))
  }

  service("prefixnames.java") {
    packageName.set("hooks.prefixnames.java")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    generateKotlinModels.set(false)
    compilerJavaHooks.set(listOf(PrefixNamesJavaHooks("GQL")))
  }

  service("capitalizeenumvalues") {
    packageName.set("hooks.capitalizeenumvalues")
    compilerKotlinHooks.set(listOf(CapitalizeEnumValuesHooks()))
  }

  service("gettersandsetters.java") {
    packageName.set("hooks.gettersandsetters")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    generateKotlinModels.set(false)
    compilerJavaHooks.set(listOf(AddGettersAndSettersHooks()))
  }

}

/**
 * Adds a default `null` value to data class fields that are nullable.
 */
class DefaultNullValuesHooks : DefaultApolloCompilerKotlinHooks() {
  override val version = "DefaultNullValuesHooks.0"

  override fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo> {
    return files.map {
      it.copy(fileSpec = it.fileSpec
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
      )
    }
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

  override fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo> {
    return files.map {
      it.copy(fileSpec = it.fileSpec
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
      )
    }
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
 * Prefix generated class names with the specified [prefix] (Kotlin).
 */
class PrefixNamesKotlinHooks(private val prefix: String) : DefaultApolloCompilerKotlinHooks() {
  override val version = "PrefixNamesKotlinHooks.0{$prefix}"

  override fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo> {
    return files.map {
      it.copy(fileSpec = it.fileSpec
          .toBuilder(name = prefix + it.fileSpec.name)
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
      )
    }
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
 * Prefix generated class names with the specified [prefix] (Java).
 */
class PrefixNamesJavaHooks(private val prefix: String) : DefaultApolloCompilerJavaHooks() {
  override val version = "PrefixNamesJavaHooks.6{$prefix}"

  override fun postProcessFiles(files: Collection<ApolloCompilerJavaHooks.FileInfo>): Collection<ApolloCompilerJavaHooks.FileInfo> {
    return files.map {
      it.copy(javaFile = JavaFile.builder(it.javaFile.packageName, it.javaFile.typeSpec!!.toBuilder(prefix + it.javaFile.typeSpec.name).build())
          .build()
      )
    }
  }

  override fun overrideResolvedType(key: ResolverKey, resolved: com.squareup.javapoet.ClassName?): com.squareup.javapoet.ClassName? {
    return resolved?.let {
      // Don't prefix nested types.
      // e.g. MyQuery -> PrefixMyQuery
      //      MyQuery.Data -> PrefixMyQuery.Data
      if (it.simpleNames().size == 1) {
        com.squareup.javapoet.ClassName.get(it.packageName(), prefix + it.simpleName())
      } else {
        val simpleNames = it.simpleNames().mapIndexed { idx, s -> if (idx == it.simpleNames().lastIndex) s else prefix + s }
        com.squareup.javapoet.ClassName.get(it.packageName(), simpleNames[0], *simpleNames.drop(1).toTypedArray())
      }
    }
  }

  private fun com.squareup.javapoet.TypeSpec.toBuilder(name: String): com.squareup.javapoet.TypeSpec.Builder {
    return when (kind) {
      com.squareup.javapoet.TypeSpec.Kind.CLASS -> {
        com.squareup.javapoet.TypeSpec.classBuilder(name)
            .addJavadoc("\$L", javadoc)
            .addAnnotations(annotations)
            .addModifiers(*modifiers.toTypedArray())
            .addTypeVariables(typeVariables)
            .superclass(superclass)
            .addSuperinterfaces(superinterfaces)
            .addFields(fieldSpecs)
            // XXX Skip the equals methods which reference the generated class name
            .addMethods(methodSpecs.filterNot { it.name == "equals" })
            .addTypes(typeSpecs)
            .addInitializerBlock(initializerBlock)
            .addStaticBlock(staticBlock)
      }

      com.squareup.javapoet.TypeSpec.Kind.ENUM -> {
        com.squareup.javapoet.TypeSpec.enumBuilder(name)
            .addJavadoc("\$L", javadoc)
            .addAnnotations(annotations)
            .addModifiers(*modifiers.toTypedArray())
            .addTypeVariables(typeVariables)
            .addSuperinterfaces(superinterfaces)
            .addFields(fieldSpecs)
            .addMethods(methodSpecs.filterNot { it.name == "equals" })
            .addTypes(typeSpecs)
            .also {
              enumConstants.forEach { (name, typeSpec) ->
                it.addEnumConstant(name, typeSpec)
              }
            }
      }

      com.squareup.javapoet.TypeSpec.Kind.INTERFACE -> {
        com.squareup.javapoet.TypeSpec.interfaceBuilder(name)
            .addJavadoc("\$L", javadoc)
            .addAnnotations(annotations)
            .addModifiers(*modifiers.toTypedArray())
            .addTypeVariables(typeVariables)
            .superclass(superclass)
            .addSuperinterfaces(superinterfaces)
            .addFields(fieldSpecs)
            .addMethods(methodSpecs.filterNot { it.name == "equals" })
            .addTypes(typeSpecs)
            .addInitializerBlock(initializerBlock)
            .addStaticBlock(staticBlock)
      }

      else -> {
        throw IllegalArgumentException("Unknown type kind: $kind")
      }
    }
  }
}

/**
 * Make generated enum values uppercase.
 */
class CapitalizeEnumValuesHooks : DefaultApolloCompilerKotlinHooks() {
  override val version = "CapitalizeEnumValuesHooks.0"

  override fun postProcessFiles(files: Collection<FileInfo>): Collection<FileInfo> {
    return files.map {
      it.copy(fileSpec = it.fileSpec
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
      )
    }
  }
}

/**
 * Add setters and getters to the models.
 */
class AddGettersAndSettersHooks : DefaultApolloCompilerJavaHooks() {
  override val version = "AddGettersAndSettersHooks.0"

  override fun postProcessFiles(files: Collection<ApolloCompilerJavaHooks.FileInfo>): Collection<ApolloCompilerJavaHooks.FileInfo> {
    return files.map {
      val javaFile = it.javaFile
      it.copy(javaFile = JavaFile.builder(
          javaFile.packageName,
          // Only process operations (generated at the top level)
          if (javaFile.packageName.endsWith(".adapter") ||
              javaFile.packageName.endsWith(".selections") ||
              javaFile.packageName.endsWith(".type") ||
              javaFile.packageName.endsWith(".fragment")
          ) {
            javaFile.typeSpec
          } else {
            javaFile.typeSpec!!.toBuilder()
                .apply {
                  typeSpecs.replaceAll { typeSpec ->
                    if (typeSpec.name == "Builder") {
                      typeSpec
                    } else {
                      typeSpec
                          .toBuilder()
                          .addMethods(typeSpec.fieldSpecs
                              // Ignore $hashCode, $toString etc.
                              .filterNot { fieldSpec -> fieldSpec.hasModifier(Modifier.TRANSIENT) }
                              .flatMap { fieldSpec ->
                                listOf(
                                    MethodSpec.methodBuilder("get${fieldSpec.name.removePrefix("__").capitalize()}")
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(fieldSpec.type)
                                        .addStatement("return this.\$L", fieldSpec.name)
                                        .build(),
                                    MethodSpec.methodBuilder("set${fieldSpec.name.removePrefix("__").capitalize()}")
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(fieldSpec.type, fieldSpec.name)
                                        .addStatement("this.\$L = \$L", fieldSpec.name, fieldSpec.name)
                                        .build()
                                )
                              })
                          .build()
                    }
                  }
                }
                .build()
          })
          .build()
      )
    }
  }
}
