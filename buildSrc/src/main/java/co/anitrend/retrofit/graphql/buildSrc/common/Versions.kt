package co.anitrend.retrofit.graphql.buildSrc.common

object Versions {

    private fun Int.toVersion(): String {
        return if (this < 9) "0$this" else "$this"
    }

    const val compileSdk = 30
    const val targetSdk = 30
    const val minSdk = 17

    private const val major = 0
    private const val minor = 11
    private const val patch = 0
    private const val revision = 2

    private const val channel = "beta"

    const val versionCode = major * 100_000 + minor * 10_000 + patch * 1_000 + revision * 100
    val versionName = if (revision > 0) 
        "$major.$minor.$patch-$channel${revision.toVersion()}"
    else "$major.$minor.$patch"

    const val mockk = "1.12.0"
    const val junit = "4.13.2"

    const val timber = "5.0.1"
    const val threeTenBp = "1.3.1"
    const val ktlint = "0.43.0"
    
    const val debugDB = "1.0.6"
    const val treesSence = "0.3.2"
}
