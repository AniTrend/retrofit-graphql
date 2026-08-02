# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.kts.
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

# =============================================================================
# Gson reflective serialization for multipart GraphQL uploads
# =============================================================================
# UploadMutationHelper uses Gson (not kotlinx.serialization) to serialize the
# operations payload of a GraphQL multipart upload request. Gson derives JSON
# keys from java.lang.reflect.Field.getName(), so field names must not be
# renamed by R8. kotlinx.serialization consumer rules do not protect field
# names because kotlinx.serialization uses compile-time generated descriptors
# with explicit wire names, never reflection.
#
# These rules are narrowly scoped to the classes serialized by Gson.toJson()
# in the upload path. -keep preserves both the class and its members (fields,
# methods) so Gson's reflective serialization works correctly.
#
# GraphQLVariables is an interface; it does not carry serializable state.

# GraphQLRequest: serialized by Gson in UploadMutationHelper.createOperationsPart()
# of the legacy bucket upload path. The class is published from :compat with
# the same fully qualified name. The neutral GraphQLOperationRequest path is
# NOT kept here: the explicit codec serializes it without reflection.
-keep class co.anitrend.retrofit.graphql.model.GraphQLRequest {
    <fields>;
    <init>(...);
}

# UploadToStorageBucketVariables: serialized by Gson as the nested variables
# object within the operations payload of a multipart upload request.
-keep class co.anitrend.retrofit.graphql.sample.bucket.UploadToStorageBucketVariables {
    <fields>;
    <init>(...);
}
