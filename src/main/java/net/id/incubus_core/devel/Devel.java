package net.id.incubus_core.devel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.id.incubus_core.IncubusCore;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that provides some useful tools for developers.
 * <br>In order to get the devel to write todo files for you,
 * you must add {@code -Dincubus_core.devel.mods=mod_id} to the VM args.
 * <br>~ Jack
 * @author gudenau
 */
@SuppressWarnings("unused")
public final class Devel {
    private static final String[] MOD_IDS = IncubusDevel.DevelConfig.MODS;
    private static final boolean isDevel = FabricLoader.getInstance().isDevelopmentEnvironment();
    private static Path directory = IncubusDevel.DevelConfig.DIRECTORY;

    private static final Map<Identifier, FaultTracker> COMMON_FAULT_TRACKERS = new HashMap<>();
    @Environment(EnvType.CLIENT)
    private static final Map<Identifier, FaultTracker> CLIENT_FAULT_TRACKERS = new HashMap<>();
    private static final Set<Identifier> LOADED_FAULT_TRACKERS = IncubusDevel.DevelConfig.FAULT_TRACKERS;

    private Devel() {}

    /**
     * Registers a fault tracker. In order for this fault tracker to print
     * things to the devel logs, add {@code -Dincubus_core.devel.fault_trackers=mod_id}
     * to the VM arguments or don't specify {@code incubus_core.devel.fault_trackers},
     * which defaults to executing all fault trackers.
     */
    public static FaultTracker registerFaultTracker(Identifier trackerId, String trackerHeader) {
        var tracker = new FaultTracker(trackerHeader);
        COMMON_FAULT_TRACKERS.put(trackerId, tracker);
        return tracker;
    }

    /**
     * Registers a fault tracker. In order for this fault tracker to print
     * things to the devel logs, add {@code -Dincubus_core.devel.fault_trackers=mod_id}
     * to the VM arguments or don't specify {@code incubus_core.devel.fault_trackers},
     * which defaults to executing all fault trackers.
     */
    @Environment(EnvType.CLIENT)
    public static FaultTracker registerClientFaultTracker(Identifier trackerId, String trackerHeader) {
        var tracker = new FaultTracker(trackerHeader);
        CLIENT_FAULT_TRACKERS.put(trackerId, tracker);
        return tracker;
    }

    static void init() {
        if (MOD_IDS.length == 0) {
            IncubusCore.LOG.info("No devels loaded");
            return;
        }
        // Create devel directory if it doesn't exist
        if (!Files.isDirectory(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                IncubusCore.LOG.error("Failed to create \"{}\" directory. Using default directory...", directory);
                // If something doesn't work, just plop the files in the /.minecraft/ directory.
                directory = Path.of("./");
                e.printStackTrace();
            }
        }

        IncubusCore.LOG.info("Devels loaded for: {}.", String.join(", ", MOD_IDS));
        String faultTrackers = LOADED_FAULT_TRACKERS.stream().map(Identifier::toString)
                .collect(Collectors.joining(", "));

        if (!faultTrackers.isEmpty()) {
            IncubusCore.LOG.info("Fault trackers loaded: {}", faultTrackers);
        } else {
            IncubusCore.LOG.info("All fault trackers loaded");
        }
        // Save on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(Devel::commonSave));
    }

    @Environment(EnvType.CLIENT)
    static void clientInit(){
        // We don't need to do all the fancy stuff for this method, since
        // it should already be covered by the common init.
        Runtime.getRuntime().addShutdownHook(new Thread(Devel::clientSave));
    }

    /**
     * @return true if the mod is being run in a development environment.
     */
    public static boolean isDevel() {
        return isDevel;
    }

    private static void commonSave() {
        save("common", COMMON_FAULT_TRACKERS);
    }

    @Environment(EnvType.CLIENT)
    private static void clientSave() {
        save("client", CLIENT_FAULT_TRACKERS);
    }

    private static void save(String env, Map<Identifier, FaultTracker> faultTrackers){
        for (var mod_id : MOD_IDS) {
            IncubusCore.LOG.info("Saving {} devel log for {}.", env, mod_id);
            var logFile = directory.resolve(Path.of(mod_id + "_todo_" + env + ".txt"));

            try (var writer = new UncheckedWriter(Files.newBufferedWriter(logFile, StandardCharsets.UTF_8))) {
                faultTrackers.forEach((trackerId, tracker) -> {
                    if (LOADED_FAULT_TRACKERS.size() > 0 && !LOADED_FAULT_TRACKERS.contains(trackerId)) return;
                    try {
                        dumpStrings(mod_id, writer, tracker);
                    } catch (UncheckedIOException e) {
                        IncubusCore.LOG.error("Failed to write \"{}\" {} devel log for fault tracker \"{}\" and mod id \"{}\".", logFile.toString(), env, trackerId, mod_id);
                    }
                });
            } catch (IOException e) {
                IncubusCore.LOG.error("Failed to write \"{}\" {} devel log for mod \"{}\".", logFile.toString(), env, mod_id);
                e.printStackTrace();
            }
        }
    }

    private static void dumpStrings(String modId, UncheckedWriter writer, FaultTracker tracker){
        synchronized(tracker.faults){
            if(!tracker.faults.isEmpty()){
                writer.write(tracker.sectionHeader + ":\n");
                tracker.faults.get(modId).stream()
                        .sorted(String::compareTo)
                        .forEachOrdered((id)->writer.write("    " + id + '\n'));
            }
        }
    }

    /**
     * Tracks faults, a.k.a. issues, believe it or not.
     */
    public static class FaultTracker {
        protected final String sectionHeader;
        protected final HashMap<String, Set<String>> faults;

        public FaultTracker(String sectionHeader) {
            this.sectionHeader = sectionHeader;
            this.faults = new HashMap<>();
        }

        public void add(String faultCauserModId, String fault) {
            if (faults.containsKey(faultCauserModId)) {
                faults.get(faultCauserModId).add(fault);
            } else {
                var faultSet = new HashSet<String>();
                faultSet.add(fault);
                faults.put(faultCauserModId, faultSet);
            }
        }

        public void add(Identifier fault) {
            add(fault.getNamespace(), fault.getPath());
        }
    }
}
