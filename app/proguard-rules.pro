# Add project specific ProGuard rules here.
# Release builds now ship with isMinifyEnabled = true / isShrinkResources = true (see
# app/build.gradle.kts), so the rules below are load-bearing: without them R8 strips or renames
# the reflection-driven kotlinx.serialization companions/serializers that BudgetRepository.kt
# needs to (de)serialize the DataStore JSON blob, breaking release builds silently at runtime.

# Keep kotlinx.serialization's generated $$serializer companion objects and their signatures.
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.budgettracker.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.budgettracker.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.budgettracker.app.data.**$$serializer {
    *** INSTANCE;
    *** serialize(...);
    *** deserialize(...);
    *** descriptor(...);
}

# Explicit keep rules for the @Serializable model classes themselves and their generated
# serializers (BudgetModels.kt): field names/order must survive intact for JSON (de)serialization.
-keep,includedescriptorclasses class com.budgettracker.app.data.Category { *; }
-keep,includedescriptorclasses class com.budgettracker.app.data.Category$$serializer { *; }
-keep,includedescriptorclasses class com.budgettracker.app.data.PeriodBudget { *; }
-keep,includedescriptorclasses class com.budgettracker.app.data.PeriodBudget$$serializer { *; }
-keep,includedescriptorclasses class com.budgettracker.app.data.BudgetState { *; }
-keep,includedescriptorclasses class com.budgettracker.app.data.BudgetState$$serializer { *; }
