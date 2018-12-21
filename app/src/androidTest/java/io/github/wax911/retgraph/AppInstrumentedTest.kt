package io.github.wax911.retgraph

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import io.github.wax911.library.annotation.GraphQuery
import io.github.wax911.library.model.request.QueryContainerBuilder
import io.github.wax911.retgraph.api.WebFactory
import io.github.wax911.retgraph.api.retro.request.IndexModel
import io.github.wax911.retgraph.model.container.TrendingFeed
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class AppInstrumentedTest {

    private val appContext by lazy { InstrumentationRegistry.getInstrumentation().context }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("io.github.wax911.retgraph.test", appContext.packageName)
    }

    @Test
    fun mimeTypeIsJson() {
        val service = WebFactory.getInstance(appContext)
                .createService(IndexModel::class.java)
        assertNotNull(service)

        val queryBuilder = QueryContainerBuilder()
                .putVariable("type", TrendingFeed.HOT)
                .putVariable("limit", 5)
                .putVariable("offset", 1)
        assertNotNull(queryBuilder.build())

        val requestBody = service
                .getTrending(queryBuilder)
                .request().body()
        assertNotNull(requestBody)

        assertEquals("application", requestBody?.contentType()?.type())
        assertEquals("json", requestBody?.contentType()?.subtype())
    }

    @Test
    fun annotationArePresent() {
        val entries = IndexModel::getRepoEntries
        val trending = IndexModel::getTrending

        val entriesAnnotations = entries.annotations.filterIsInstance(GraphQuery::class.java)
        assertFalse(entriesAnnotations.isNullOrEmpty())
        assertEquals(1, entriesAnnotations.size)
        assertEquals("RepoEntries", entriesAnnotations.first().value)

        val trendingAnnotations = trending.annotations.filterIsInstance(GraphQuery::class.java)
        assertFalse(trendingAnnotations.isNullOrEmpty())
        assertEquals(1, trendingAnnotations.size)
        assertEquals("Trending", trendingAnnotations.first().value)
    }
}
