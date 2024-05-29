package co.anitrend.retrofit.graphql.data.user.converters

import co.anitrend.arch.data.converter.SupportConverter
import co.anitrend.retrofit.graphql.data.user.entity.UserEntity
import co.anitrend.retrofit.graphql.data.user.model.node.UserNode
import co.anitrend.retrofit.graphql.domain.entities.user.User

internal class UserEntityConverter(
    override val fromType: (UserEntity) -> User = {
        User(
            id = it.id,
            avatar = it.avatarUrl,
            bio = it.bio.orEmpty(),
            status = User.Status(
                emoji = it.statusEmoji.orEmpty(),
                message = it.statusMessage.orEmpty()
            ),
            username = it.username
        )
    },
    override val toType: (User) -> UserEntity = { throw NotImplementedError() }
) : SupportConverter<UserEntity, User>()

internal class UserModelConverter(
    override val fromType: (UserEntity) -> UserNode = { throw NotImplementedError() },
    override val toType: (UserNode) -> UserEntity = {
        UserEntity(
            id = it.id,
            username = it.login,
            bio = it.bio,
            avatarUrl = it.avatarUrl,
            statusEmoji = it.status?.emoji,
            statusMessage = it.status?.message
        )
    }
): SupportConverter<UserEntity, UserNode>()