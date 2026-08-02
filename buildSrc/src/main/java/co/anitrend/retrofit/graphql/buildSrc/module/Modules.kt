package co.anitrend.retrofit.graphql.buildSrc.module

internal object Modules {

    interface Module {
        val id: String

        /**
         * @return Formatted id of module as a path string
         */
        fun path(): String = ":$id"
    }

    enum class Components(override val id: String) : Module {
        App("app"),
        Library("library"),
        Annotations("annotations"),
        Api("api"),
        AndroidAssets("android-assets"),
        Runtime("runtime"),
        Compat("compat"),
        CodegenCore("codegen-core"),
        GradlePlugin("gradle-plugin"),
        SerializationApi("serialization-api"),
        SerializationGson("serialization-gson"),
        SerializationKotlinx("serialization-kotlinx"),
    }
}