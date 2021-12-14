This project contains the bulk of integration tests modules. These tests:

- Don't use android
- Don't use sample-server
- Don't use RxJava
- Use operationBased codegen
- Are run with both Java and Kotlin codegen
- Are run on the JVM and on MacOS

For more specialized tests, create a separate module
