package com.apollographql.apollo3.internal

enum class CallState {
  IDLE, ACTIVE, TERMINATED, CANCELED;

  internal class IllegalStateMessage private constructor(private val callState: CallState) {
    fun expected(vararg acceptableStates: CallState): String {
      val stringBuilder = StringBuilder("Found: " + callState.name + ", but expected [")
      var deliminator = ""
      for (state in acceptableStates) {
        stringBuilder.append(deliminator).append(state.name)
        deliminator = ", "
      }
      return stringBuilder.append("]").toString()
    }

    companion object {
      @JvmStatic
      fun forCurrentState(callState: CallState): IllegalStateMessage {
        return IllegalStateMessage(callState)
      }
    }
  }
}