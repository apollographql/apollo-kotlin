package com.apollographql.apollo3.compiler.codegen.kotlin.experimental

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Class which models the explicitly removed
 */
@ApolloExperimental
@Serializable
class ExplicitlyRemovedSubClasses(
  val className: String,
  val extract: String,
  val renameTo: String
) {

  val classHierarchy: List<String> by lazy {
    className.split("$")
  }

  val extractHierarchy: List<String> by lazy {
    extract.split("$")
  }

  val renamedExtractedHierarchy: List<String> by lazy {
    if (renameTo.isNotEmpty()) {
      extractHierarchy.toMutableList().apply {
        removeLast()
        add(renameTo)
      }
    } else {
      extractHierarchy
    }
  }

  val subclasses: List<String> by lazy {
    classHierarchy.drop(extractHierarchy.size)
  }

  val parentPath: List<String> by lazy {
    classHierarchy.dropLast(subclasses.size + 1)
  }

  override fun toString(): String {
    return "ExplicitlyRemovedSubClasses(className='$className', extract='$extract', renameTo='$renameTo')"
  }
}

/**
 * Node representation of the explicitly removed subclasses. Allowing us to efficiently map the class hierarchy and what is
 * supposed to be extracted. This will help us traverse the IrModelGroup tree and remove the subclasses that are supposed to be
 * in their own class, similar to flatten.
 */
@ApolloExperimental
data class ExplicitlyRemovedNode(
  val name: String,
  val hasExtracted: Boolean,
  val renameTo: String = "",
  val children: MutableMap<String, ExplicitlyRemovedNode> = mutableMapOf()
) {

  companion object {
    fun buildTree(documentList: List<ExplicitlyRemovedSubClasses>): ExplicitlyRemovedNode {
      val root = ExplicitlyRemovedNode("root", false)

      documentList.forEach { explicitlyRemovedSubClasses ->
        // Clean up the code just in case the copy & paste includes
        val classHierarchy = getCleanedHierarchy(explicitlyRemovedSubClasses.classHierarchy)
        val extractedHierarchy = getCleanedHierarchy(explicitlyRemovedSubClasses.extractHierarchy)

        // Build the tree
        walkTree(root, classHierarchy, extractedHierarchy, explicitlyRemovedSubClasses.renameTo)
      }
      return root
    }

    private fun getCleanedHierarchy(data: List<String>): MutableList<String> {
      val list = data.toMutableList()
      if (data.last().contains(".")) {
        val last = list.removeLast()
        list.add(last.substringBefore("."))
      }
      return list
    }

    private fun walkTree(
      node: ExplicitlyRemovedNode,
      classHierarchy: MutableList<String>,
      extractedHierarchy: MutableList<String>,
      renameTo: String
    ) {
      val classFirst = classHierarchy.removeFirstOrNull()
      val extractedFirst = extractedHierarchy.removeFirstOrNull()

      if (extractedFirst != null && classFirst != extractedFirst) {
        throw RuntimeException("The class hierarchy and the extracted hierarchy do not match")
      }

      if (extractedFirst == null) {
        return // At the end
      }

      val child = node.children[extractedFirst]
      val isTerminal = extractedHierarchy.isEmpty()
      if (child == null) {
        val childNode = ExplicitlyRemovedNode(extractedFirst, isTerminal, renameTo)
        node.children[extractedFirst] = childNode
        walkTree(childNode, classHierarchy, extractedHierarchy, renameTo)
      } else {
        walkTree(child, classHierarchy, extractedHierarchy, renameTo)
      }
    }// End walkTree
  } // End companion object

  override fun toString(): String {
    return "ExplicitlyRemovedNode(name='$name', children=$children, )"
  }
}

@ApolloExperimental
internal fun String.parseInformation(): ExplicitlyRemovedNode? {
  return if (this.isBlank()) {
    null
  } else {
    return try {
      // Init JSON parser
      val json = Json { ignoreUnknownKeys = true }

      // Find the file from the application root
      val file = buildFileFromUserDir(this)

      // Read the file as Text
      val jsonText = file.readText()

      // Deserialize the JSON text to a List of JsonDocument objects
      val documentList: List<ExplicitlyRemovedSubClasses> = json.decodeFromString(
        ListSerializer(ExplicitlyRemovedSubClasses.serializer()),
        jsonText
      )

      // Create a tree for processing
      ExplicitlyRemovedNode.buildTree(documentList)
    } catch (e: Exception) {
      throw RuntimeException(
        "There are issues locating or parsing your JSON file for defaultFlattenModelsExplicitly, please take a look", e
      )
    }
  }
}

fun buildFileFromUserDir(fromUserDirPath: String): File {
  val userDir = System.getProperty("user.dir")
  val fromUserDir = File(userDir, fromUserDirPath)
  return if (fromUserDir.exists()) {
    fromUserDir
  } else {
    val fromRoot = File(fromUserDirPath)
    throw RuntimeException(
      "File not found: $fromUserDirPath\n" +
          "user.dir: $userDir\n" +
          "fromUserDir: $fromUserDir\n" +
          "fromRoot: $fromRoot\n"
    )
  }
}
