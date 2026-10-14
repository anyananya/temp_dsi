# Room entities and DAOs are kept by Room's own consumer rules.
# OkHttp ships its own consumer rules; silence optional-platform warnings.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
