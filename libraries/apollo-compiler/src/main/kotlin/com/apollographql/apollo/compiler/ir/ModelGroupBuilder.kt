package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.CatchTo
import com.apollographql.apollo.ast.GQLSelection

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
