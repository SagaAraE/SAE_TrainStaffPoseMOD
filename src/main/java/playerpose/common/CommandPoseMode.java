package playerpose.common;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import java.util.List;

/**
 * サーバー側のコマンド。サーバー稼働中にポーズ機能の使用権限を切り替える。
 *  /posemode      … 今の設定を表示
 *  /posemode op   … OPのみ使用可能にする
 *  /posemode all  … 全員使用可能にする
 * 使えるのは OP（権限レベル不問）・シングルプレイのホスト・サーバーのコンソール。
 * 切り替えるとサーバーにいる全員のチャットに知らせ、設定ファイルにも保存する。
 */
public class CommandPoseMode extends CommandBase {

    @Override
    public String getCommandName() {
        return "posemode";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/posemode [op|all]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 実際の判定は canCommandSenderUseCommand で行う
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        if (sender instanceof MinecraftServer) {
            return true; // サーバーのコンソール
        }
        if (sender instanceof EntityPlayerMP) {
            MinecraftServer server = MinecraftServer.getServer();
            EntityPlayerMP player = (EntityPlayerMP) sender;
            return server != null
                    && (PosePermission.isSingleplayerHost(server, player) || PosePermission.isOp(server, player));
        }
        return false;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            reply(sender, ServerConfig.isOpOnly() ? "playerpose.msg.modeIsOp" : "playerpose.msg.modeIsAll",
                    ServerConfig.isOpOnly() ? "Pose mode: OP only" : "Pose mode: everyone");
            return;
        }
        String arg = args[0].toLowerCase();
        boolean opOnly;
        if ("op".equals(arg)) {
            opOnly = true;
        } else if ("all".equals(arg)) {
            opOnly = false;
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        ServerConfig.setOpOnly(opOnly);

        // 全員に知らせる（プレイヤーには各自の言語で、コンソールには英語で）
        MinecraftServer server = MinecraftServer.getServer();
        String key = opOnly ? "playerpose.msg.modeSetOp" : "playerpose.msg.modeSetAll";
        if (server != null) {
            server.getConfigurationManager().sendChatMsg(new ChatComponentTranslation(key));
            server.addChatMessage(new ChatComponentText(opOnly
                    ? "[PlayerPose] Pose mode set to OP only" : "[PlayerPose] Pose mode set to everyone"));
        }
        // 使えるかどうかを全員に送り直し、使えなくなった人のポーズは解除する
        PoseServerHandler.recheckAllPermissions();
    }

    private static void reply(ICommandSender sender, String key, String english) {
        if (sender instanceof EntityPlayerMP) {
            sender.addChatMessage(new ChatComponentTranslation(key));
        } else {
            sender.addChatMessage(new ChatComponentText("[PlayerPose] " + english));
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "op", "all");
        }
        return null;
    }
}
