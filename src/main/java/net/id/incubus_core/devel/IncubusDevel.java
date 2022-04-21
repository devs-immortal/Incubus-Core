package net.id.incubus_core.devel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.id.incubus_core.util.Config;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static net.id.incubus_core.IncubusCore.locate;
import static net.id.incubus_core.devel.Devel.*;

/**
 * This is not for public use.
 */
public final class IncubusDevel {
    private static final FaultTracker BAD_FEATURES = registerFaultTracker(locate("bad_features"), "Bad Features");
    @Environment(EnvType.CLIENT)
    private static final FaultTracker MISSING_TEXTURES = registerClientFaultTracker(locate("missing_textures"), "Missing Textures");
    @Environment(EnvType.CLIENT)
    private static final FaultTracker BAD_TEXTURES = registerClientFaultTracker(locate("bad_textures"), "Bad Textures");
    @Environment(EnvType.CLIENT)
    private static final FaultTracker MISSING_LANGUAGE_KEYS = registerClientFaultTracker(locate("missing_lang"), "Missing Language Keys");

    /**
     * This is not for public use.
     */
    public static void init() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            throw new RuntimeException("Trying to initiate devel tools in production!");
        }
        Devel.init();
    }

    /**
     * This is especially not for public use.
     */
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            throw new RuntimeException("Trying to initiate client devel tools in production!");
        }
        Devel.clientInit();
    }

    /**
     * This is not for public use.
     */
    public static void logBadFeature(String feature){
        synchronized(BAD_FEATURES){
            BAD_FEATURES.add("unknown", feature);
        }
    }

    /**
     * This is especially not for public use.
     */
    @Environment(EnvType.CLIENT)
    public static void logMissingTexture(Identifier identifier){
        synchronized(MISSING_TEXTURES){
            MISSING_TEXTURES.add(identifier);
        }
    }

    /**
     * This is especially not for public use.
     */
    @Environment(EnvType.CLIENT)
    public static void logBadTexture(Identifier identifier){
        synchronized(BAD_TEXTURES){
            BAD_TEXTURES.add(identifier);
        }
    }

    /**
     * This is especially not for public use.
     */
    @Environment(EnvType.CLIENT)
    public static void logMissingLanguageKey(String key){
        synchronized(MISSING_LANGUAGE_KEYS){
            MISSING_LANGUAGE_KEYS.add("unknown", key);
        }
    }

    /**
     * Configuration options for development. Mostly internal stuff you don't need to bother with.
     */
    @SuppressWarnings("SameParameterValue")
    public static final class DevelConfig {
        public static final boolean PRINT_SETBLOCK_STACK_TRACE = Config.getBoolean(locate("devel.setblock_stack_trace"), false);
        public static final Path DIRECTORY = getPath(locate("devel.directory"), Path.of("./devel"));
        public static final String[] MODS = getStringArrayAndUnknown(locate("devel.mods"), new String[0]);
        public static final Set<Identifier> FAULT_TRACKERS = getIdSet(locate("devel.fault_trackers"), new HashSet<>());

        private static String[] getStringArrayAndUnknown(Identifier key, String[] defaultValue) {
            var arr = Config.get(key, (s) -> s.split(","), defaultValue);
            if (arr.length == 0) return arr; // Don't check devels if they don't want to.
            var newArr = Arrays.copyOf(arr, arr.length + 1);
            newArr[arr.length] = "unknown";
            return newArr;
        }

        private static Set<Identifier> getIdSet(Identifier key, Set<Identifier> defaultValue) {
            return Config.get(key, (s) ->
                    Arrays.stream(s.split(","))
                            .map(Identifier::tryParse)
                            .collect(Collectors.toSet()),
                    defaultValue);
        }

        private static Path getPath(Identifier key, Path defaultValue) {
            return Config.get(key, Path::of, defaultValue);
        }
    }
}
