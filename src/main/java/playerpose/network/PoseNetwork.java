package playerpose.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/** 通信チャンネル。パケットは「ポーズ要求」と「ポーズ同期」の2種類だけ。 */
public final class PoseNetwork {

    public static SimpleNetworkWrapper CHANNEL;

    private PoseNetwork() {
    }

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("PlayerPose");
        CHANNEL.registerMessage(MsgPoseRequest.Handler.class, MsgPoseRequest.class, 0, Side.SERVER);
        CHANNEL.registerMessage(MsgPoseSync.Handler.class, MsgPoseSync.class, 1, Side.CLIENT);
    }
}
