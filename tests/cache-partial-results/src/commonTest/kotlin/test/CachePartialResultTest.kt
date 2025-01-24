package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CompiledFragment
import com.apollographql.apollo.api.CompiledListType
import com.apollographql.apollo.api.CompiledNamedType
import com.apollographql.apollo.api.CompiledNotNullType
import com.apollographql.apollo.api.CompiledSelection
import com.apollographql.apollo.api.CompiledType
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.CustomScalarType
import com.apollographql.apollo.api.EnumType
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Error.Location
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.api.InterfaceType
import com.apollographql.apollo.api.ObjectType
import com.apollographql.apollo.api.UnionType
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.cacheHeaders
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.cache.normalized.storePartialResponses
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import okio.use
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CachePartialResultTest {
  private lateinit var mockServer: MockServer

  private suspend fun setUp() {
    mockServer = MockServer()
  }

  private fun tearDown() {
    mockServer.close()
  }

  @Test
  fun simple() = runTest(before = { setUp() }, after = { tearDown() }) {
//    val definitions: List<GQLDefinition> = MeWithoutNickNameWithEmailQuery().getDefinitions(__Schema.all.associateBy { it.name })
//    println(GQLDocument(definitions, null).toUtf8())
//
//    val dataFromCacheMap: Map<String, Any?> = mapOf("me" to mapOf("id" to "1", "firstName" to "John", "lastName" to "Smith"))
//
//
//    val query = MeWithoutNickNameWithoutEmailQuery()
//    val graphQLRequest = GraphQLRequest.Builder()
//        .document(query.document())
//        .variables(emptyMap())
//        .build()
//    val graphQLResponse: GraphQLResponse = ExecutableSchema.Builder()
//        .schema(GQLDocument(definitions, null))
//        .resolver { resolveInfo -> dataFromCacheMap.valueAtPath(resolveInfo.path) }
//        .build()
//        .execute(graphQLRequest, ExecutionContext.Empty)
//
//    val customScalarAdapters = CustomScalarAdapters.Empty
//    val data: MeWithoutNickNameWithoutEmailQuery.Data =
//      (graphQLResponse.data as Map<String, Any?>).toData(query.adapter(), customScalarAdapters, query.variables(customScalarAdapters, true))
//    println(data)


    mockServer.enqueueString(
        // language=JSON
        """
        {
          "data": {
            "me": {
              "__typename": "User",
              "id": "1",
              "firstName": "John",
              "lastName": "Smith",
              "email": "jsmith@example.com"
            }
          }
        }
        """
    )
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .normalizedCache(MemoryCacheFactory())
        .cacheHeaders(CacheHeaders.builder()
            .addHeader("schema", SCHEMA)
            .build()
        )
        .build()
        .use { apolloClient ->
          val networkResult = apolloClient.query(MeWithoutNickNameWithEmailQuery())
              .fetchPolicy(FetchPolicy.NetworkOnly)
              .execute()
          assertEquals(
              MeWithoutNickNameWithEmailQuery.Data(
                  MeWithoutNickNameWithEmailQuery.Me(
                      __typename = "User",
                      firstName = "John",
                      lastName = "Smith",
                      email = "jsmith@example.com",
                      id = "1",
                      onUser = MeWithoutNickNameWithEmailQuery.OnUser(
                          id = "1"
                      )
                  )
              ),
              networkResult.data
          )

          val cacheResult = apolloClient.query(MeWithoutNickNameWithoutEmailQuery())
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          assertEquals(
              MeWithoutNickNameWithoutEmailQuery.Data(
                  MeWithoutNickNameWithoutEmailQuery.Me(
                      id = "1",
                      firstName = "John",
                      lastName = "Smith",
                      __typename = "User"
                  )
              ),
              cacheResult.data
          )

          val cacheMissResult = apolloClient.query(MeWithNickNameQuery())
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          assertEquals(
              MeWithNickNameQuery.Data(
                  MeWithNickNameQuery.Me(
                      id = "1",
                      firstName = "John",
                      lastName = "Smith",
                      nickName = null,
                      __typename = "User"
                  )
              ),
              cacheMissResult.data
          )
          assertErrorsEquals(
              listOf(
                  Error.Builder("Object 'User:1' has no field named 'nickName' in the cache").path(listOf("me", "nickName")).build()
              ),
              cacheMissResult.errors
          )
        }
  }

  @Test
  fun lists() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(
        // language=JSON
        """
        {
          "data": {
            "users": [
              {
                "__typename": "User",
                "id": "1",
                "firstName": "John",
                "lastName": "Smith",
                "email": "jsmith@example.com"
              },
              {
                "__typename": "User",
                "id": "2",
                "firstName": "Jane",
                "lastName": "Doe",
                "email": "jdoe@example.com"
              },
              null
            ]
          },
          "errors": [
            {
              "message": "User `3` not found",
              "path": ["users", 2]
            }
          ]
        }
        """
    )
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .cacheHeaders(CacheHeaders.builder()
            .addHeader("schema", SCHEMA)
            .build()
        )
        .store(
            ApolloStore(
                normalizedCacheFactory = MemoryCacheFactory(),
                cacheKeyGenerator = IdCacheKeyGenerator(),
                cacheResolver = IdCacheKeyResolver()
            )
        )
        .build()
        .use { apolloClient ->
          val networkResult = apolloClient.query(UsersQuery(listOf("1", "2", "3")))
              .fetchPolicy(FetchPolicy.NetworkOnly)
              .storePartialResponses(true)
              .execute()
          assertEquals(
              UsersQuery.Data(
                  users = listOf(
                      UsersQuery.User(
                          __typename = "User",
                          id = "1",
                          firstName = "John",
                          lastName = "Smith",
                          email = "jsmith@example.com",
                      ),
                      UsersQuery.User(
                          __typename = "User",
                          id = "2",
                          firstName = "Jane",
                          lastName = "Doe",
                          email = "jdoe@example.com",
                      ),
                      null,
                  )
              ),
              networkResult.data
          )

          val cacheResult = apolloClient.query(UsersQuery(listOf("1", "2", "3")))
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          assertEquals(
              networkResult.data,
              cacheResult.data,
          )
          assertErrorsEquals(
              listOf(
                  Error.Builder("Object 'User:3' not found in the cache").path(listOf("users", 2)).build()
              ),
              cacheResult.errors
          )
        }
  }

  @Test
  fun composite() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(
        // language=JSON
        """
        {
          "data": {
            "me": {
              "__typename": "User",
              "id": "1",
              "firstName": "John",
              "lastName": "Smith",
              "bestFriend": {
                "__typename": "User",
                "id": "2",
                "firstName": "Jane",
                "lastName": "Doe"
              },
              "projects": [
                {
                  "__typename": "Project",
                  "lead": {
                    "__typename": "User",
                    "id": "3",
                    "firstName": "Amanda",
                    "lastName": "Brown"
                  },
                  "users": [
                    {
                      "__typename": "User",
                      "id": "4",
                      "firstName": "Alice",
                      "lastName": "White"
                    }
                  ]
                }
              ]
            }
          }
        }
        """
    )
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .normalizedCache(MemoryCacheFactory())
        .cacheHeaders(CacheHeaders.builder()
            .addHeader("schema", SCHEMA)
            .build()
        )
        .build()
        .use { apolloClient ->
          // Prime the cache
          val networkResult = apolloClient.query(MeWithBestFriendQuery())
              .fetchPolicy(FetchPolicy.NetworkOnly)
              .execute()
          assertEquals(
              MeWithBestFriendQuery.Data(
                  MeWithBestFriendQuery.Me(
                      __typename = "User",
                      id = "1",
                      firstName = "John",
                      lastName = "Smith",
                      bestFriend = MeWithBestFriendQuery.BestFriend(
                          __typename = "User",
                          id = "2",
                          firstName = "Jane",
                          lastName = "Doe"
                      ),
                      projects = listOf(
                          MeWithBestFriendQuery.Project(
                              lead = MeWithBestFriendQuery.Lead(
                                  __typename = "User",
                                  id = "3",
                                  firstName = "Amanda",
                                  lastName = "Brown"
                              ),
                              users = listOf(
                                  MeWithBestFriendQuery.User(
                                      __typename = "User",
                                      id = "4",
                                      firstName = "Alice",
                                      lastName = "White"
                                  )
                              )
                          )
                      )
                  )
              ),
              networkResult.data
          )

          // Remove project lead from the cache
          apolloClient.apolloStore.remove(CacheKey("User", "3"))
          val cacheResult = apolloClient.query(MeWithBestFriendQuery())
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          assertEquals(
              MeWithBestFriendQuery.Data(
                  MeWithBestFriendQuery.Me(
                      __typename = "User",
                      id = "1",
                      firstName = "John",
                      lastName = "Smith",
                      bestFriend = MeWithBestFriendQuery.BestFriend(
                          __typename = "User",
                          id = "2",
                          firstName = "Jane",
                          lastName = "Doe"
                      ),
                      projects = listOf(
                          MeWithBestFriendQuery.Project(
                              lead = null,
                              users = listOf(
                                  MeWithBestFriendQuery.User(
                                      __typename = "User",
                                      id = "4",
                                      firstName = "Alice",
                                      lastName = "White"
                                  )
                              )
                          )
                      )
                  )
              ),
              cacheResult.data
          )
          assertErrorsEquals(
              listOf(
                  Error.Builder("Object 'User:3' not found in the cache").path(listOf("me", "projects", 0, "lead")).build()
              ),
              cacheResult.errors
          )

          // Remove best friend from the cache
          apolloClient.apolloStore.remove(CacheKey("User", "2"))
          val cacheResult2 = apolloClient.query(MeWithBestFriendQuery())
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          assertEquals(
              MeWithBestFriendQuery.Data(
                  MeWithBestFriendQuery.Me(
                      __typename = "User",
                      id = "1",
                      firstName = "John",
                      lastName = "Smith",
                      bestFriend = null,
                      projects = listOf(
                          MeWithBestFriendQuery.Project(
                              lead = null,
                              users = listOf(
                                  MeWithBestFriendQuery.User(
                                      __typename = "User",
                                      id = "4",
                                      firstName = "Alice",
                                      lastName = "White"
                                  )
                              )
                          )
                      )
                  )
              ),
              cacheResult2.data
          )
          assertErrorsEquals(
              listOf(
                  Error.Builder("Object 'User:2' not found in the cache").path(listOf("me", "bestFriend")).build(),
                  Error.Builder("Object 'User:3' not found in the cache").path(listOf("me", "projects", 0, "lead")).build(),
              ),
              cacheResult2.errors
          )

          // Remove project user from the cache
          apolloClient.apolloStore.remove(CacheKey("User", "4"))
          val cacheResult3 = apolloClient.query(MeWithBestFriendQuery())
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          // Due to null bubbling the whole data is null
          assertNull(cacheResult3.data)
          assertErrorsEquals(
              listOf(
                  Error.Builder("Object 'User:4' not found in the cache").path(listOf("me", "projects", 0, "users", 0)).build()
              ),
              cacheResult3.errors
          )
        }
  }

  @Test
  fun argumentsAndAliases() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(
        // language=JSON
        """
        {
          "data": {
            "project": {
              "__typename": "Project",
              "id": "42",
              "name": "El Dorado",
              "description": "The lost city of gold"
            },
            "project2": {
              "__typename": "Project",
              "id": "44",
              "name": "Atlantis",
              "description": "The lost city of water"
            }
          }
        }
        """
    )
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .store(
            ApolloStore(
                normalizedCacheFactory = MemoryCacheFactory(),
                cacheKeyGenerator = IdCacheKeyGenerator(),
                cacheResolver = IdCacheKeyResolver()
            )
        )
        .cacheHeaders(CacheHeaders.builder()
            .addHeader("schema", SCHEMA)
            .build()
        )
        .build()
        .use { apolloClient ->
          val networkResult = apolloClient.query(DefaultProjectQuery())
              .fetchPolicy(FetchPolicy.NetworkOnly)
              .execute()
          assertEquals(
              DefaultProjectQuery.Data(
                  project = DefaultProjectQuery.Project(
                      id = "42",
                      name = "El Dorado",
                      description = "The lost city of gold"
                  ),
                  project2 = DefaultProjectQuery.Project2(
                      id = "44",
                      name = "Atlantis",
                      description = "The lost city of water"
                  )
              ),
              networkResult.data
          )

          println(NormalizedCache.prettifyDump(apolloClient.apolloStore.dump()))

          val cacheResult = apolloClient.query(DefaultProjectQuery())
              .fetchPolicy(FetchPolicy.CacheOnly)
              .execute()
          assertEquals(
              networkResult.data,
              cacheResult.data
          )
        }
  }
}

