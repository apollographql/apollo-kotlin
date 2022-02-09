package test

/**
 * Because JS tests cannot run longer than 2s, we do only one iteration
 *
 * See https://github.com/apollographql/apollo-kotlin/pull/3853
 */
actual val watcherIterations: Int
  get() = 1