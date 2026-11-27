package playerpose.client;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import playerpose.common.CommonProxy;

/** クライアント側の初期化（シングルプレイ用にサーバー側の処理も親クラスで登録される）。 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        PoseConfig.init(event.getSuggestedConfigurationFile());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientEventHandler.registerKeys();
        ClientEventHandler clientEvents = new ClientEventHandler();
        FMLCommonHandler.instance().bus().register(clientEvents); // ClientTickEvent
        MinecraftForge.EVENT_BUS.register(clientEvents);          // WorldEvent.Unload
        MinecraftForge.EVENT_BUS.register(new PoseHud());        // RenderGameOverlayEvent（切り替え時の名前表示）
        PoseRenderHandler renderHandler = new PoseRenderHandler();
        MinecraftForge.EVENT_BUS.register(renderHandler);          // RenderPlayerEvent
        FMLCommonHandler.instance().bus().register(renderHandler); // RenderTickEvent
        FMLCommonHandler.instance().bus().register(new CameraFollowHandler()); // RenderTickEvent（視点追従）
        ClientCommandHandler.instance.registerCommand(new CommandPose());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MpmModelInstaller.install();
    }
}
