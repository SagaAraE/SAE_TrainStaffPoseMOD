package playerpose.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/** 通信チャンネル。パケットは「ポーズ要求」「ポーズ同期」「使用権限の通知」の3種類。 */
public final class PoseNetwork {

    public static SimpleNetworkWrapper CHANNEL;

    private PoseNetwork() {
    }

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("PlayerPose");
        CHANNEL.registerMessage(MsgPoseRequest.Handler.class, MsgPoseRequest.class, 0, Side.SERVER);
        CHANNEL.registerMessage(MsgPoseSync.Handler.class, MsgPoseSync.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(MsgPermission.Handler.class, MsgPermission.class, 2, Side.CLIENT);
    }
}
