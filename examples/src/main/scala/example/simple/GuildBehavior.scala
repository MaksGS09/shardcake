package example.simple

import com.devsisters.shardcake.Messenger.Replier
import com.devsisters.shardcake.{ EntityType, Sharding }
import zio.{ Dequeue, RIO, Ref }

import scala.util.{ Failure, Success, Try }

object GuildBehavior {
  sealed trait GuildMessage

  object GuildMessage {
    case class Join(userId: String, replier: Replier[Try[Set[String]]]) extends GuildMessage
    case class Leave(userId: String)                                    extends GuildMessage
  }

  object Guild extends EntityType[GuildMessage]("guild")

  def behavior(entityId: String, messages: Dequeue[GuildMessage]): RIO[Sharding, Nothing] =
    Ref
      .make(Set.empty[String])
      .flatMap(state => messages.take.flatMap(handleMessage(state, _)).forever)

  def handleMessage(state: Ref[Set[String]], message: GuildMessage): RIO[Sharding, Unit] =
    message match {
      case GuildMessage.Join(userId, replier) =>
        state.get.flatMap(members =>
          if (members.size >= 5)
            replier.reply(Failure(new Exception("Guild is already full!")))
          else
            state.updateAndGet(_ + userId).flatMap { newMembers =>
              replier.reply(Success(newMembers))
            }
        )
      case GuildMessage.Leave(userId)         =>
        state.update(_ - userId)
    }
}
