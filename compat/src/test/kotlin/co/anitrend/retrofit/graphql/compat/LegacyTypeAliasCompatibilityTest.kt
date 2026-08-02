package co.anitrend.retrofit.graphql.compat

import android.content.res.AssetManager
import co.anitrend.retrofit.graphql.annotation.processor.GraphProcessor
import co.anitrend.retrofit.graphql.annotation.processor.contract.AbstractGraphProcessor
import co.anitrend.retrofit.graphql.annotation.processor.fragment.FragmentAnalysis
import co.anitrend.retrofit.graphql.annotation.processor.fragment.FragmentPatcher
import co.anitrend.retrofit.graphql.annotation.processor.fragment.GraphRegexUtil
import co.anitrend.retrofit.graphql.annotation.processor.fragment.RegexFragmentAnalyzer
import co.anitrend.retrofit.graphql.annotation.processor.plugin.AssetManagerDiscoveryPlugin
import co.anitrend.retrofit.graphql.annotation.processor.plugin.contract.AbstractDiscoveryPlugin
import co.anitrend.retrofit.graphql.converter.GraphConverter
import co.anitrend.retrofit.graphql.converter.request.GraphRequestConverter
import co.anitrend.retrofit.graphql.converter.response.GraphResponseConverter
import co.anitrend.retrofit.graphql.model.EmptyGraphQLVariables
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import co.anitrend.retrofit.graphql.model.GraphQLResponseException
import co.anitrend.retrofit.graphql.model.attribute.GraphError
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.model.request.PersistedQuery
import co.anitrend.retrofit.graphql.model.request.PersistedQueryUrlParameterBuilder
import co.anitrend.retrofit.graphql.model.request.PersistedQueryUrlParameters
import co.anitrend.retrofit.graphql.model.request.QueryContainer
import co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder
import co.anitrend.retrofit.graphql.persisted.query.AutomaticPersistedQueryCalculator
import co.anitrend.retrofit.graphql.persisted.query.error.AutomaticPersistedQueryErrors
import co.anitrend.retrofit.graphql.serialization.gson.GsonGraphQLJson
import co.anitrend.retrofit.graphql.serialization.kotlinx.KotlinxGraphQLJson
import co.anitrend.retrofit.graphql.util.LogLevel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compile-time and runtime proof that every legacy `io.github.wax911.library.*`
 * type alias resolves exactly once to its `:compat` target.
 *
 * The aliases are `@Deprecated(WARNING)` with `ReplaceWith` expressions that
 * name the targets below; every reference in this file is compiled against
 * the alias FQCN and the assertion checks the runtime type is the compat
 * target class.
 */
@Suppress("DEPRECATION")
class LegacyTypeAliasCompatibilityTest {

    @Test
    fun `legacy model aliases resolve to compat targets`() {
        val request: io.github.wax911.library.model.GraphQLRequest<EmptyGraphQLVariables> =
            GraphQLRequest(query = "query Q { viewer { login } }", operationName = "Q")
        assertTrue(request is GraphQLRequest<*>)

        val container: io.github.wax911.library.model.body.GraphContainer<String> =
            GraphContainer(data = "data")
        assertTrue(container is GraphContainer<*>)

        val error: io.github.wax911.library.model.attribute.GraphError = GraphError(message = "boom")
        assertTrue(error is GraphError)

        val json: io.github.wax911.library.model.GraphQLJson = GsonGraphQLJson()
        assertTrue(json is co.anitrend.retrofit.graphql.model.GraphQLJson)

        val variables: io.github.wax911.library.model.GraphQLVariables = EmptyGraphQLVariables
        assertTrue(variables is co.anitrend.retrofit.graphql.model.GraphQLVariables)

        val sentinel: io.github.wax911.library.model.EmptyGraphQLVariables = EmptyGraphQLVariables
        assertEquals(EmptyGraphQLVariables, sentinel)

        val exception: io.github.wax911.library.model.GraphQLResponseException =
            GraphQLResponseException(errors = listOf(error))
        assertTrue(exception is GraphQLResponseException)

        val registry: io.github.wax911.library.model.GraphQLDocumentRegistry = MapRegistry
        assertTrue(registry is co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry)

        val operation: io.github.wax911.library.model.GraphQLOperation<EmptyGraphQLVariables> = NoVarOperation
        assertTrue(operation is co.anitrend.retrofit.graphql.model.GraphQLOperation<*>)

        val noVar: io.github.wax911.library.model.GraphQLNoVarOperation = NoVarOperation
        assertTrue(noVar is co.anitrend.retrofit.graphql.model.GraphQLNoVarOperation)
    }

