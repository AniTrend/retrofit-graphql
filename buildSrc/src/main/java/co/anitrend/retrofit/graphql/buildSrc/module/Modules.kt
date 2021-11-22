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
        Library("library")
    }
}