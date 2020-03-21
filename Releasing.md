# Before Releasing


Run `./gradlew japicmp` and check what API may have changed. We ideally want 100% API and ABI compatibility but the project is still moving fast and it's not always possible.

# Releasing

0. Make sure that you have `kscript` installed - https://github.com/holgerbrandl/kscript#installation
1. Run `./scripts/release.kts` from your local dev machine.
2. The script removes the `-SNAPSHOT` suffix, commits and tags version `v1.x.y`
3. The script bumps version to the next patch version and commits
4. Push (this could be automated in a future PR but I prefer to keep a manual step for the time being)
5. CI will build tag `v1.x.y` and `scripts/deploy.sh` will see it's from a tag and deploy to bintray with the encrypted credentials

