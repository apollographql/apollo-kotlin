import me.lucko.jarrelocator.JarRelocator
import me.lucko.jarrelocator.Relocation
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.IOException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@CacheableTransform
abstract class AarRelocateTransform : TransformAction<AarRelocateTransform.Parameters> {
  interface Parameters : TransformParameters {
    @get:Input
    val relocations: MapProperty<String, String>
    @get:Input
    val random: Property<Int>
    @get:Internal
    val tmpDir: DirectoryProperty
  }

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputArtifact
  abstract val inputArtifact: Provider<FileSystemLocation>

  override fun transform(outputs: TransformOutputs) {
    val renames = parameters.relocations.get()
    val inputFile = inputArtifact.get().asFile
    val outputFile = outputs.file(inputFile.nameWithoutExtension + "-relocated.aar")
    // Make sure the output is ready
    outputFile.parentFile.mkdirs()
    outputFile.delete()

    val tmpFolder = parameters.tmpDir.asFile.get()
    tmpFolder.deleteRecursively()
    tmpFolder.mkdirs()

    println("relocating AAR")

    ZipOutputStream(outputFile.outputStream()).use output@{ zipOutputStream ->
      ZipInputStream(inputFile.inputStream()).use input@{ zipInputStream ->
        while (true) {
          val entry = zipInputStream.nextEntry

          if (entry == null) {
            return@input
          }

          println("put next entry: ${entry.name}")
          zipOutputStream.putNextEntry(entry)

          if (entry.name == "classes.jar") {
            val rules = renames.entries.map {
              Relocation(it.key, it.value)
            }

            val relocatorInput = tmpFolder.resolve("input.jar")
            val relocatorOutput = tmpFolder.resolve("output.jar")

            relocatorInput.outputStream().buffered().use {
              zipInputStream.transferTo(it)
            }
            try {
              JarRelocator(relocatorInput, relocatorOutput, rules).run()
            } catch (e: IOException) {
              throw RuntimeException("Unable to relocate", e)
            }

            relocatorOutput.inputStream().use {
              it.transferTo(zipOutputStream)
            }
          } else {
            zipInputStream.transferTo(zipOutputStream)
          }
          zipOutputStream.closeEntry()
        }
      }
    }
  }
}
