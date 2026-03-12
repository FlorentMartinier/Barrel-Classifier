# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 1. Empêcher la suppression des modèles et leurs constructeurs
-keep class com.fmartinier.barrelclassifier.data.model.** { *; }

# 2. Conserver les annotations Jackson pour qu'il retrouve ses clés
-keepattributes *Annotation*

# 3. Conserver les informations sur les types génériques (pour List<Barrel>)
-keepattributes Signature

# Conserver les signatures de type pour que Jackson puisse lire les Generics
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses