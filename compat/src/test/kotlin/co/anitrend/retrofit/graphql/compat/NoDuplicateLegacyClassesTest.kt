package co.anitrend.retrofit.graphql.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.jar.JarFile

/**
 * Proves every legacy class moved into `:compat` exists exactly once on the
 * `:compat` runtime classpath and that the `io.github.wax911.library.*` type
 * aliases produce no runtime classes of their own (Kotlin type aliases erase
 * at compile time).
 *
 * Duplicates would appear when a legacy implementation was left behind in
 * another module (`:api`, `:runtime`, `:serialization-gson`,
 * `:serialization-kotlinx`) while a copy moved to `:compat`.
 */
class NoDuplicateLegacyClassesTest {

    private val legacyClassResources =
        listOf(
            "co/anitrend/retrofit/graphql/model/GraphQLRequest.class",
            "co/anitrend/retrofit/graphql/model/GraphQLJson.class",
            "co/anitrend/retrofit/graphql/model/GraphQLResponseException.class",
            "co/anitrend/retrofit/graphql/model/body/GraphContainer.class",
            "co/anitrend/retrofit/graphql/model/attribute/GraphError.class",
            "co/anitrend/retrofit/graphql/model/request/QueryContainer.class",
            "co/anitrend/retrofit/graphql/model/request/QueryContainerBuilder.class",
            "co/anitrend/retrofit/graphql/model/request/PersistedQuery.class",
            "co/anitrend/retrofit/graphql/model/request/PersistedQueryUrlParameterBuilder.class",
            "co/anitrend/retrofit/graphql/model/request/PersistedQueryUrlParameters.class",
            "co/anitrend/retrofit/graphql/converter/GraphConverter.class",
            "co/anitrend/retrofit/graphql/converter/request/GraphRequestConverter.class",
            "co/anitrend/retrofit/graphql/converter/response/GraphResponseConverter.class",
            "co/anitrend/retrofit/graphql/serialization/gson/GsonGraphQLJson.class",
            "co/anitrend/retrofit/graphql/serialization/kotlinx/KotlinxGraphQLJson.class",
            "co/anitrend/retrofit/graphql/util/GraphErrorUtilKt.class",
        )

    @Test
    fun `legacy classes exist exactly once on the compat classpath`() {
        val resources = classpathResources()
        legacyClassResources.forEach { resource ->
            val occurrences = resources.count { it.resource == resource }
            assertEquals(
                "Expected exactly one '$resource' on the :compat runtime classpath",
                1,
                occurrences,
            )
        }
    }

    @Test
    fun `legacy classes are shipped only by the compat module`() {
        val resources = classpathResources()
        legacyClassResources.forEach { resource ->
            val carriers =
                resources
                    .filter { it.resource == resource }
                    .map { it.entryPath }

            assertEquals(
                "Expected exactly one carrier for '$resource'",
                1,
                carriers.size,
            )
            assertTrue(
                "Legacy '$resource' must be shipped by :compat only, found in: $carriers",
                carriers.all { it.contains("compat") },
            )
        }
    }

    @Test
    fun `legacy library type aliases emit only empty file facade classes`() {
        // Type aliases themselves produce no classes, but Kotlin emits an
        // empty file-facade class (named <File>Kt) for every source file that
        // contains top-level declarations, including typealias-only files.
        // These facades carry no members, so they are behaviorally inert and
        // need no keep rules; this test locks that shape so a future change
        // cannot turn an alias into a real class.
        val aliasClasses =
            classpathResources()
                .filter { it.resource.startsWith("io/github/wax911/library/") && it.resource.endsWith(".class") }

        val nonFacade = aliasClasses.filterNot { it.resource.endsWith("Kt.class") }
        assertTrue(
            "Expected only empty *Kt facade classes under io/github/wax911/library/, found: ${nonFacade.take(5)}",
            nonFacade.isEmpty(),
        )
        // Every typealias source file contributes exactly one facade class.
        assertEquals(37, aliasClasses.size)
    }

    private data class ClasspathEntry(
        val entryPath: String,
        val resource: String,
    )

    private fun classpathResources(): List<ClasspathEntry> =
        System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .flatMap { entry ->
                val file = File(entry)
                when {
                    file.isDirectory ->
                        file.walkTopDown()
                            .filter { it.isFile }
                            .map { ClasspathEntry(entry, it.relativeTo(file).invariantSeparatorsPath) }
                            .toList()

                    file.isFile && file.name.endsWith(".jar") ->
                        JarFile(file).use { jar ->
                            jar.entries().asSequence().map { ClasspathEntry(entry, it.name) }.toList()
                        }

                    else -> emptyList()
                }
            }
}
