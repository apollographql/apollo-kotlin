Performance benchmarks for the native targets.

Results can be seen in the `measurements` file.

To update the file with up-to-date measurements, remove the `@Ignore` annotation in `BenchmarksTest` and run it:

```shell
./gradlew -p tests :native-benchmarks:macosArm64Test
```
