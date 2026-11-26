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
        FMLCommonHandler.instance().bus().register(clientEvents); // ClientTickEvent / KeyInputEvent
        MinecraftForge.EVENT_BUS.register(clientEvents);          // WorldEvent.Unload
        PoseRenderHandler renderHandler = new PoseRenderHandler();
        MinecraftForge.EVENT_BUS.register(renderHandler);          // RenderPlayerEvent / Specials
        FMLCommonHandler.instance().bus().register(renderHandler); // RenderTickEvent
        ClientCommandHandler.instance.registerCommand(new CommandPose());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        MpmModelInstaller.install();
    }
}
