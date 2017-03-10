Used with FixturesIntegrationTest.kt to verify that the generated output code matches the expected. This runs `gradle clean build` on all projects in its subdirectories
This will as a result also run the tests placed under the project fixtures' source sets so any test under `basic/src/test` would run as well.

To add a fixture, simply create a folder with the name of the test and place an Android/Java project with a gradle build file there.
