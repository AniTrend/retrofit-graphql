package co.anitrend.retrofit.graphql.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.jar.JarFile

/**
 * Aggregate-classpath duplicate check for the deprecated `:library` facade.
 *
 * The historical `co.anitrend:retrofit-graphql` artifact must expose exactly
 * one copy of every legacy class: the legacy implementation lives in
 * `:compat` (re-exported via `api()`), while `:api`, `:runtime`,
 * `:serialization-gson`, and `:serialization-kotlinx` ship only their neutral
 * contracts and the new transport codecs. Any second copy on this classpath
 * means a legacy class was left behind in one of those modules.
 */
class AggregateClasspathDuplicateTest {

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
    fun `aggregate classpath contains exactly one copy of every legacy class`() {
        val resources = classpathResources()
        legacyClassResources.forEach { resource ->
            val occurrences = resources.count { it.resource == resource }
            assertEquals(
                "Expected exactly one '$resource' on the :library aggregate classpath",
                1,
                occurrences,
            )
        }
    }

    @Test
    fun `aggregate classpath contains the new transport codecs exactly once`() {
        val resources = classpathResources()
        listOf(
            "co/anitrend/retrofit/graphql/serialization/gson/GsonGraphQLTransportCodec.class",
            "co/anitrend/retrofit/graphql/serialization/kotlinx/KotlinxGraphQLTransportCodec.class",
            "co/anitrend/retrofit/graphql/converter/GraphQLConverterFactory.class",
            "co/anitrend/retrofit/graphql/model/request/GraphQLOperationRequest.class",
        ).forEach { resource ->
            val occurrences = resources.count { it.resource == resource }
            assertEquals(
                "Expected exactly one '$resource' on the :library aggregate classpath",
                1,
                occurrences,
            )
        }
    }

    @Test
    fun `legacy library type aliases emit only empty file facade classes on the aggregate`() {
        // Type aliases themselves produce no classes, but Kotlin emits an
        // empty file-facade class (named <File>Kt) for every typealias-only
        // source file. These facades carry no members, so they are inert and
        // need no keep rules; this test locks that shape on the aggregate.
        val aliasClasses =
            classpathResources()
                .filter { it.resource.startsWith("io/github/wax911/library/") && it.resource.endsWith(".class") }

        val nonFacade = aliasClasses.filterNot { it.resource.endsWith("Kt.class") }
        assertTrue(
            "Expected only empty *Kt facade classes under io/github/wax911/library/, found: ${nonFacade.take(5)}",
            nonFacade.isEmpty(),
        )
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
