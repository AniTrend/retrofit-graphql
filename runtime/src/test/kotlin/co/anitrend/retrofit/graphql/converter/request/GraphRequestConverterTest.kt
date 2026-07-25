package co.anitrend.retrofit.graphql.converter.request

import co.anitrend.retrofit.graphql.annotation.GraphQuery
import co.anitrend.retrofit.graphql.annotation.processor.contract.AbstractGraphProcessor
import co.anitrend.retrofit.graphql.annotation.processor.fragment.FragmentPatcher
import co.anitrend.retrofit.graphql.converter.GraphConverter
import co.anitrend.retrofit.graphql.logger.contract.ILogger
import co.anitrend.retrofit.graphql.logger.core.AbstractLogger
import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class GraphRequestConverterTest {
    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val json: GraphQLJson = GsonGraphQLJson(gson)

    @Test
    fun `registry hit does not require processor fallback`() {
        val converter =
            GraphRequestConverter(
                methodAnnotations = methodAnnotations("RegisteredOperation"),
                graphProcessor = ThrowingGraphProcessor(),
                json = json,
                registry = MapRegistry(mapOf("RegisteredOperation" to "query RegisteredOperation { viewer { login } }")),
            )

        val requestBody = converter.convert(QueryContainerBuilder())

        assertEquals(
            "query RegisteredOperation { viewer { login } }",
            requestJson(requestBody).get("query").asString,
        )
    }

    @Test
    fun `registry miss still falls back to processor lookup`() {
        val processor = TrackingGraphProcessor("query MissingFromRegistry { repository { id } }")
        val converter =
            GraphRequestConverter(
                methodAnnotations = methodAnnotations("MissingFromRegistry"),
                graphProcessor = processor,
                json = json,
                registry = MapRegistry(emptyMap()),
            )

        val requestBody = converter.convert(QueryContainerBuilder())

        assertEquals(1, processor.getQueryCalls)
        assertEquals(
            "query MissingFromRegistry { repository { id } }",
            requestJson(requestBody).get("query").asString,
        )
    }

    @Test
    fun `query container builder remains functional with kotlinx backend`() {
        val processor = TrackingGraphProcessor("query LegacyOperation { viewer { login } }")
        val converter =
            GraphRequestConverter(
                methodAnnotations = methodAnnotations("MissingFromRegistry"),
                graphProcessor = processor,
                json = ThrowingJson(),
                registry = MapRegistry(emptyMap()),
            )

        val requestBody = converter.convert(QueryContainerBuilder())

        assertEquals(1, processor.getQueryCalls)
        assertEquals(
            "query LegacyOperation { viewer { login } }",
            requestJson(requestBody).get("query").asString,
        )
    }

    @Test
    fun `registry only converter throws for missing operation`() {
        val converter = GraphConverter.create(MapRegistry(emptyMap()))
        val requestConverter =
            converter.requestBodyConverter(
                QueryContainerBuilder::class.java,
                emptyArray(),
                methodAnnotations("UnregisteredOperation"),
                Retrofit.Builder().baseUrl("https://example.com/").build(),
            ) as retrofit2.Converter<Any, RequestBody>

        val error =
            assertThrows(IllegalStateException::class.java) {
                requestConverter.convert(QueryContainerBuilder())
            }

        assertTrue(error.message?.contains("UnregisteredOperation") == true)
    }

    @Test
    fun `registry only converter keeps missing annotation behavior sensible`() {
        val converter = GraphConverter.create(MapRegistry(emptyMap()))
        val requestConverter =
            converter.requestBodyConverter(
                QueryContainerBuilder::class.java,
                emptyArray(),
                methodAnnotations("WithoutAnnotation"),
                Retrofit.Builder().baseUrl("https://example.com/").build(),
            ) as retrofit2.Converter<Any, RequestBody>

        val requestBody = requestConverter.convert(QueryContainerBuilder())
        val requestJson = requestJson(requireNotNull(requestBody))

        assertTrue(requestJson.get("query").isJsonNull)
    }

    private fun methodAnnotations(name: String): Array<Annotation> =
        TestService::class.java.getDeclaredMethod(name).annotations

    private fun requestJson(requestBody: RequestBody): JsonObject {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        return gson.fromJson(buffer.readUtf8(), JsonObject::class.java)
    }

    private interface TestService {
        @GraphQuery("RegisteredOperation")
        fun RegisteredOperation()

        @GraphQuery("MissingFromRegistry")
        fun MissingFromRegistry()

        @GraphQuery("UnregisteredOperation")
        fun UnregisteredOperation()

        fun WithoutAnnotation()
    }

    private class MapRegistry(
        private val documents: Map<String, String>,
    ) : GraphQLDocumentRegistry {
        override fun document(operationName: String): String? = documents[operationName]

        override fun hash(operationName: String): String? = null
    }

    private class ThrowingJson : GraphQLJson {
        override fun <T : Any> encode(
            value: T,
            type: java.lang.reflect.Type?,
        ): String {
            throw AssertionError("Legacy QueryContainerBuilder path should fall back to internal Gson serializer")
        }

        override fun <T : Any> decode(
            json: String,
            type: java.lang.reflect.Type,
        ): T {
            throw AssertionError("decode should not be used in GraphRequestConverter tests")
        }
    }

    private class ThrowingGraphProcessor : BaseTestGraphProcessor() {
        override fun getQuery(annotations: Array<out Annotation>): String? {
            throw AssertionError("Processor fallback should not be required")
        }
    }

    private class TrackingGraphProcessor(
        private val query: String,
    ) : BaseTestGraphProcessor() {
        var getQueryCalls: Int = 0
            private set

        override fun getQuery(annotations: Array<out Annotation>): String {
            getQueryCalls += 1
            return query
        }
    }

    private abstract class BaseTestGraphProcessor : AbstractGraphProcessor() {
        override val defaultExtension: String = ".graphql"
        override val defaultDirectory: String = "graphql"
        override val logger: AbstractLogger = TestLogger()
        override val fragmentPatcher: FragmentPatcher = FragmentPatcher(defaultExtension, logger = logger)
        override val graphFiles: Map<String, String> = emptyMap()

        override fun patchQueries() = Unit
    }

    private class TestLogger : AbstractLogger(ILogger.Level.NONE) {
        override fun log(
            level: ILogger.Level,
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit
    }
}
