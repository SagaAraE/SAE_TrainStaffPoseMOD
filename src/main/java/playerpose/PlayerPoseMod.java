package playerpose;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import playerpose.common.CommandPoseMode;
import playerpose.common.CommonProxy;

/**
 * PlayerPose 本体。
 * More Player Models 1.7.10b が必須（MPMより後に読み込む）。
 */
@Mod(modid = PlayerPoseMod.MODID, name = PlayerPoseMod.NAME, version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.7.10]", dependencies = "required-after:moreplayermodels")
public class PlayerPoseMod {

    public static final String MODID = "playerpose";
    public static final String NAME = "SAE TrainStaffPose";

    @SidedProxy(clientSide = "playerpose.client.ClientProxy", serverSide = "playerpose.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    /** サーバー（シングルプレイの内部サーバーを含む）の起動時に /posemode を登録する。 */
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPoseMode());
    }
}
