package com.apollographql.apollo3.compiler.unified

internal data class Model(
    val responseName: String,
    val typeSet: TypeSet,
    val irField: IrField,
    val irFieldSet: IrFieldSet,
    val children: List<Model>,
)

internal class ModelBuilder(val rootField: IrField) {
  private val edges = mutableListOf<Edge<Model>>()

  fun build(): Model {
    // build the model tree without any inheritance information
    val rootModels = rootField.toModels()

    // then walk the tree and build the inheritance edges
    walk(rootModels, emptyList())

    return Model(

    )
  }

  /**
   * @param neighbours all the models with the same responseName path, ordered by common ancestor distance,
   * ie the neighbours with the closest common ancestor will come first
   */
  @Suppress("NAME_SHADOWING")
  private fun walk(models: List<Model>, neighbours: List<Model>): Model {
    val models = models.sortedByDescending { it.typeSet.size }
    for (i in 0.until(models.size)) {
      val model = models[i]
      /**
       * a model cannot implement itself so start at i + 1
       * it can implement an interface with the same typeSet though if an implementation implements an interface
       */
      val neighbours = models.subList(i + 1, models.size) + neighbours
      neighbours.forEach {
        if (model.typeSet.implements(it.typeSet)) {
          edges.add(Edge(model, it))
        }
      }

      model.children.forEach { childModel ->
        walk(childModel, )
      }
    }
  }

  /**
   * @param neighbours all the models with the same responseName path, ordered by common ancestor distance,
   * that this model must inherit from
   */
  private fun walk(model: Model, neighbours: List<Model>): Model {
    neighbours.forEach {
      if (model.typeSet.implements(it.typeSet)) {
        edges.add(Edge(model, it))
      }
    }

    model.children.forEach {

    }

      model.children.forEach { childModel ->
        walk(childModel, )
      }
    }
  }
  private fun IrField.toModels(): List<Model> {
    return fieldSets.map { fieldSet ->
      Model(
          responseName = responseName,
          typeSet = fieldSet.typeSet,
          irField = this,
          irFieldSet = fieldSet,
          children = fieldSet.fields.flatMap {
            it.toModels()
          }
      )
    }
  }
}