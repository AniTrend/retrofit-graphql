package co.anitrend.retrofit.graphql.buildSrc

import co.anitrend.retrofit.graphql.buildSrc.common.Versions

object Libraries {
    const val threeTenBp = "com.jakewharton.threetenabp:threetenabp:${Versions.threeTenBp}"
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"

    const val treessence = "fr.bipi.treessence:treessence:${Versions.treesSence}"
    const val debugDb = "com.amitshekhar.android:debug-db:${Versions.debugDB}"

    const val junit = "junit:junit:${Versions.junit}"
    const val mockk = "io.mockk:mockk:${Versions.mockk}"

    object Android {

        object Tools {
            private const val version = "4.0.1"
            const val buildGradle = "com.android.tools.build:gradle:$version"
        }
    }

    object AndroidX {

        object Activity {
            private const val version = "1.2.0-alpha06"
            const val activity = "androidx.activity:activity:$version"
            const val activityKtx = "androidx.activity:activity-ktx:$version"
        }

        object Annotation {
            private const val version = "1.2.0-alpha01"
            const val annotation = "androidx.annotation:annotation:$version"
        }

        object AppCompat {
            private const val version = "1.3.0-alpha01"
            const val appcompat = "androidx.appcompat:appcompat:$version"
            const val appcompatResources = "androidx.appcompat:appcompat-resources:$version"
        }

        object Collection {
            private const val version = "1.1.0"
            const val collection = "androidx.collection:collection:$version"
            const val collectionKtx = "androidx.collection:collection-ktx:$version"
        }

        object Core {
            private const val version = "1.5.0-alpha01"
            const val core = "androidx.core:core:$version"
            const val coreKtx = "androidx.core:core-ktx:$version"
        }

        object ConstraintLayout {
            private const val version = "2.0.0-beta7"
            const val constraintLayout = "androidx.constraintlayout:constraintlayout:$version"
            const val constraintLayoutSolver = "androidx.constraintlayout:constraintlayout-solver:$version"
        }

        object Emoji {
            private const val version = "1.1.0-rc01"
            const val appCompat = "androidx.emoji:emoji-appcompat:$version"
        }

        object Fragment {
            private const val version = "1.3.0-alpha06"
            const val fragment = "androidx.fragment:fragment:$version"
            const val fragmentKtx = "androidx.fragment:fragment-ktx:$version"
            const val test = "androidx.fragment:fragment-ktx:fragment-testing$version"
        }

        object Lifecycle {
            private const val version = "2.2.0"
            const val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
            const val runTimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            const val liveDataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val liveDataCoreKtx = "androidx.lifecycle:lifecycle-livedata-core-ktx:$version"
        }

        object Paging {
            private const val version = "2.1.2"
            const val common = "androidx.paging:paging-common-ktx:$version"
            const val runtime = "androidx.paging:paging-runtime:$version"
            const val runtimeKtx = "androidx.paging:paging-runtime-ktx:$version"
        }

        object Preference {
            private const val version = "1.1.0"
            const val preference = "androidx.preference:preference:$version"
            const val preferenceKtx = "androidx.preference:preference-ktx:$version"
        }

        object Recycler {
            private const val version = "1.2.0-alpha03"
            const val recyclerView = "androidx.recyclerview:recyclerview:$version"
            const val recyclerViewSelection = "androidx.recyclerview:recyclerview-selection:$version"
        }

        object Room {
            private const val version = "2.2.5"
            const val compiler = "androidx.room:room-compiler:$version"
            const val runtime = "androidx.room:room-runtime:$version"
            const val test = "androidx.room:room-testing:$version"
            const val ktx = "androidx.room:room-ktx:$version"
        }

        object StartUp {
            private const val version = "1.0.0-alpha02"
            const val startUpRuntime = "androidx.startup:startup-runtime:$version"
        }

        object SwipeRefresh {
            private const val version = "1.1.0-rc01"
            const val swipeRefreshLayout = "androidx.swiperefreshlayout:swiperefreshlayout:$version"
        }

        object Test {
            private const val version = "1.3.0-rc01"
            const val core = "androidx.test:core:$version"
            const val coreKtx = "androidx.test:core-ktx:$version"
            const val runner = "androidx.test:runner:$version"
            const val rules = "androidx.test:rules:$version"

            object Espresso {
                private const val version = "3.3.0-rc01"
                const val core = "androidx.test.espresso:espresso-core:$version"
            }

            object Extension {
                private const val version = "1.1.2-rc01"
                const val junit = "androidx.test.ext:junit:$version"
                const val junitKtx = "androidx.test.ext:junit-ktx:$version"
            }
        }

