plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("retrofitGraphQL") {
            id = "co.anitrend.retrofit.graphql.codegen"
            implementationClass = "co.anitrend.retrofit.graphql.codegen.RetrofitGraphQLPlugin"
        }
    }
}

dependencies {
    implementation(project(":codegen-core"))
    implementation(libs.kotlinpoet)
    implementation(gradleApi())
}
