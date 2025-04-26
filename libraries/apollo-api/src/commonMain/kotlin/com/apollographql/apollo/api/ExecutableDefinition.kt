package  com.apollographql.apollo.api

interface ExecutableDefinition<D: Executable.Data> {
  val ADAPTER: Adapter<D>

  val ROOT_FIELD: CompiledField
}
