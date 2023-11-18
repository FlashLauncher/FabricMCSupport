package fabricmc;

import UIL.base.IImage;
import minecraft.IMinecraftVersion;
import minecraft.MinecraftList;

public class FabricMCList extends MinecraftList {
    public final FabricMCSupport plugin;

    public FabricMCList(final FabricMCSupport plugin, final Object name, final IImage icon, final boolean smooth) {
        super(name, icon, smooth);
        this.plugin = plugin;
    }

    @Override
    public IMinecraftVersion get(String id) {
        if (!id.startsWith("fabric-loader-"))
            return super.get(id);
        id = id.substring(14);
        final int s = id.indexOf('-');
        return s == -1 ? null : plugin.market.get(id.substring(s + 1), id.substring(0, s));
    }
}
