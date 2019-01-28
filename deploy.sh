GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CLEAR='\033[0m'

set -e

echo ${YELLOW}Deploying Android Support ...${CLEAR}
./gradlew clean :apollo-android-support:bintrayUpload
echo ${YELLOW}Deploying Android Support - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Api ...${CLEAR}
./gradlew clean :apollo-api:bintrayUpload
echo ${YELLOW}Deploying Api - ${GREEN}Done!${CLEAR}


echo ${YELLOW}Deploying Compiler ...${CLEAR}
./gradlew clean :apollo-compiler:bintrayUpload
echo ${YELLOW}Deploying Compiler - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Coroutines Support ...${CLEAR}
./gradlew clean :apollo-coroutines-support:bintrayUpload
echo ${YELLOW}Deploying Coroutines Support - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Espresso Support ...${CLEAR}
./gradlew clean :apollo-espresso-support:bintrayUpload
echo ${YELLOW}Deploying Espresso Support - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Gradle Plugin ...${CLEAR}
./gradlew clean :apollo-gradle-plugin:bintrayUpload
echo ${YELLOW}Deploying Gradle Plugin - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Http Cache ...${CLEAR}
./gradlew clean :apollo-http-cache:bintrayUpload
echo ${YELLOW}Deploying Http Cache - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Runtime ...${CLEAR}
./gradlew clean :apollo-runtime:bintrayUpload
echo ${YELLOW}Deploying Runtime - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Rx2 Support ...${CLEAR}
./gradlew clean :apollo-rx2-support:bintrayUpload
echo ${YELLOW}Deploying Rx2 Support - ${GREEN}Done!${CLEAR}

echo ${YELLOW}Deploying Rx Support ...${CLEAR}
./gradlew clean :apollo-rx-support:bintrayUpload
echo ${YELLOW}Deploying Rx Support - ${GREEN}Done!${CLEAR}

echo ${GREEN}ALL DONE!${CLEAR}