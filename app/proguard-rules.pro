# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to release builds only.

# Keep ViewModel subclasses
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep data/model classes (add specific rules when models are added)
-keep class com.labeliq.app.domain.model.** { *; }
