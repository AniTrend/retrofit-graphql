package co.anitrend.retrofit.graphql.data.user.converters

import co.anitrend.arch.data.converter.SupportConverter
import co.anitrend.arch.data.mapper.contract.ISupportMapperHelper
import co.anitrend.retrofit.graphql.data.arch.common.SampleMapper
import co.anitrend.retrofit.graphql.data.user.entity.UserEntity
import co.anitrend.retrofit.graphql.data.user.model.node.UserNode
import co.anitrend.retrofit.graphql.domain.entities.user.User

internal class UserEntityConverter(
    override val fromType: (UserEntity) -> User = { from().transform(it) },
    override val toType: (User) -> UserEntity = { to().transform(it) }
) : SupportConverter<UserEntity, User>() {
    companion object : SampleMapper<UserEntity, User>() {
        override fun from() =
            object : ISupportMapperHelper<UserEntity, User> {
                /**
                 * Transforms the the [source] to the target type
                 */
                override fun transform(source: UserEntity) =
                    User(
                        id = source.id,
                        avatar = source.avatarUrl,
                        bio = source.bio.orEmpty(),
                        status = User.Status(
                            emoji = source.statusEmoji.orEmpty(),
                            message = source.statusMessage.orEmpty()
                        ),
                        username = source.username
                    )
            }

        override fun to(): ISupportMapperHelper<User, UserEntity> {
            throw Throwable("Not yet implemented")
        }
    }
}

internal class UserModelConverter(
    override val fromType: (UserEntity) -> UserNode = { from().transform(it) },
    override val toType: (UserNode) -> UserEntity = { to().transform(it) }
): SupportConverter<UserEntity, UserNode>() {
    companion object : SampleMapper<UserEntity, UserNode>() {
        override fun from(): ISupportMapperHelper<UserEntity, UserNode> {
            throw Throwable("Not yet implemented")
        }

        override fun to() = 
            object : ISupportMapperHelper<UserNode, UserEntity> {
                /**
                 * Transforms the the [source] to the target type
                 */
                override fun transform(source: UserNode) =
                    UserEntity(
                        id = source.id,
                        username = source.login,
                        bio = source.bio,
                        avatarUrl = source.avatarUrl,
                        statusEmoji = source.status?.emoji,
                        statusMessage = source.status?.message
                    )
            }
    }
}