    @Test
    fun `legacy request aliases resolve to compat targets`() {
        val queryContainer: io.github.wax911.library.model.request.QueryContainer =
            QueryContainerBuilder().build()
        assertTrue(queryContainer is QueryContainer)

        val builder: io.github.wax911.library.model.request.QueryContainerBuilder =
            QueryContainerBuilder()
        assertTrue(builder is QueryContainerBuilder)

        val persistedQuery: io.github.wax911.library.model.request.PersistedQuery =
            PersistedQuery(sha256Hash = "hash", version = 1)
        assertTrue(persistedQuery is PersistedQuery)

        val urlParameters: io.github.wax911.library.model.request.PersistedQueryUrlParameters =
            PersistedQueryUrlParameters(extensions = "{}", operationName = "Q", variables = "{}")
        assertTrue(urlParameters is PersistedQueryUrlParameters)

        val urlBuilder: io.github.wax911.library.model.request.PersistedQueryUrlParameterBuilder =
            PersistedQueryUrlParameterBuilder(gson = com.google.gson.Gson())
        assertTrue(urlBuilder is PersistedQueryUrlParameterBuilder)
    }

    @Test
    fun `legacy converter and serialization aliases resolve to compat targets`() {
        val converter: io.github.wax911.library.converter.GraphConverter =
            GraphConverter.create(MapRegistry)
        assertTrue(converter is GraphConverter)

        val requestConverter: io.github.wax911.library.converter.request.GraphRequestConverter =
            GraphRequestConverter(
                methodAnnotations = emptyArray(),
                graphProcessor = mockk<AbstractGraphProcessor>(relaxed = true),
                json = GsonGraphQLJson(),
            )
        assertTrue(requestConverter is GraphRequestConverter)

        val responseConverter: io.github.wax911.library.converter.response.GraphResponseConverter<Any> =
            GraphResponseConverter(type = String::class.java, json = GsonGraphQLJson())
        assertTrue(responseConverter is GraphResponseConverter<*>)

        val gsonJson: io.github.wax911.library.serialization.gson.GsonGraphQLJson = GsonGraphQLJson()
        assertTrue(gsonJson is GsonGraphQLJson)

        val kotlinxJson: io.github.wax911.library.serialization.kotlinx.KotlinxGraphQLJson =
            KotlinxGraphQLJson()
        assertTrue(kotlinxJson is KotlinxGraphQLJson)
    }

