package fabricmc;

import Launcher.Plugin;
import Launcher.PluginContext;
import minecraft.MinecraftSupport;

public class FabricMCSupport extends Plugin {
    public final MinecraftSupport mcPlugin;
    public final FabricMCMarket market;

    public FabricMCSupport(final PluginContext context) {
        super(context);
        mcPlugin = (MinecraftSupport) context.getPlugin("minecraft-support");
        addMarket(market = new FabricMCMarket(this, "fabricmc-support.market", context.getIcon(), false));
    }
}