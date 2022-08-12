fun checkGitStatus() {
  val modifiedFiles = runCommand("git", "status", "--porcelain")
  if (modifiedFiles.isNotEmpty()) {
    error("The CI modified local files. This is certainly an indication that they should have been included in the PR. Modified files:\n$modifiedFiles")
  }
}

fun runCommand(vararg args: String): String {
  val builder = ProcessBuilder(*args)
      .redirectError(ProcessBuilder.Redirect.INHERIT)

  val process = builder.start()
  val ret = process.waitFor()

  val output = process.inputStream.bufferedReader().readText()
  if (ret != 0) {
    throw java.lang.Exception("command ${args.joinToString(" ")} failed:\n$output")
  }

  return output
}
