package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.ast.CatchTo
import com.apollographql.apollo3.ast.GQLSelection

internal interface ModelGroupBuilder {
  fun buildOperationData(
      selections: List<GQLSelection>,
      rawTypeName: String,
      operationName: String,
      defaultCatchTo: CatchTo?,
  ): Pair<IrProperty, IrModelGroup>

  fun buildFragmentInterface(
      fragmentName: String,
  ): IrModelGroup?

  fun buildFragmentData(
      fragmentName: String,
      defaultCatchTo: CatchTo?,
  ): Pair<IrProperty, IrModelGroup>

}
