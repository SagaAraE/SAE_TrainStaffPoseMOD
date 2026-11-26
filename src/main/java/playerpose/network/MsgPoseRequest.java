package playerpose.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import playerpose.common.PoseServerHandler;

/** クライアント → サーバー：「このポーズを取りたい」（0 = 解除）。 */
public class MsgPoseRequest implements IMessage {

    public int pose;

    public MsgPoseRequest() {
    }

    public MsgPoseRequest(int pose) {
        this.pose = pose;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pose = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(pose);
    }

    public static class Handler implements IMessageHandler<MsgPoseRequest, IMessage> {
        @Override
        public IMessage onMessage(MsgPoseRequest message, MessageContext ctx) {
            // 値の検証は PoseServerHandler 側で行う
            PoseServerHandler.request(ctx.getServerHandler().playerEntity, message.pose);
            return null;
        }
    }
}
