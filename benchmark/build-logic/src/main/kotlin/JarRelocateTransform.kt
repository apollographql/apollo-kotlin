import me.lucko.jarrelocator.JarRelocator
import me.lucko.jarrelocator.Relocation
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.IOException

@CacheableTransform
abstract class JarRelocateTransform : TransformAction<JarRelocateTransform.Parameters> {
  interface Parameters : TransformParameters {
    @get:Input
    val relocations: MapProperty<String, String>
  }

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputArtifact
  abstract val inputArtifact: Provider<FileSystemLocation>

  override fun transform(outputs: TransformOutputs) {
    val renames = parameters.relocations.get()

    val inputFile = inputArtifact.get().asFile
    val outputFile = outputs.file(inputFile.nameWithoutExtension + "-relocated.jar")

    // Make sure the output is ready
    outputFile.parentFile.mkdirs()
    outputFile.delete()

    val rules = renames.entries.map {
      Relocation(it.key, it.value)
    }

    try {
      JarRelocator(inputFile, outputFile, rules).run()
    } catch (e: IOException) {
      throw RuntimeException("Unable to relocate", e)
    }
  }
}
