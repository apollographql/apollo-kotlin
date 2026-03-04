#!/usr/bin/env sh

./gradlew rmbuild
./gradlew -p tests :apollo-kotlin:kotlinUpgradePackageLock :apollo-kotlin:kotlinWasmUpgradePackageLock :kotlinUpgradePackageLock :kotlinWasmUpgradePackageLock --rerun-tasks