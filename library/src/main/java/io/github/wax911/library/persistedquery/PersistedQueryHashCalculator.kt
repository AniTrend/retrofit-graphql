package io.github.wax911.library.persistedquery

import android.content.Context
import android.util.Log
import io.github.wax911.library.annotation.processor.GraphProcessor
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class PersistedQueryHashCalculator constructor(context: Context?) {

    private val apqHashes: MutableMap<String, String> by lazy {
        HashMap<String, String>()
    }

    private val graphProcessor: GraphProcessor by lazy {
        GraphProcessor.getInstance(context?.assets)
    }

    fun getOrCreateAPQHash(queryName: String): String? {
        val fileKey = "$queryName$defaultExtension"
        return if(apqHashes.containsKey(fileKey)){
            apqHashes[fileKey]
        }
        else{
            createAndStoreHash(queryName, fileKey)
        }
    }

    private fun createAndStoreHash(queryName: String, fileKey: String): String? {
        Log.d(this.toString(), "Creating hash for $queryName")
        return if (graphProcessor.graphFiles.containsKey(fileKey)) {
            val hashOfQuery = hashOfQuery(graphProcessor.graphFiles.getValue(fileKey))
            Log.d(this.toString(), "Created ")
            apqHashes[fileKey] = hashOfQuery
            hashOfQuery
        } else {
            Log.e(this.toString(), "The request query $fileKey could not be found!")
            Log.e(this.toString(), "Current size of graphFiles -> size: ${graphProcessor.graphFiles.size}")
            null
        }
    }


    private fun hashOfQuery(query: String): String {
        val md = MessageDigest.getInstance(sha256Algorithm)
        md.update(query.toByteArray())
        val digest = md.digest()
        return String.format("%064x", BigInteger(1, digest))
    }

    companion object {
        private const val sha256Algorithm = "SHA-256"
        private const val defaultExtension = ".graphql"
    }
}