package co.anitrend.retrofit.graphql.data.user.repository

import co.anitrend.arch.data.model.UserInterfaceState
import co.anitrend.arch.data.model.UserInterfaceState.Companion.create
import co.anitrend.arch.data.repository.SupportRepository
import co.anitrend.retrofit.graphql.data.user.source.contract.UserSource
import co.anitrend.retrofit.graphql.domain.entities.user.User
import co.anitrend.retrofit.graphql.domain.repositories.UserRepository

internal class UserRepositoryImpl(
    private val source: UserSource
) : SupportRepository(source), UserRepositoryContract {
    override fun getCurrentUser() =
        source.create(
            source()
        )
}

typealias UserRepositoryContract = UserRepository<UserInterfaceState<User>>