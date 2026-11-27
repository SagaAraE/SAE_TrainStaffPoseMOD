package playerpose.client;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import playerpose.common.ClientPermission;
import playerpose.common.Poses;

import java.util.List;

/**
 * クライアント側だけで処理するコマンド。
 *  /pose         … 選択画面を開く
 *  /pose 1〜9    … そのポーズを取る
 *  /pose off (0) … ポーズ解除
 *  /pose reload  … 設定ファイルを読み直す（角度調整用）
 * 使用権限が無いときは、起立以外のポーズはメッセージだけ出す（起立・解除・reload は使える）。
 */
public class CommandPose extends CommandBase {

    @Override
    public String getCommandName() {
        return "pose";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "playerpose.cmd.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ClientEventHandler.requestOpenWheel();
            return;
        }
        String arg = args[0].toLowerCase();
        if ("off".equals(arg) || "0".equals(arg)) {
            send(Poses.NONE);
            return;
        }
        if ("reload".equals(arg)) {
            PoseConfig.reload();
            sender.addChatMessage(new ChatComponentTranslation("playerpose.msg.reloaded"));
            return;
        }
        int pose;
        try {
            pose = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if (!Poses.isValid(pose)) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if (!ClientPermission.canSelect(pose)) {
            sender.addChatMessage(new ChatComponentTranslation("playerpose.msg.noPermission"));
            return;
        }
        send(pose);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "1", "2", "3", "4", "5", "6", "7", "8", "9", "off", "reload");
        }
        return null;
    }

    private static void send(int pose) {
        PoseConfig.selectPose(pose);
    }
}
