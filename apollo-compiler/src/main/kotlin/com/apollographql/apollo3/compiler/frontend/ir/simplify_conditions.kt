package com.apollographql.apollo3.compiler.frontend.ir

internal fun FrontendIr.Operation.simplifyConditions(): FrontendIr.Operation {
  return copy( selections = selections.simplifyConditions())
}

private fun List<FrontendIr.Selection>.simplifyConditions(): List<FrontendIr.Selection> {
  return mapNotNull {
    when (it) {
      is FrontendIr.Selection.Field -> it.simplifyConditions()
      is FrontendIr.Selection.InlineFragment -> it.simplifyConditions()
      is FrontendIr.Selection.FragmentSpread -> it.simplifyConditions()
    }
  }
}

private fun FrontendIr.Selection.FragmentSpread.simplifyConditions(): FrontendIr.Selection.FragmentSpread? {
  val newCondition = condition.simplify()
  return if (newCondition == FrontendIr.Condition.False) {
    null
  } else {
    copy(
        condition = condition.simplify()
    )
  }
}

private fun FrontendIr.Selection.InlineFragment.simplifyConditions(): FrontendIr.Selection.InlineFragment? {
  val newCondition = condition.simplify()
  return if (newCondition == FrontendIr.Condition.False) {
    null
  } else {
    copy(
        condition = newCondition,
        fragmentDefinition = fragmentDefinition.simplifyConditions()
    )
  }
}

internal fun FrontendIr.InlineFragmentDefinition.simplifyConditions(): FrontendIr.InlineFragmentDefinition {
  return copy(selections = selections.simplifyConditions())
}

internal fun FrontendIr.NamedFragmentDefinition.simplifyConditions(): FrontendIr.NamedFragmentDefinition {
  return copy(selections = selections.simplifyConditions())
}

private fun FrontendIr.Selection.Field.simplifyConditions(): FrontendIr.Selection.Field? {
  val newCondition = condition.simplify()
  return if (newCondition == FrontendIr.Condition.False) {
    null
  } else {
    copy(
        condition = newCondition,
        selections = selections.simplifyConditions()
    )
  }
}
