import co.anitrend.retrofit.graphql.buildSrc.Libraries
import java.net.URI

plugins {
    id("co.anitrend.retrofit.graphql")
    id("kotlinx-serialization")
    id("koin")
}

repositories {
    maven {
        url = URI("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":library"))

    implementation(Libraries.JetBrains.KotlinX.Serialization.runtime)

    implementation(Libraries.Google.Material.material)
    implementation(Libraries.threeTenBp)

    implementation(Libraries.AndroidX.Core.coreKtx)
    implementation(Libraries.AndroidX.StartUp.startUpRuntime)
    implementation(Libraries.AndroidX.Collection.collectionKtx)
    implementation(Libraries.AndroidX.Fragment.fragmentKtx)
    implementation(Libraries.AndroidX.Activity.activityKtx)
    implementation(Libraries.AndroidX.Emoji.appCompat)
    implementation(Libraries.AndroidX.ConstraintLayout.constraintLayout)
    implementation(Libraries.AndroidX.SwipeRefresh.swipeRefreshLayout)
    implementation(Libraries.AndroidX.Preference.preferenceKtx)
    implementation(Libraries.AndroidX.Recycler.recyclerView)
    implementation(Libraries.AndroidX.Work.runtimeKtx)

    implementation(Libraries.AndroidX.Paging.common)
    implementation(Libraries.AndroidX.Paging.runtime)
    implementation(Libraries.AndroidX.Paging.runtimeKtx)

    implementation(Libraries.AndroidX.Room.ktx)
    implementation(Libraries.AndroidX.Room.runtime)
    kapt(Libraries.AndroidX.Room.compiler)

    implementation(Libraries.AniTrend.Arch.ui)
    implementation(Libraries.AniTrend.Arch.ext)
    implementation(Libraries.AniTrend.Arch.core)
    implementation(Libraries.AniTrend.Arch.data)
    implementation(Libraries.AniTrend.Arch.theme)
    implementation(Libraries.AniTrend.Arch.domain)
    implementation(Libraries.AniTrend.Arch.recycler)
    implementation(Libraries.AniTrend.Emojify.emojify)

    implementation(Libraries.Coil.coil)
    implementation(Libraries.Coil.gif)

    implementation(Libraries.timber)
    implementation(Libraries.debugDb)
    implementation(Libraries.treessence)

    releaseImplementation(Libraries.Chuncker.release)
    debugImplementation(Libraries.Chuncker.debug)
}
