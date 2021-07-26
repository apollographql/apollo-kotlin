This project contains the tests for the vast majority of `apollo-xyz` modules that share a "standard"
 Gradle configuration with MPP support, no Android, etc...

In particular, these tests do not use fragments because fragments are generated differently depending the codegen options. Tests that use fragments are available in the `models-response-based` or `models-operation-based` projects