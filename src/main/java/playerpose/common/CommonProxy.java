package playerpose.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import playerpose.network.PoseNetwork;

/** サーバー・クライアント共通の初期化。専用サーバーではこのクラスがそのまま使われる。 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ServerConfig.init(event.getModConfigurationDirectory());
        PoseNetwork.init();
    }

    public void init(FMLInitializationEvent event) {
        PoseServerHandler handler = new PoseServerHandler();
        FMLCommonHandler.instance().bus().register(handler); // PlayerTickEvent / ログアウト等
        MinecraftForge.EVENT_BUS.register(handler);          // LivingDeathEvent
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
