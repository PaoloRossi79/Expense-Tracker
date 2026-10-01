# Project-specific ProGuard/R8 rules.

# Keep line numbers for readable crash reports, hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firestore maps documents to model classes reflectively.
-keepclassmembers class com.paolorossi.expensetracker.data.model.** { *; }

# Firebase / Play services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
