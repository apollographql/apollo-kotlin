package com.apollographql.apollo.compiler

object SchemaGenerator {
  fun generateSDLSchemaWithInputTypes(inputTypeCount: Int): String {
    val inputTypes42 = """
input User42 {
  id: ID!
  name: String!
}

input Body42 {
  subject: String!
  content: String!
}

scalar CustomScalar42 

input MessageInput42 {
  from: User42!
  to: User42!
  body: Body42!
  customScalar: CustomScalar42!
  id: ID!
  encoding: Encoding!
}
"""

    val roots = """
  type Query {
      random: String!
  }

  enum Encoding {
      UTF8,
      CP1952
  }

  type Mutation {
""".trimIndent()

    // There are 3 input types for each loop
    val repeat = inputTypeCount / 3

    val stringBuilder = StringBuilder()
    repeat(repeat) {
      stringBuilder.append(inputTypes42.replace("42", it.toString()))
    }
    stringBuilder.append(roots)
    repeat(repeat) {
      stringBuilder.append("sendMessage$it(input: MessageInput$it): String!\n")
    }
    stringBuilder.append("}\n")

    return stringBuilder.toString()
  }

  fun generateMutation(): String {
    return """
      mutation sendMessage(      ${'$'}input      : MessageInput0) {
          sendMessage0(input:       ${'$'}input      )
      }
    """.trimIndent()
  }
}