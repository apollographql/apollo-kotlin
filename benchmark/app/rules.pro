# Obsfuscation must be disabled for the build variant that generates Baseline Profile, otherwise
# wrong symbols would be generated. The generated Baseline Profile will be properly applied when generated
# without obfuscation and your app is being obfuscated.
-dontobfuscate

# Annotations referenced by androidx.benchmark and androidx.test
-dontwarn androidx.core.os.BuildCompat$PrereleaseSdkCheck
-dontwarn androidx.core.os.BuildCompat
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.MustBeClosed
