package fabricmc;

import Launcher.*;
import Launcher.base.LaunchListener;
import UIL.Lang;
import UIL.base.IImage;
import Utils.json.Json;
import Utils.json.JsonDict;
import Utils.json.JsonElement;
import Utils.web.WebClient;
import Utils.web.WebResponse;
import minecraft.IMinecraftVersion;
import minecraft.MinecraftList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

public class FabricMCMarket extends Market {
    public final FabricMCSupport plugin;

    public final MinecraftList versions, versionsStables, versionsUnstable, loaders;

    public final WebClient client = new WebClient();

    private TaskGroup group = null;

    public FabricMCMarket(final FabricMCSupport plugin, final String id, final IImage icon, final boolean smooth) {
        super(id, icon);
        this.plugin = plugin;
        versions = new MinecraftList(Lang.get("fabricmc.versions.all"), getIcon(), smooth);
        versionsStables = new MinecraftList(Lang.get("fabricmc.versions.stables"), getIcon(), smooth);
        versionsUnstable = new MinecraftList(Lang.get("fabricmc.versions.notStables"), getIcon(), smooth);
        loaders = new MinecraftList(Lang.get("fabricmc.loaders"), getIcon(), smooth);
        client.allowRedirect = true;
        client.headers.put("User-Agent", "FlashLauncher/FabricMCSupport/" + plugin.getVersion() + " (mcflashlauncher@gmail.com)");
    }

    private class VerSel extends MinecraftList {
        public final String version;

        private final Object l = new Object();
        private ArrayList<IMinecraftVersion> versions = null;

        public VerSel(final String version) {
            super(version, plugin.getIcon(), false);
            this.version = version;
        }

        private ArrayList<IMinecraftVersion> getVersions() {
            synchronized (l) {
                if (versions == null) {
                    versions = new ArrayList<>();
                    for (final IMinecraftVersion ver : loaders)
                        versions.add(new IMinecraftVersion() {
                            @Override
                            public String getID() {
                                return "fabric-loader-" + ver.getID() + "-" + version;
                            }

                            @Override public LaunchListener init(RunProc configuration) { return null; }

                            @Override public String toString() { return ver.toString(); }
                        });
                }
                return versions;
            }
        }

        @Override public boolean isEmpty() { return loaders.isEmpty(); }
        @Override public int size() { return loaders.size(); }
        @Override public void add(IMinecraftVersion ver) {}
        @Override public void addAll(Collection<? extends IMinecraftVersion> versions) {}
        @Override public IMinecraftVersion get(String id) { return null; }
        @Override public void remove(IMinecraftVersion ver) {}
        @Override public void removeAll(Collection<? extends IMinecraftVersion> versions) {}
        @Override public String toString() { return version; }
        @Override public Iterator<IMinecraftVersion> iterator() { return getVersions().iterator(); }
        @Override public void forEach(Consumer<? super IMinecraftVersion> action) { getVersions().forEach(action); }
        @Override public Spliterator<IMinecraftVersion> spliterator() { return getVersions().spliterator(); }
    }

    @Override
    public void checkForUpdates(final Meta... items) {
        group = new TaskGroupAutoProgress();
        group.addTask(new Task() {
            @Override
            public void run() throws Throwable {
                try {
                    // https://meta.fabricmc.net/v2/versions/game
                    // https://meta.fabricmc.net/v2/versions/lodaer

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    WebResponse r = client.open("GET", URI.create("https://meta.fabricmc.net/v2/versions/loader"), os, true);
                    r.auto();
                    if (r.getResponseCode() != 200) {
                        System.out.println("[FabricMC] Loaders code: " + r.getResponseCode());
                        return;
                    }
                    for (final JsonElement e : Json.parse(new ByteArrayInputStream(os.toByteArray()), true, "UTF-8").getAsList())
                        if (e.isDict()) {
                            final JsonDict d = e.getAsDict();
                            loaders.add(new IMinecraftVersion() {
                                final String version = d.getAsString("version");

                                @Override
                                public String getID() {
                                    return version;
                                }

                                @Override
                                public LaunchListener init(RunProc configuration) {
                                    return null;
                                }

                                @Override public String toString() { return "Fabric Loader " + version; }
                            });
                        } else
                            System.out.println("[FabricMC] The version is not dictionary: " + e);

                    os = new ByteArrayOutputStream();
                    r = client.open("GET", URI.create("https://meta.fabricmc.net/v2/versions/game"), os, true);
                    r.auto();
                    if (r.getResponseCode() != 200) {
                        System.out.println("[FabricMC] Versions code: " + r.getResponseCode());
                        return;
                    }
                    for (final JsonElement e : Json.parse(new ByteArrayInputStream(os.toByteArray()), true, "UTF-8").getAsList())
                        if (e.isDict()) {
                            final JsonDict d = e.getAsDict();
                            final VerSel sel = new VerSel(d.getAsString("version"));
                            versions.add(sel);
                            if (d.getAsBool("stable"))
                                versionsStables.add(sel);
                            else
                                versionsUnstable.add(sel);
                        } else
                            System.out.println("[FabricMC] The version is not dictionary: " + e);

                    plugin.mcPlugin.market.waitFinish();
                    if (plugin.mcPlugin.market.isSuccess())
                        plugin.mcPlugin.addList(versions, versionsStables, versionsUnstable);
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        plugin.getContext().addTaskGroup(group);
    }

    @Override public Meta[] find(final String query) { return new Meta[0]; }
}