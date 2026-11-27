package playerpose.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import playerpose.common.ClientPermission;

/** サーバー → クライアント：「あなたはポーズ機能を使える / 使えない」。 */
public class MsgPermission implements IMessage {

    public boolean allowed;

    public MsgPermission() {
    }

    public MsgPermission(boolean allowed) {
        this.allowed = allowed;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        allowed = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(allowed);
    }

    public static class Handler implements IMessageHandler<MsgPermission, IMessage> {
        @Override
        public IMessage onMessage(MsgPermission message, MessageContext ctx) {
            ClientPermission.allowed = message.allowed;
            return null;
        }
    }
}
