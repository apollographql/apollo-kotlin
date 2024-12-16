# Test server using Apollo Server, for `@defer` tests

- This uses graphql-js `17.0.0-alpha.7`, which implements the latest draft of the `@defer` incremental format (as of 2024-12-16).
- Apollo Server `4.11.2` needs a patch (in `patches`) to surface this format in the responses.