private fun <D : Executable.Data> Map<String, Any?>.toData(
    adapter: Adapter<D>,
    customScalarAdapters: CustomScalarAdapters,
    variables: Executable.Variables,
): D {
  val reader = MapJsonReader(this)
  return adapter.fromJson(reader, customScalarAdapters.newBuilder().falseVariables(variables.valueMap.filter { it.value == false }.keys)
      .build()
  )
}


private fun Map<String, Any?>.valueAtPath(path: List<Any>): Any? {
  var value: Any? = this
  for (key in path) {
    value = if (value is List<*>) {
      value[key as Int]
    } else {
      @Suppress("UNCHECKED_CAST")
      value as Map<String, Any?>
      value[key]
    }
  }
  return value
}

private fun <D : Executable.Data> Executable<D>.getDefinitions(knownTypes: Map<String, CompiledNamedType>): List<GQLDefinition> {
  val definitions = mutableMapOf<String, GQLDefinition>()

  fun addDefinitions(parentFieldRawType: CompiledNamedType, compiledSelections: List<CompiledSelection>) {
    val typeName = parentFieldRawType.name
    var parentFieldTypeDefinition = definitions.getOrPut(typeName) {
      when (parentFieldRawType) {
        is CustomScalarType -> GQLScalarTypeDefinition(name = typeName, description = null, directives = emptyList())
        is EnumType -> GQLEnumTypeDefinition(name = typeName, description = null, directives = emptyList(), enumValues = emptyList())
        is InterfaceType -> GQLInterfaceTypeDefinition(name = typeName, description = null, implementsInterfaces = emptyList(), directives = emptyList(), fields = emptyList())
        is ObjectType -> GQLObjectTypeDefinition(name = typeName, description = null, implementsInterfaces = emptyList(), directives = emptyList(), fields = emptyList())
        is UnionType -> GQLUnionTypeDefinition(name = typeName, description = null, directives = emptyList(), memberTypes = emptyList())
        else -> error("Unsupported type $parentFieldRawType")
      }
    }

    for (selection in compiledSelections) {
      when (selection) {
        is CompiledField -> {
          val field = selection.toGQLFieldDefinition()
          when (parentFieldTypeDefinition) {
            is GQLObjectTypeDefinition -> {
              parentFieldTypeDefinition = parentFieldTypeDefinition.withField(field)
              definitions[typeName] = parentFieldTypeDefinition
            }

            is GQLInterfaceTypeDefinition -> {
              parentFieldTypeDefinition = parentFieldTypeDefinition.withField(field)
              definitions[typeName] = parentFieldTypeDefinition
            }

            else -> error("Cannot add field to $parentFieldTypeDefinition")
          }
          addDefinitions(selection.type.rawType(), selection.selections)
        }

        is CompiledFragment -> {
          val typeCondition = selection.typeCondition
          addDefinitions(knownTypes[typeCondition]!!, selection.selections)
        }
      }
    }
  }
  addDefinitions(rootField().type as CompiledNamedType, rootField().selections)
  return definitions.values.toList()
}

