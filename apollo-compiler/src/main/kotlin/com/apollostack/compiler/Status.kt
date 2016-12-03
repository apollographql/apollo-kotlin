package com.apollostack.compiler

sealed class Status() {
  class Success() : Status()
  class Failure() : Status()
  class Invalid() : Status()
}
