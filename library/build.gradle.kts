import co.anitrend.retrofit.graphql.buildSrc.Libraries

plugins {
    id("co.anitrend.retrofit.graphql")
}

dependencies {
    implementation(Libraries.AndroidX.Annotation.annotation)

    testImplementation(Libraries.JetBrains.Kotlin.reflect)
    testImplementation(Libraries.AndroidX.Test.coreKtx)
    testImplementation(Libraries.AndroidX.Test.Extension.junit)
    testImplementation(Libraries.AndroidX.Test.Extension.junitKtx)
}