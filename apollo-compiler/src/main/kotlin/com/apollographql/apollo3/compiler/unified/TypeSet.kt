package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.possibleTypes
import org.jgrapht.alg.TransitiveReduction
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

internal typealias TypeSet = Set<String>
internal typealias PossibleTypes = Set<String>


data class Edge<V>(val source: V, val target: V)

sealed class Node(val typeSet: TypeSet)
class InterfaceNode(typeSet: TypeSet): Node(typeSet)
class ShapeNode(typeSet: TypeSet): Node(typeSet)

/**
 * Return the different possible shapes
 *
 * Note that this is not linked to the fragments typeSets
 *
 * If fragment typeSets are [[A],[A,B],[A,B,C],[A,B,D]]
 *
 * A type implementing [A,B,C,D] will match to [A,B,C,D]
 */
internal fun computeShapes(schema: Schema, typeConditions: Set<String>): Map<TypeSet, PossibleTypes> {
  val typeConditionToPossibleTypes = typeConditions.map {
    it to schema.typeDefinition(it).possibleTypes(schema)
  }

  return schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>()
      .map { it.name }
      .map { concreteType ->
        val matchedSupers = typeConditionToPossibleTypes.filter { it.second.contains(concreteType) }
            .map { it.first }
            .toSet()

        matchedSupers to concreteType
      }
      .groupBy(
          keySelector = { it.first },
          valueTransform = { it.second }
      )
      .mapValues { it.value.toSet() }
}

internal fun TypeSet.implements(other: TypeSet) = intersect(other) == other


fun <V> transitiveReduce(
    edges: List<Edge<out V>>
): List<Edge<V>> {
  val graph = DefaultDirectedGraph<V, DefaultEdge>(DefaultEdge::class.java)

  edges.flatMap { listOf(it.source, it.target) }.distinct()
      .forEach {
        graph.addVertex(it)
      }
  edges.forEach {
    graph.addEdge(it.source, it.target)
  }

  TransitiveReduction.INSTANCE.reduce(graph)

  return graph.edgeSet().map {
    Edge(graph.getEdgeSource(it), graph.getEdgeTarget(it))
  }
}