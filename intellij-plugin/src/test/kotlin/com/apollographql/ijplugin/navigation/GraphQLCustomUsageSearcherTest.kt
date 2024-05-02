package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloTestCase
import com.apollographql.ijplugin.util.cast
import com.intellij.usages.UsageInfo2UsageAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GraphQLCustomUsageSearcherTest : ApolloTestCase() {
  private fun testFindUsages(
      fromFile: String,
      moveCaret: () -> Unit,
      expected: List<Pair<String, String>>,
  ) {
    // Open file
    myFixture.configureFromTempProjectFile(fromFile)

    // Move caret on element to call Find Usages on
    moveCaret()

    // Call Find Usages
    val usages = myFixture.testFindUsagesUsingAction()
        // Ignore usages found in generated files
        .filterNot { it.cast<UsageInfo2UsageAdapter>()!!.file.path.contains("build") }

    // Evaluate this expression to see expected results
    // usages.map {(it as UsageInfo2UsageAdapter).file.name.quoted() + " to " + it.plainText.trim().quoted()}.joinToString(",\n")

    // Compare with expected results
    assertEquals(expected.size, usages.size)
    expected.forEach { (fileName, text) ->
      assertTrue(
          usages.any { usage ->
            usage as UsageInfo2UsageAdapter
            usage.file.name == fileName && usage.plainText.contains(text)
          }
      )
    }
  }

  @Test
  fun operation() = testFindUsages(
      fromFile = "src/main/graphql/AnimalsQuery.graphql",
      moveCaret = { moveCaret("animals") },
      expected = listOf(
          "Main.kt" to "import com.example.generated.AnimalsQuery",
          "Main.kt" to "val animalsQuery = AnimalsQuery()",
          "Markers.kt" to "import com.example.generated.AnimalsQuery",
          "Markers.kt" to "val animalsQuery = AliasedAnimalsQuery()",
      ),
  )

  @Test
  fun fragment() = testFindUsages(
      fromFile = "src/main/graphql/fragments/ComputerFields.graphql",
      moveCaret = { moveCaret("computerFields") },
      expected = listOf(
          "ComputersQuery.graphql" to "...computerFields",
          "Main.kt" to "import com.example.generated.fragment.ComputerFields",
          "Main.kt" to "val computerFields = ComputerFields(",
          "Main.kt" to "screen = ComputerFields.Screen(",
      ),
  )

  @Test
  fun enumClass() = testFindUsages(
      fromFile = "src/main/graphql/schema.graphqls",
      moveCaret = { moveCaret("myEnum", afterText = "enum myEnum {") },
      expected = listOf(
          "Main.kt" to "import com.example.generated.type.MyEnum",
          "Main.kt" to "MyEnum.VALUE_C",
          "schema.graphqls" to "inputEnum: myEnum",
          "schema.graphqls" to "): myEnum",
          "MyEnumQuery.graphql" to "query MyEnumQuery(\$inputEnum: myEnum) {",
      ),
  )

  @Test
  fun enumValue() = testFindUsages(
      fromFile = "src/main/graphql/schema.graphqls",
      moveCaret = { moveCaret("VALUE_C", afterText = "enum myEnum {") },
      expected = listOf(
          "Main.kt" to "MyEnum.VALUE_C",
      ),
  )


  @Test
  fun inputType() = testFindUsages(
      fromFile = "src/main/graphql/schema.graphqls",
      moveCaret = { moveCaret("personInput", afterText = "input personInput {") },
      expected = listOf(
          "schema.graphqls" to "createPerson(personInput: personInput): Boolean",
          "CreatePersonMutation.graphql" to "mutation CreatePerson(\$personInput: personInput) {",
          "Main.kt" to "import com.example.generated.type.PersonInput",
          "Main.kt" to "val personInput = PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "import com.example.generated.type.PersonInput",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
          "InputConstructor.kt" to "PersonInput(",
      )
  )

  @Test
  fun inputField() = testFindUsages(
      fromFile = "src/main/graphql/schema.graphqls",
      moveCaret = { moveCaret("lastName", afterText = "input personInput {") },
      expected = listOf(
          "Main.kt" to "lastName =",
      ),
  )
}
