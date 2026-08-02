package co.anitrend.retrofit.graphql.converter

import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import co.anitrend.retrofit.graphql.model.GraphQLJson
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Retrofit

class GraphConverterTest {

    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val json: GraphQLJson = GsonGraphQLJson(gson)

    private class MapRegistry(
        private val documents: Map<String, String>,
    ) : GraphQLDocumentRegistry {
        override fun document(operationName: String): String? = documents[operationName]
        override fun hash(operationName: String): String? = null
    }

    // ---- Factory method tests ----------------------------------------------

    @Test
    fun `create with registry only uses default json`() {
        val registry = MapRegistry(emptyMap())
        val converter = GraphConverter.create(registry)
        assertNotNull(converter)
    }

    @Test
    fun `create with Gson and registry is source compatible`() {
        val registry = MapRegistry(emptyMap())
        val converter = GraphConverter.create(gson, registry)
        assertNotNull(converter)
    }

    @Test
    fun `create with GraphQLJson and registry works`() {
        val registry = MapRegistry(emptyMap())
        val converter = GraphConverter.create(json, registry)
        assertNotNull(converter)
    }

    @Test
    fun `converter can produce response converter`() {
        val registry = MapRegistry(emptyMap())
        val converter = GraphConverter.create(json, registry)
        val retrofit = Retrofit.Builder().baseUrl("https://example.com/").build()

        val responseConverter = converter.responseBodyConverter(
            Map::class.java,
            emptyArray(),
            retrofit,
        )
        assertNotNull(responseConverter)
    }

    @Test
    fun `converter can produce request converter`() {
        val registry = MapRegistry(
            mapOf("TestQuery" to "query TestQuery { viewer { login } }"),
        )
        val converter = GraphConverter.create(json, registry)
        val retrofit = Retrofit.Builder().baseUrl("https://example.com/").build()

        val requestConverter = converter.requestBodyConverter(
            co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder::class.java,
            emptyArray(),
            emptyArray(),
            retrofit,
        )
        assertNotNull(requestConverter)
    }

    @Test
    fun `all Gson factory overloads compile and create valid converters`() {
        // Verify existing Gson overloads still compile and work
        val registry = MapRegistry(emptyMap())
        val converter1 = GraphConverter.create(registry) // default Gson
        assertNotNull(converter1)

        val converter2 = GraphConverter.create(gson, registry) // custom Gson
        assertNotNull(converter2)

        val converter3 = GraphConverter.create(json, registry) // GraphQLJson
        assertNotNull(converter3)
    }
}
