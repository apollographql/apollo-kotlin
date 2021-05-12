import com.apollographql.apollo3.testing.checkFile
import com.apollographql.apollo3.testing.readFile
import kotlin.test.assertEquals

fun readTestFixture(name: String) = readFile("../integration-tests/testFixtures/$name")
fun checkTestFixture(actualText: String, name: String) = checkFile(actualText, "../integration-tests/testFixtures/$name")

fun readResource(name: String) = readFile("../integration-tests/testFixtures/resources/$name")


/**
 * A helper function to reverse the order of the argument so that we can easily column edit the tests
 */
fun assertEquals2(actual: Any?, expected: Any?) = assertEquals(expected, actual)
