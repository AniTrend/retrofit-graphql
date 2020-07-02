package co.anitrend.retrofit.graphql.sample.presenter

import android.content.Context
import co.anitrend.arch.core.presenter.SupportPresenter
import co.anitrend.arch.ui.view.widget.model.StateLayoutConfig
import co.anitrend.retrofit.graphql.core.extension.using
import co.anitrend.retrofit.graphql.core.settings.Settings
import co.anitrend.retrofit.graphql.domain.entities.user.User
import co.anitrend.retrofit.graphql.sample.databinding.NavHeaderMainBinding
import co.anitrend.retrofit.graphql.sample.extensions.emojify
import coil.transform.CircleCropTransformation
import io.wax911.emojify.parser.parseToUnicode

class MainPresenter(
    context: Context,
    settings: Settings,
    private val stateLayoutConfig: StateLayoutConfig
) : SupportPresenter<Settings>(context, settings) {

    fun configureNavigationHeader(binding: NavHeaderMainBinding) {
        binding.navStateLayout.stateConfigFlow.value = stateLayoutConfig
    }

    fun updateNavigationHeaderView(user: User, binding: NavHeaderMainBinding) {
        val emojiManager = context.emojify()
        binding.navAvatar.using(user.avatar, null, CircleCropTransformation())
        binding.navUserName.text = user.username
        binding.navUserBio.text = user.bio
        binding.navUserStatus.text = emojiManager.parseToUnicode(
            "${user.status.emoji} ${user.status.message}"
        )
    }
}