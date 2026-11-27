package playerpose.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import playerpose.common.PoseServerHandler;

/** クライアント → サーバー：「このポーズを、この調整値で取りたい」（pose 0 = 解除）。 */
public class MsgPoseRequest implements IMessage {

    public int pose;
    public float sideShift;
    public float height;

    public MsgPoseRequest() {
    }

    public MsgPoseRequest(int pose, float sideShift, float height) {
        this.pose = pose;
        this.sideShift = sideShift;
        this.height = height;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pose = buf.readByte();
        sideShift = buf.readFloat();
        height = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(pose);
        buf.writeFloat(sideShift);
        buf.writeFloat(height);
    }

    public static class Handler implements IMessageHandler<MsgPoseRequest, IMessage> {
        @Override
        public IMessage onMessage(MsgPoseRequest message, MessageContext ctx) {
            // 値の検証・範囲制限は PoseServerHandler 側で行う
            PoseServerHandler.request(ctx.getServerHandler().playerEntity, message.pose, message.sideShift, message.height);
            return null;
        }
    }
}
