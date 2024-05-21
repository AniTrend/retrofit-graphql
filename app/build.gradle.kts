import java.net.URI

plugins {
    id("co.anitrend.retrofit.graphql")
    id("kotlinx-serialization")
}

repositories {
    maven {
        url = URI("https://jitpack.io")
    }
}

android {
    namespace = "co.anitrend.retrofit.graphql.sample"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":library"))

    implementation(libs.jetbrains.kotlinx.serialization.json)

    implementation(libs.google.material)
    implementation(libs.threeTenBp)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recycler.view)

    implementation(libs.androidx.lifecycle.livedata.core)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.runtime.ktx)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

    implementation(libs.anitrend.arch.ui)
    implementation(libs.anitrend.arch.ext)
    implementation(libs.anitrend.arch.core)
    implementation(libs.anitrend.arch.data)
    implementation(libs.anitrend.arch.theme)
    implementation(libs.anitrend.arch.domain)
    implementation(libs.anitrend.arch.recycler)
    implementation(libs.anitrend.emojify)

    implementation(libs.coil)
    implementation(libs.coil.gif)

    implementation(libs.timber)

    releaseImplementation(libs.chuncker.release)
    debugImplementation(libs.chuncker.debug)
}
