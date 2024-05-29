package co.anitrend.retrofit.graphql.core.settings

import android.content.Context
import co.anitrend.arch.extension.preference.SupportPreference
import co.anitrend.arch.extension.settings.BooleanSetting
import co.anitrend.arch.extension.settings.IntSetting
import co.anitrend.arch.extension.settings.StringSetting
import co.anitrend.retrofit.graphql.data.authentication.settings.IAuthenticationSettings
import co.anitrend.retrofit.graphql.sample.R

class Settings(context: Context) : SupportPreference(context), IAuthenticationSettings {

    override val authenticatedUserId = StringSetting(
        key = R.string.setting_authenticated_user_id,
        default = IAuthenticationSettings.INVALID_USER_ID,
        resources = context.resources,
        preference = this,
    )

    override val isNewInstallation = BooleanSetting(
        key = R.string.setting_is_new_installation,
        default = true,
        resources = context.resources,
        preference = this,
    )

    override val versionCode = IntSetting(
        key = R.string.setting_version_code,
        default = 1,
        resources = context.resources,
        preference = this,
    )

    companion object {
        val BINDINGS = arrayOf(
            Settings::class, SupportPreference::class, IAuthenticationSettings::class
        )
    }
}