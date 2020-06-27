package io.github.wax911.library.persisted.contract

interface IAutomaticPersistedQuery {
    /**
     * Calculates the automated persisted query hash for a given [queryName]
     *
     * @return Hash or null if a query with the name [queryName] could not be found
     */
    fun getOrCreateAPQHash(queryName: String): String?
}