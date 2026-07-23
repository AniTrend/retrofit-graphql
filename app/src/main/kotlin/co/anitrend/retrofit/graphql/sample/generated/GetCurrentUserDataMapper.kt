package co.anitrend.retrofit.graphql.sample.generated

/**
 * Demonstrates mapping a generated response DTO to a domain model.
 *
 * Generated types (like [GetCurrentUserData]) are transport DTOs designed
 * for the Retrofit/network boundary. Map them to your own domain models
 * rather than exposing generated classes throughout your application.
 *
 * Usage pattern:
 * ```kotlin
 * val response: GraphContainer<GetCurrentUserData> = remoteSource.getCurrentUser(request)
 * val user = response.data?.let { GetCurrentUserDataMapper.toDomain(it) }
 * ```
 *
 * Generated response types use kotlinx serialization. Configure your
 * Retrofit instance with a kotlinx.serialization converter and ensure
 * the generated models are properly deserialized. For polymorphic
 * responses (interfaces/unions), configure:
 * ```kotlin
 * val json = Json { classDiscriminator = "__typename" }
 * ```
 */
object GetCurrentUserDataMapper {

    /**
     * Maps the generated [GetCurrentUserData] DTO to a simple domain type.
     * Replace [DomainUser] with your own domain/entity class.
     */
    fun toDomain(data: GetCurrentUserData): DomainUser {
        val viewer = data.viewer
        return DomainUser(
            id = viewer.id,
            login = viewer.login,
            name = viewer.bio ?: viewer.login,
            avatarUrl = viewer.avatarUrl,
        )
    }

    /**
     * Simple domain model example. Replace with your actual domain class
     * (e.g., Room entity, view state, or presentation model).
     */
    data class DomainUser(
        val id: String,
        val login: String,
        val name: String,
        val avatarUrl: String,
    )
}
