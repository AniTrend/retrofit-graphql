package co.anitrend.retrofit.graphql.core.settings

import android.content.Context
import co.anitrend.arch.extension.preference.BooleanPreference
import co.anitrend.arch.extension.preference.IntPreference
import co.anitrend.arch.extension.preference.StringPreference
import co.anitrend.arch.extension.preference.SupportSettings
import co.anitrend.retrofit.graphql.data.authentication.settings.IAuthenticationSettings
import co.anitrend.retrofit.graphql.sample.R

class Settings(context: Context) : SupportSettings(context), IAuthenticationSettings {

    override var authenticatedUserId by StringPreference(
        R.string.setting_authenticated_user_id,
        IAuthenticationSettings.INVALID_USER_ID,
        context.resources
    )

    override var isNewInstallation by BooleanPreference(
        R.string.setting_is_new_installation,
        true,
        context.resources
    )

    override var versionCode by IntPreference(
        R.string.setting_version_code,
        1,
        context.resources
    )

    companion object {
        val BINDINGS = arrayOf(
            Settings::class, SupportSettings::class, IAuthenticationSettings::class
        )
    }
}