package io.github.wax911.library.annotation.processor

import android.content.res.AssetManager
import android.util.Log
import io.github.wax911.library.annotation.GraphQuery
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Created by max on 2018/03/12.
 * GraphQL annotation processor
 */
class GraphProcessor private constructor(assetManager: AssetManager?) {

    private val _graphFiles: MutableMap<String, String> by lazy {
        HashMap<String, String>()
    }

    val graphFiles: Map<String, String>
        get() {
            return Collections.unmodifiableMap(_graphFiles)
        }

    init {
        synchronized(lock) {
            Log.d("GraphProcessor", Thread.currentThread().name + ": has obtained a synchronized lock on the object")
            if (_graphFiles.isEmpty()) {
                Log.d("GraphProcessor", Thread.currentThread().name + ": is initializing query files")
                createGraphQLMap(defaultDirectory, assetManager)
                Log.d("GraphProcessor", Thread.currentThread().name + ": has completed initializing all files")
                Log.d("GraphProcessor", Thread.currentThread().name + ": Total count of graphFiles -> size: " + _graphFiles.size)
            } else
                Log.d("GraphProcessor", Thread.currentThread().name + ": skipped initialization of graphFiles -> size: " + _graphFiles.size)
        }
    }

    @Synchronized
    fun getQuery(annotations: Array<Annotation>): String? {
        var graphQuery: GraphQuery? = null

        for (annotation in annotations)
            if (annotation is GraphQuery) {
                graphQuery = annotation
                break
            }

        if (graphQuery != null) {
            val fileName = String.format("%s%s", graphQuery.value, defaultExtension)
            Log.d("GraphProcessor", fileName)
            if (_graphFiles.containsKey(fileName))
                return _graphFiles[fileName]
            Log.e(this.toString(), String.format("The request query %s could not be found!", graphQuery.value))
            Log.e(this.toString(), String.format("Current size of graphFiles -> size: %d", _graphFiles.size))
        }
        return null
    }

    @Synchronized
    private fun createGraphQLMap(path: String, assetManager: AssetManager?) {
        try {
            assetManager?.apply {
                val paths = list(path)
                paths?.also {
                    for (item in it) {
                        val absolute = "$path/$item"
                        if (item.endsWith(defaultExtension))
                            _graphFiles[item] = getFileContents(open(absolute))
                        else
                            createGraphQLMap(absolute, this)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun getFileContents(inputStream: InputStream): String {
        val queryBuffer = StringBuilder()
        try {
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)

            var line: String?
            do {
                line = bufferedReader.readLine()
                line?.apply {
                    queryBuffer.append(this)
                }
            } while (line != null)

            inputStreamReader.close()
            bufferedReader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return queryBuffer.toString()
    }

    companion object {

        @Volatile
        private var instance: GraphProcessor? = null
        private val lock = Any()

        private const val defaultExtension = ".graphql"
        private const val defaultDirectory = "graphql"

        fun getInstance(assetManager: AssetManager?): GraphProcessor {
            val singleton = instance
            if (singleton != null)
                return singleton

            return synchronized(lock) {
                val init = instance
                if (init != null)
                    init
                else {
                    val created = GraphProcessor(assetManager)
                    instance = created
                    created
                }
            }
        }
    }
}