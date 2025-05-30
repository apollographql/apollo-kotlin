package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.compiler.After
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.Before
import com.apollographql.apollo.compiler.CodeGenerator
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.LayoutFactory
import com.apollographql.apollo.compiler.LegacyOperationIdsGenerator
import com.apollographql.apollo.compiler.OperationIdsGenerator
import com.apollographql.apollo.compiler.ExecutableDocumentTransform
import com.apollographql.apollo.compiler.Order
import com.apollographql.apollo.compiler.SchemaTransform
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.codegen.java.JavaOutput
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.operationoutput.OperationId
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal class Registration<T>(val id: String, val transform: T, val orders: Array<out Order>)

private fun <T> List<Registration<T>>.toNodes(): Collection<Node<T>> {
  val map = mutableMapOf<String, Node<T>>()

  forEach {
    if (map.contains(it.id)) {
      error("Apollo: id '${it.id}' is used multiple times for '${it.transform}")
    }
    map.put(it.id, Node(it.id, it.transform))
  }

  forEach {
    val self = map.get(it.id)!!
    it.orders.forEach {
      when (it) {
        is After -> {
          val other = map.get(it.id)
          if (other != null) {
            self.dependencies.add(other)
          }
        }

        is Before -> {
          val other = map.get(it.id)
          if (other != null) {
            other.dependencies.add(self)
          }
        }
      }
    }
  }

  return map.values
}

private class Node<T>(val id: String, val transform: T) {
  val dependencies = mutableListOf<Node<T>>()
}

internal class DefaultApolloCompilerRegistry : ApolloCompilerRegistry {
  private val foreignSchemas = mutableListOf<ForeignSchema>()
  private val schemaTransforms = mutableListOf<Registration<SchemaTransform>>()
  private val executableDocumentTransforms = mutableListOf<Registration<ExecutableDocumentTransform>>()
  private val irTransforms = mutableListOf<Registration<Transform<IrOperations>>>()
  private val layoutFactories = mutableListOf<LayoutFactory>()
  private val operationIdsGenerators = mutableListOf<OperationIdsGenerator>()
  private val javaOutputTransforms = mutableListOf<Registration<Transform<JavaOutput>>>()
  private val kotlinOutputTransforms = mutableListOf<Registration<Transform<KotlinOutput>>>()
  private val extraCodeGenerators = mutableListOf<CodeGenerator>()

  @Suppress("DEPRECATION")
  fun registerPlugin(plugin: ApolloCompilerPlugin) {
    val operationIdsGenerator = LegacyOperationIdsGenerator(plugin)
    registerOperationIdsGenerator(operationIdsGenerator)
  }

  override fun registerForeignSchemas(schemas: List<ForeignSchema>) {
    foreignSchemas.addAll(schemas)
  }

  override fun registerSchemaTransform(
      id: String,
      vararg orders: Order,
      transform: SchemaTransform,
  ) {
    schemaTransforms.add(Registration(id, transform, orders))
  }

  override fun registerExecutableDocumentTransform(
      id: String,
      vararg orders: Order,
      transform: ExecutableDocumentTransform,
  ) {
    executableDocumentTransforms.add(Registration(id, transform, orders))
  }

  override fun registerIrTransform(
      id: String,
      vararg orders: Order,
      transform: Transform<IrOperations>,
  ) {
    irTransforms.add(Registration(id, transform, orders))
  }

  override fun registerLayout(factory: LayoutFactory) {
    layoutFactories.add(factory)
  }

  override fun registerOperationIdsGenerator(generator: OperationIdsGenerator) {
    operationIdsGenerators.add(generator)
  }

  override fun registerJavaOutputTransform(
      id: String,
      vararg orders: Order,
      transform: Transform<JavaOutput>,
  ) {
    javaOutputTransforms.add(Registration(id, transform, orders))
  }

  override fun registerKotlinOutputTransform(
      id: String,
      vararg orders: Order,
      transform: Transform<KotlinOutput>,
  ) {
    kotlinOutputTransforms.add(Registration(id, transform, orders))
  }

  override fun registerExtraCodeGenerator(codeGenerator: CodeGenerator) {
    extraCodeGenerators.add(codeGenerator)
  }

  fun foreignSchemas() = foreignSchemas

  fun schemaTransform(): SchemaTransform {
    val nodes = schemaTransforms.toNodes().sort()
    return SchemaTransform {
      nodes.fold(it) { acc, node ->
        node.transform.transform(acc)
      }
    }
  }

  fun operationsTransform(): ExecutableDocumentTransform {
    val nodes = executableDocumentTransforms.toNodes().sort()
    return ExecutableDocumentTransform { schema, document, fragmentDefinitions ->
      nodes.fold(document) { acc, node ->
        node.transform.transform(schema, acc, fragmentDefinitions)
      }
    }
  }

  fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
    if (layoutFactories.isEmpty()) {
      return null
    }
    if (layoutFactories.size > 1) {
      error("Apollo: multiple layouts registered. Check your compiler plugins.")
    }
    return layoutFactories.single().create(codegenSchema)
  }

  fun irOperationsTransform(): Transform<IrOperations> {
    val nodes = irTransforms.toNodes().sort()
    return Transform {
      nodes.fold(it) { acc, node ->
        node.transform.transform(acc)
      }
    }
  }

  fun javaOutputTransform(): Transform<JavaOutput> {
    val nodes = javaOutputTransforms.toNodes().sort()
    return Transform {
      nodes.fold(it) { acc, node ->
        node.transform.transform(acc)
      }
    }
  }

  fun kotlinOutputTransform(): Transform<KotlinOutput> {
    val nodes = kotlinOutputTransforms.toNodes().sort()
    return Transform {
      nodes.fold(it) { acc, node ->
        node.transform.transform(acc)
      }
    }
  }

  fun toOperationIdsGenerator(): OperationIdsGenerator {
    return OperationIdsGenerator { descriptors ->
      val candidates = operationIdsGenerators.mapNotNull {
        when (val operationIds = it.generate(descriptors)) {
          LegacyOperationIdsGenerator.NoList -> null
          else -> operationIds
        }
      }
      return@OperationIdsGenerator if (candidates.isEmpty()) {
        descriptors.map {
          OperationId(it.source.sha256(), it.name)
        }
      } else if (candidates.size == 1) {
        candidates.single()
      } else {
        error("Apollo: multiple operationIdGenerators are registered, please check your compiler plugins.")
      }
    }
  }

  fun extraCodeGenerator(): CodeGenerator {
    return CodeGenerator { document, outputDirectory ->
      extraCodeGenerators.forEach {
        it.generate(document, outputDirectory)
      }
    }
  }
}

internal fun String.sha256(): String {
  val bytes = toByteArray(charset = StandardCharsets.UTF_8)
  val md = MessageDigest.getInstance("SHA-256")
  val digest = md.digest(bytes)
  return digest.fold("") { str, it -> str + "%02x".format(it) }
}

private fun <T> Collection<Node<T>>.sort(): List<Node<T>> {
  val visited = mutableListOf<Node<T>>()
  val result = mutableListOf<Node<T>>()

  fun visit(node: Node<T>) {
    visited.add(node)
    node.dependencies.forEach {
      if (!visited.contains(it)) {
        visit(it)
      } else if (!result.contains(it)){
        throw IllegalArgumentException("Apollo: circular dependency detected on transform '${node.id}': ${visited.map { it.id } + node.id}")
      }
    }
    result.add(node)
  }

  forEach {
    if (!visited.contains(it)) {
      visit(it)
    }
  }
  return result
}
