package co.anitrend.retrofit.graphql.buildSrc.common

import org.gradle.api.Project

internal const val sample = "app"
internal const val emojify = "library"

fun Project.isSampleModule() = name == sample
fun Project.isLibraryModule() = name == emojify