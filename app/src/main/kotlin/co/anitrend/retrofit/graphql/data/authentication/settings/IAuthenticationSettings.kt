package co.anitrend.retrofit.graphql.data.authentication.settings

interface IAuthenticationSettings {
    var authenticatedUserId: String

    companion object  {
        const val INVALID_USER_ID: String = ""
    }
}