private fun GQLObjectTypeDefinition.withField(field: GQLFieldDefinition): GQLObjectTypeDefinition {
  if (fields.any { it.name == field.name }) {
    return this
  }
  return GQLObjectTypeDefinition(
      name = name,
      description = description,
      implementsInterfaces = implementsInterfaces,
      directives = directives,
      fields = fields + field
  )
}

private fun GQLInterfaceTypeDefinition.withField(field: GQLFieldDefinition): GQLInterfaceTypeDefinition {
  if (fields.any { it.name == field.name }) {
    return this
  }
  return GQLInterfaceTypeDefinition(
      name = name,
      description = description,
      implementsInterfaces = implementsInterfaces,
      directives = directives,
      fields = fields + field
  )
}

private fun CompiledField.toGQLFieldDefinition(): GQLFieldDefinition {
  return GQLFieldDefinition(
      name = name,
      description = null,
      arguments = emptyList(),
      type = type.toGQLType(),
      directives = emptyList()
  )
}

private fun CompiledType.toGQLType(): GQLType {
  return when (this) {
    is CompiledNotNullType -> GQLNonNullType(sourceLocation = null, type = ofType.toGQLType())
    is CompiledListType -> GQLListType(sourceLocation = null, type = ofType.toGQLType())
    is CompiledNamedType -> GQLNamedType(name = name, sourceLocation = null)
  }
}

