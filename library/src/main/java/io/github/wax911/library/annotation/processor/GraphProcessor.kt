package io.github.wax911.library.annotation.processor

import android.content.res.AssetManager
import android.util.Log
import io.github.wax911.library.annotation.GraphQuery
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

/**
 * Created by max on 2018/03/12.
 * GraphQL annotation processor
 */
class GraphProcessor private constructor(assetManager: AssetManager?) {

    private val graphFiles: MutableMap<String, String> by lazy {
        HashMap<String, String>()
    }

    private val apqHashes: MutableMap<String, String> by lazy {
        HashMap<String, String>()
    }

    init {
        synchronized(lock) {
            Log.d("GraphProcessor", Thread.currentThread().name + ": has obtained a synchronized lock on the object")
            if (graphFiles.isEmpty()) {
                Log.d("GraphProcessor", Thread.currentThread().name + ": is initializing query files")
                createGraphQLMap(defaultDirectory, assetManager)
                Log.d("GraphProcessor", Thread.currentThread().name + ": has completed initializing all files")
                Log.d("GraphProcessor", Thread.currentThread().name + ": Total count of graphFiles -> size: " + graphFiles.size)
            } else
                Log.d("GraphProcessor", Thread.currentThread().name + ": skipped initialization of graphFiles -> size: " + graphFiles.size)
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
            if (graphFiles.containsKey(fileName))
                return graphFiles[fileName]
            Log.e(this.toString(), String.format("The request query %s could not be found!", graphQuery.value))
            Log.e(this.toString(), String.format("Current size of graphFiles -> size: %d", graphFiles.size))
        }
        return null
    }

    fun getOrCreateAPQHash(queryName: String) : String? {
        val hash = apqHashes[queryName]
        if (hash == null) {
            if (graphFiles.containsKey(queryName)) {
                val hashOfQuery = hashOfQuery(graphFiles[queryName]!!)
                apqHashes[queryName] = hashOfQuery
                return hashOfQuery
            }
        }
        return hash
    }

    private fun hashOfQuery(query: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(query.toByteArray())
        val digest = md.digest()
        return String.format("%064x", BigInteger(1, digest))
    }

    @Synchronized
    private fun createGraphQLMap(path: String, assetManager: AssetManager?) {
        try {
            assetManager?.apply {
                val paths = list(path)
                paths?.also {
                    for (item in it) {
                        val absolute = "$path/$item"
                        if (!item.endsWith(defaultExtension))
                            createGraphQLMap(absolute, this)
                        else
                            graphFiles[item] = getFileContents(open(absolute))
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