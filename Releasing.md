# Before Releasing


Run `./gradlew checkJapicmp` and check what API may have changed. We ideally want 100% API and ABI compatibility but the project is still moving fast and it's not always possible.

# Releasing

0. Make sure that you have `kotlin 1.3.70+` installed.
1. Run `./scripts/bump.main.kts` from your local dev machine.
2. Follow the instructions there for the rest of the release