private class IdCacheKeyResolver(
    private val idFields: List<String> = listOf("id"),
    private val idListFields: List<String> = listOf("ids"),
) : CacheKeyResolver() {
  override fun cacheKeyForField(field: CompiledField, variables: Executable.Variables): CacheKey? {
    val typeName = field.type.rawType().name
    val id = idFields.firstNotNullOfOrNull { field.argumentValue(it, variables).getOrNull()?.toString() } ?: return null
    return CacheKey(typeName, id)
  }

  override fun listOfCacheKeysForField(field: CompiledField, variables: Executable.Variables): List<CacheKey?>? {
    val typeName = field.type.rawType().name
    val ids = idListFields.firstNotNullOfOrNull { field.argumentValue(it, variables).getOrNull() as? List<*> }
        ?: return null
    return ids.map { id -> id?.toString()?.let { CacheKey(typeName, it) } }
  }
}

private class IdCacheKeyGenerator(private vararg val idFields: String = arrayOf("id")) : CacheKeyGenerator {
  override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext): CacheKey? {
    val values = idFields.map {
      (obj[it] ?: return null).toString()
    }
    val typeName = context.field.type.rawType().name
    return CacheKey(typeName, values)
  }
}

private const val SCHEMA = """
type Query {
  me: User!
  users(ids: [ID!]!): [User]!
  project(id: ID! = "42"): Project
}

type User {
  id: ID!
  firstName: String!
  lastName: String!
  nickName: String
  email: String!
  bestFriend: User
  projects: [Project!]!
  mainProject: Project!
}

type Project {
  id: ID!
  name: String!
  description: String
  lead: User
  users: [User!]!
}
"""

/**
 * Helps using assertEquals.
 */
private data class ComparableError(
    val message: String,
    val locations: List<Location>?,
    val path: List<Any>?,
    val extensions: Map<String, Any?>?,
    val nonStandardFields: Map<String, Any?>?,
)

private fun assertErrorsEquals(expected: Iterable<Error>?, actual: Iterable<Error>?) =
  assertContentEquals(expected?.map {
    ComparableError(
        message = it.message,
        locations = it.locations,
        path = it.path,
        extensions = it.extensions,
        nonStandardFields = it.nonStandardFields
    )
  }, actual?.map {
    ComparableError(
        message = it.message,
        locations = it.locations,
        path = it.path,
        extensions = it.extensions,
        nonStandardFields = it.nonStandardFields
    )
  })
