package co.anitrend.retrofit.graphql.data.authentication.settings

import co.anitrend.arch.extension.settings.contract.AbstractSetting

interface IAuthenticationSettings {
    val authenticatedUserId: AbstractSetting<String>

    companion object  {
        const val INVALID_USER_ID: String = ""
    }
}