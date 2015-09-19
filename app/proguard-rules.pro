# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Program Files (x86)\Android\android-studio\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


## --------------------------------------------------------------------------------
#
# Ease future debugging
#
## --------------------------------------------------------------------------------

-keepattributes SourceFile,LineNumberTable



## --------------------------------------------------------------------------------
#
# Preventing stripping of EventBus onEvent* methods
#
## --------------------------------------------------------------------------------

-keepclassmembers class ** {
    public void onEvent(**);
}


-keepclassmembers class ** {
    public void onEventMainThread(**);
}


## --------------------------------------------------------------------------------
#
# org.apache.commons.validator package references many other org.apache.commons
# packages, which are not included in the app. This makes ProGuard go crazy.
# Disable warnings for org.apache.commons altogether.
#
## --------------------------------------------------------------------------------

-dontwarn org.apache.commons.**
