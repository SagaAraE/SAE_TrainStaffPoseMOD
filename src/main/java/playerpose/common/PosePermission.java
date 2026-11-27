package playerpose.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/**
 * ポーズ機能を使えるかどうかの判定（サーバー側）。
 *  ・シングルプレイ（LAN公開した世界のホストを含む）：常に使える
 *  ・それ以外：設定が「OPのみ」ならOP（権限レベルは問わない）だけ、「全員」なら誰でも使える
 *  ・ただし「7 起立」だけは、設定に関係なく誰でも使える
 */
public final class PosePermission {

    private PosePermission() {
    }

    /** そのポーズを取れるか（起立は誰でも、それ以外は canUse）。 */
    public static boolean canUsePose(EntityPlayer player, int pose) {
        return isOpenToAll(pose) || canUse(player);
    }

    /** 権限に関係なく誰でも使えるポーズか。 */
    public static boolean isOpenToAll(int pose) {
        return pose == Poses.STAND;
    }

    /** 全部のポーズを使えるか。 */
    public static boolean canUse(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return false;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return false;
        }
        if (isSingleplayerHost(server, player)) {
            return true;
        }
        return !ServerConfig.isOpOnly() || isOp(server, (EntityPlayerMP) player);
    }

    /** OP（ops.json に入っている）か。権限レベルは問わない。 */
    public static boolean isOp(MinecraftServer server, EntityPlayerMP player) {
        return server.getConfigurationManager().func_152596_g(player.getGameProfile());
    }

    /** シングルプレイの世界を遊んでいる本人（LAN公開時のホスト）か。 */
    public static boolean isSingleplayerHost(MinecraftServer server, EntityPlayer player) {
        return !server.isDedicatedServer() && player.getCommandSenderName().equals(server.getServerOwner());
    }
}
