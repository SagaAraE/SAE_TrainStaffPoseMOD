package playerpose.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import playerpose.common.ClientPoseCache;

/** サーバー → クライアント：「エンティティID X のプレイヤーはポーズ N」（0 = 解除）。 */
public class MsgPoseSync implements IMessage {

    public int entityId;
    public int pose;

    public MsgPoseSync() {
    }

    public MsgPoseSync(int entityId, int pose) {
        this.entityId = entityId;
        this.pose = pose;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        pose = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(pose);
    }

    public static class Handler implements IMessageHandler<MsgPoseSync, IMessage> {
        @Override
        public IMessage onMessage(MsgPoseSync message, MessageContext ctx) {
            ClientPoseCache.put(message.entityId, message.pose);
            return null;
        }
    }
}
