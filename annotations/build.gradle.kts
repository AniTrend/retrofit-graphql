import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureDokkaForJvm
import co.anitrend.retrofit.graphql.buildSrc.plugin.components.configureSpotlessForJvm

plugins {
    kotlin("jvm")
}

configureSpotlessForJvm()
configureDokkaForJvm()

