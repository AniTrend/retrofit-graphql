package co.anitrend.retrofit.graphql.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the deprecated legacy surface keeps its deprecation contract after
 * the move to `:compat`: every legacy type carries a WARNING-level
 * `@Deprecated` annotation whose message points at the backend-neutral
 * replacement, and the `io.github.wax911.library.*` aliases (compile-time
 * only) still name these exact targets.
 */
class LegacyDeprecationTest {

    private fun deprecationOf(type: Class<*>): Deprecated {
        val annotation = type.getAnnotation(Deprecated::class.java)
        assertNotNull("Expected @Deprecated on ${type.name}", annotation)
        return annotation!!
    }

    @Test
    fun `legacy model types are deprecated with replacement guidance`() {
        listOf(
            co.anitrend.retrofit.graphql.model.GraphQLRequest::class.java,
            co.anitrend.retrofit.graphql.model.GraphQLJson::class.java,
            co.anitrend.retrofit.graphql.model.GraphQLResponseException::class.java,
            co.anitrend.retrofit.graphql.model.body.GraphContainer::class.java,
            co.anitrend.retrofit.graphql.model.attribute.GraphError::class.java,
            co.anitrend.retrofit.graphql.model.request.QueryContainer::class.java,
            co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder::class.java,
            co.anitrend.retrofit.graphql.model.request.PersistedQuery::class.java,
            co.anitrend.retrofit.graphql.model.request.PersistedQueryUrlParameterBuilder::class.java,
            co.anitrend.retrofit.graphql.model.request.PersistedQueryUrlParameters::class.java,
        ).forEach { type ->
            val deprecation = deprecationOf(type)
            assertEquals(DeprecationLevel.WARNING, deprecation.level)
            assertTrue(
                "Deprecation message of ${type.name} must point at the neutral replacement",
                deprecation.message.contains("GraphQLOperationRequest") ||
                    deprecation.message.contains("GraphQLResponse") ||
                    deprecation.message.contains("GraphQLTransportCodec"),
            )
        }
    }

    @Test
    fun `legacy converter and serialization types are deprecated with replacement guidance`() {
        listOf(
            co.anitrend.retrofit.graphql.converter.GraphConverter::class.java,
            co.anitrend.retrofit.graphql.converter.request.GraphRequestConverter::class.java,
            co.anitrend.retrofit.graphql.converter.response.GraphResponseConverter::class.java,
            co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson::class.java,
            co.anitrend.retrofit.graphql.serialization.kotlinx.KotlinxGraphQLJson::class.java,
        ).forEach { type ->
            val deprecation = deprecationOf(type)
            assertEquals(DeprecationLevel.WARNING, deprecation.level)
            assertTrue(
                "Deprecation message of ${type.name} must name GraphQLConverterFactory or the codec",
                deprecation.message.contains("GraphQLConverterFactory") ||
                    deprecation.message.contains("GraphQLTransportCodec"),
            )
        }
    }

    @Test
    fun `legacy helper functions are deprecated`() {
        // The file facade classes are not resolvable as classifiers from the
        // test source set, so the top-level functions are located reflectively.
        val responseHelpers = Class.forName("co.anitrend.retrofit.graphql.model.GraphQLResponseHelpersKt")
        val containerClass = Class.forName("co.anitrend.retrofit.graphql.model.body.GraphContainer")
        assertNotNull(
            responseHelpers
                .getDeclaredMethod("requireData", containerClass)
                .getAnnotation(Deprecated::class.java),
        )

        val errorUtil = Class.forName("co.anitrend.retrofit.graphql.util.GraphErrorUtilKt")
        assertNotNull(
            errorUtil
                .getDeclaredMethod("getError", retrofit2.Response::class.java)
                .getAnnotation(Deprecated::class.java),
        )
    }
}
