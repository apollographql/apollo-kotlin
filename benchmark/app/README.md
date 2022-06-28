A small empty apk to make Firebase Test Labs happy because it requires an 'app' APK:

```
gcloud --project apollo-kotlin firebase test android run --type=instrumentation 
--device model=Pixel3,locale=en,orientation=portrait --test=benchmark/build/outputs/apk/androidTest/release/benchmark-release-androidTest.apk 
--app benchmark/app/build/outputs/apk/release/app-release-unsigned.apk
```