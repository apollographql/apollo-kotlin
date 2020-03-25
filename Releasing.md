# Before Releasing


Run `./gradlew japicmp` and check what API may have changed. We ideally want 100% API and ABI compatibility but the project is still moving fast and it's not always possible.

# Releasing

0. Make sure that you have `kotlin 1.3.70+` installed.
1. Run `./scripts/release.main.kts` from your local dev machine.
2. The script removes the `-SNAPSHOT` suffix, commits and tags version `v1.x.y`.
3. The script bumps version to the next patch version and commits.
4. Verify that everything is OK and push.
5. The CI will build tag `v1.x.y` and deploy the artifact.