    @Test
    fun `legacy processor and logger aliases resolve to compat targets`() {
        val logLevel: io.github.wax911.library.util.LogLevel = LogLevel.ERROR
        assertTrue(logLevel is LogLevel)

        val defaultLogger: io.github.wax911.library.logger.DefaultGraphLogger =
            co.anitrend.retrofit.graphql.logger.DefaultGraphLogger()
        assertTrue(defaultLogger is co.anitrend.retrofit.graphql.logger.DefaultGraphLogger)

        val logger: io.github.wax911.library.logger.contract.ILogger = defaultLogger
        assertTrue(logger is co.anitrend.retrofit.graphql.logger.contract.ILogger)

        val abstractLogger: io.github.wax911.library.logger.core.AbstractLogger = defaultLogger
        assertTrue(abstractLogger is co.anitrend.retrofit.graphql.logger.core.AbstractLogger)

        val discoveryPlugin: io.github.wax911.library.annotation.processor.plugin.contract.AbstractDiscoveryPlugin<AssetManager> =
            AssetManagerDiscoveryPlugin(assetManager = mockk<AssetManager>(relaxed = true))
        assertTrue(discoveryPlugin is AbstractDiscoveryPlugin<*>)

        val assetPlugin: io.github.wax911.library.annotation.processor.plugin.AssetManagerDiscoveryPlugin =
            discoveryPlugin as io.github.wax911.library.annotation.processor.plugin.AssetManagerDiscoveryPlugin
        assertTrue(assetPlugin is AssetManagerDiscoveryPlugin)

        val processor: io.github.wax911.library.annotation.processor.GraphProcessor =
            GraphProcessor(discoveryPlugin = discoveryPlugin)
        assertTrue(processor is GraphProcessor)

        val contractProcessor: io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor =
            processor
        assertTrue(contractProcessor is AbstractGraphProcessor)

        val fragmentPatcher: io.github.wax911.library.annotation.processor.fragment.FragmentPatcher =
            FragmentPatcher(
                defaultExtension = ".graphql",
                logger = co.anitrend.retrofit.graphql.logger.DefaultGraphLogger(),
            )
        assertTrue(fragmentPatcher is FragmentPatcher)

        val fragmentAnalysis: io.github.wax911.library.annotation.processor.fragment.FragmentAnalysis =
            FragmentAnalysis(fragmentReference = "F", isDefined = true)
        assertTrue(fragmentAnalysis is FragmentAnalysis)

        val regexAnalyzer: io.github.wax911.library.annotation.processor.fragment.RegexFragmentAnalyzer =
            RegexFragmentAnalyzer()
        assertTrue(regexAnalyzer is RegexFragmentAnalyzer)

        val fragmentAnalyzer: io.github.wax911.library.annotation.processor.fragment.FragmentAnalyzer =
            regexAnalyzer
        assertTrue(fragmentAnalyzer is co.anitrend.retrofit.graphql.annotation.processor.fragment.FragmentAnalyzer)

        val regexUtil: io.github.wax911.library.annotation.processor.fragment.GraphRegexUtil = GraphRegexUtil
        assertTrue(regexUtil is GraphRegexUtil)

        val queryAnnotation: io.github.wax911.library.annotation.GraphQuery =
            co.anitrend.retrofit.graphql.annotation.GraphQuery("Q")
        assertTrue(queryAnnotation is co.anitrend.retrofit.graphql.annotation.GraphQuery)
    }

    @Test
    fun `legacy persisted query aliases resolve to compat targets`() {
        val calculator: io.github.wax911.library.persisted.query.AutomaticPersistedQueryCalculator =
            AutomaticPersistedQueryCalculator(
                processor = mockk<AbstractGraphProcessor>(relaxed = true),
            )
        assertTrue(calculator is AutomaticPersistedQueryCalculator)

        val contract: io.github.wax911.library.persisted.contract.IAutomaticPersistedQuery =
            calculator
        assertTrue(contract is co.anitrend.retrofit.graphql.persisted.contract.IAutomaticPersistedQuery)

        val errors: io.github.wax911.library.persisted.query.error.AutomaticPersistedQueryErrors =
            AutomaticPersistedQueryErrors
        assertEquals(AutomaticPersistedQueryErrors, errors)
    }

    private companion object {
        object MapRegistry : co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry {
            override fun document(operationName: String): String? = null
            override fun hash(operationName: String): String? = null
        }

        object NoVarOperation : co.anitrend.retrofit.graphql.model.GraphQLNoVarOperation {
            override val name: String = "Q"
            override val document: String = "query Q { viewer { login } }"
            override val sha256Hash: String = "hash"
        }
    }
}