        object Work {
            private const val version = "2.4.0-beta01"
            const val runtimeKtx = "androidx.work:work-runtime-ktx:$version"
            const val runtime = "androidx.work:work-runtime:$version"
            const val test = "androidx.work:work-test:$version"
        }
    }

    object AniTrend {

        object Arch {
            private const val version = "1.3.0-beta14"
            const val ui = "com.github.anitrend.support-arch:support-ui:${version}"
            const val ext = "com.github.anitrend.support-arch:support-ext:${version}"
            const val core = "com.github.anitrend.support-arch:support-core:${version}"
            const val data = "com.github.anitrend.support-arch:support-data:${version}"
            const val theme = "com.github.anitrend.support-arch:support-theme:${version}"
            const val domain = "com.github.anitrend.support-arch:support-domain:${version}"
            const val recycler = "com.github.anitrend.support-arch:support-recycler:${version}"
        }

        object Emojify {
            private const val version = "1.6.0-alpha01"
            const val emojify = "com.github.anitrend:android-emojify:$version"
        }
    }

    object Chuncker {
        private const val version = "3.2.0"

        const val debug = "com.github.ChuckerTeam.Chucker:library:$version"
        const val release = "com.github.ChuckerTeam.Chucker:library-no-op:$version"
    }

    object Coil {
        private const val version = "0.11.0"
        const val coil = "io.coil-kt:coil:$version"
        const val base = "io.coil-kt:coil-base:$version"
        const val gif = "io.coil-kt:coil-gif:$version"
        const val svg = "io.coil-kt:coil-svg:$version"
        const val video = "io.coil-kt:coil-video:$version"
    }

    object Google {

        object Material {
            private const val version = "1.3.0-alpha01"
            const val material = "com.google.android.material:material:$version"
        }
    }

    object JetBrains {

        object Dokka {
            private const val version = "0.10.1"
            const val gradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:$version"
        }

        object Kotlin {
            private const val version = "1.4.0"
            const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
            const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"

            object Gradle {
                const val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
                const val serialization = "org.jetbrains.kotlin:kotlin-serialization:$version"
            }

            object Android {
                const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:$version"
            }
        }

        object KotlinX {
            object Coroutines {
                private const val version = "1.3.8"
                const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
                const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
                const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
            }

            object Serialization {
                private const val version = "0.20.0"
                const val runtime = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$version"
            }
        }
    }

    object Koin {
        private const val version = "2.1.6"
        const val core = "org.koin:koin-core:$version"
        const val extension = "org.koin:koin-core-ext:$version"
        const val test = "org.koin:koin-test:$version"

        object AndroidX {
            const val scope = "org.koin:koin-androidx-scope:$version"
            const val fragment = "org.koin:koin-androidx-fragment:$version"
            const val viewmodel = "org.koin:koin-androidx-viewmodel:$version"
        }

        object Gradle {
            const val plugin = "org.koin:koin-gradle-plugin:$version"
        }
    }

    object Markwon {
        private const val version = "4.4.0"
        const val core = "io.noties.markwon:core:$version"
        const val html = "io.noties.markwon:html:$version"
        const val image = "io.noties.markwon:image:$version"
        const val glide = "io.noties.markwon:image-glide:$version"
        const val coil = "io.noties.markwon:image-coil:$version"
        const val parser = "io.noties.markwon:inline-parser:$version"
        const val linkify = "io.noties.markwon:linkify:$version"
        const val simpleExt = "io.noties.markwon:simple-ext:$version"
        const val syntaxHighlight = "io.noties.markwon:syntax-highlight:$version"

        object Extension {
            const val taskList = "io.noties.markwon:ext-tasklist:$version"
            const val strikeThrough = "io.noties.markwon:ext-strikethrough:$version"
            const val tables = "io.noties.markwon:ext-tables:$version"
            const val latex = "io.noties.markwon:ext-latex:$version"
        }
    }

    object Square {

        object LeakCanary {
            private const val version = "2.3"
            const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$version"
        }

        object Retrofit {
            private const val version = "2.9.0"
            const val retrofit = "com.squareup.retrofit2:retrofit:$version"
            const val gsonConverter =  "com.squareup.retrofit2:converter-gson:$version"
        }

        object OkHttp {
            private const val version = "4.7.2"
            const val okhttp = "com.squareup.okhttp3:okhttp:$version"
            const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
            const val mockServer = "com.squareup.okhttp3:mockwebserver:$version"
        }
    }
}