import co.anitrend.retrofit.graphql.buildSrc.Libraries
import java.net.URI

plugins {
    id("co.anitrend.retrofit.graphql")
}

repositories {
    maven {
        url = URI("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":library"))

    implementation(Libraries.Google.Material.material)

    implementation(Libraries.AndroidX.Core.coreKtx)
    implementation(Libraries.AndroidX.ContraintLayout.constraintLayout)
    implementation(Libraries.AndroidX.SwipeRefresh.swipeRefreshLayout)
    implementation(Libraries.AndroidX.Recycler.recyclerView)

    implementation(Libraries.Coil.coil)

    implementation(Libraries.Glide.glide)
    kapt(Libraries.Glide.compiler)

    implementation(Libraries.timber)
    implementation(Libraries.debugDb)
    implementation(Libraries.treessence)

    releaseImplementation(Libraries.Chuncker.release)
    debugImplementation(Libraries.Chuncker.debug)
}
