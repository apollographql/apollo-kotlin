package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema

interface ModelGroupBuilder {
  fun buildOperationData(
      selections: List<GQLSelection>,
      rawTypeName: String,
      operationName: String
  ): IrModelGroup

  fun buildFragmentInterface(
      fragmentName: String
  ): IrModelGroup?

  fun buildFragmentData(
      fragmentName: String
  ): IrModelGroup

}