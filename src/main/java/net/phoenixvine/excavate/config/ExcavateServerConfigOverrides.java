package net.phoenixvine.excavate.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.phoenixvine.excavate.PhoenixExcavate;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = PhoenixExcavate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExcavateServerConfigOverrides {

    public static final String OVERRIDES_FILE_NAME = "phoenix_excavate-server-overrides.toml";

    private static final String HEADER_COMMENT =
            "=================================================================================\n" +
            " Phoenix Excavate - GLOBAL server-config overrides\n" +
            "=================================================================================\n" +
            " Every value below is auto-generated at its CURRENT DEFAULT. On its own, this file\n" +
            " does nothing - a value only becomes an active override once you edit it away from\n" +
            " its shipped default. Anything left matching the default (including keys generated\n" +
            " by an older version of this mod whose default has since changed) is ignored, the\n" +
            " same as if the key weren't in this file at all.\n" +
            "\n" +
            " This file is never written to by the mod once it exists, and its values are never\n" +
            " written back into any per-world serverconfig/phoenix_excavate-server.toml file.\n" +
            "=================================================================================";

    private static Field cachedValueField;

    private ExcavateServerConfigOverrides() {}

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        apply(event.getConfig());
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        apply(event.getConfig());
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() != ExcavateServerConfig.SPEC) return;

        Path overridesPath = FMLPaths.CONFIGDIR.get().resolve(OVERRIDES_FILE_NAME);
        if (!Files.exists(overridesPath)) {
            generateDefaultsFile(overridesPath);
            return; 
        }

        Map<String, Object> flattened = new HashMap<>();
        CommentedFileConfig overrides = CommentedFileConfig.of(overridesPath);
        try {
            overrides.load();
            flatten(overrides, "", flattened);
        } catch (Exception e) {
            PhoenixExcavate.LOGGER.warn("[{}] Failed to load {}: {}", PhoenixExcavate.MOD_ID, OVERRIDES_FILE_NAME, e.toString());
            return;
        } finally {
            try {
                overrides.close();
            } catch (Exception ignored) {
                
            }
        }

        if (flattened.isEmpty()) return;

        Map<String, ForgeConfigSpec.ConfigValue<?>> byPath = mapConfigValues();

        for (Map.Entry<String, Object> entry : flattened.entrySet()) {
            String path = entry.getKey();
            ForgeConfigSpec.ConfigValue<?> configValue = byPath.get(path);
            if (configValue == null) {
                PhoenixExcavate.LOGGER.warn("[{}] {} sets unknown config key '{}' - ignoring it. Check for a typo " +
                        "against the generated per-world serverconfig/phoenix_excavate-server.toml.",
                        PhoenixExcavate.MOD_ID, OVERRIDES_FILE_NAME, path);
                continue;
            }
            try {
                applyOverride(configValue, entry.getValue(), path);
            } catch (Exception e) {
                PhoenixExcavate.LOGGER.warn("[{}] Failed to apply override for '{}' from {}: {}",
                        PhoenixExcavate.MOD_ID, path, OVERRIDES_FILE_NAME, e.toString());
            }
        }
    }

    private static void generateDefaultsFile(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            CommentedFileConfig generated = CommentedFileConfig.of(path);
            try {
                UnmodifiableConfig specRoot = ExcavateServerConfig.SPEC.getSpec();
                writeDefaults(ExcavateServerConfig.SPEC.getValues(), specRoot, new ArrayList<>(), generated);
                generated.save();
            } finally {
                try {
                    generated.close();
                } catch (Exception ignored) {
                    
                }
            }

            String body = Files.readString(path, StandardCharsets.UTF_8);
            Files.writeString(path, HEADER_COMMENT + "\n\n" + body, StandardCharsets.UTF_8);

            PhoenixExcavate.LOGGER.info("[{}] Generated default {} - edit a value there and save to make it an active override.",
                    PhoenixExcavate.MOD_ID, OVERRIDES_FILE_NAME);
        } catch (Exception e) {
            PhoenixExcavate.LOGGER.warn("[{}] Failed to generate default {}: {}",
                    PhoenixExcavate.MOD_ID, OVERRIDES_FILE_NAME, e.toString());
        }
    }

    private static void writeDefaults(UnmodifiableConfig valuesNode, UnmodifiableConfig specRoot,
                                       List<String> path, CommentedFileConfig target) {
        for (Map.Entry<String, Object> entry : valuesNode.valueMap().entrySet()) {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                target.set(childPath, configValue.getDefault());
                Object specEntry = specRoot.get(childPath);
                if (specEntry instanceof ForgeConfigSpec.ValueSpec valueSpec && valueSpec.getComment() != null) {
                    target.setComment(childPath, valueSpec.getComment());
                }
            } else if (value instanceof UnmodifiableConfig nested) {
                writeDefaults(nested, specRoot, childPath, target);
            }
        }
    }

    private static void flatten(UnmodifiableConfig config, String prefix, Map<String, Object> out) {
        for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig nested) {
                flatten(nested, path, out);
            } else {
                out.put(path, value);
            }
        }
    }

    private static Map<String, ForgeConfigSpec.ConfigValue<?>> mapConfigValues() {
        Map<String, ForgeConfigSpec.ConfigValue<?>> map = new HashMap<>();
        for (Field field : ExcavateServerConfig.class.getDeclaredFields()) {
            if (!ForgeConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                    map.put(String.join(".", configValue.getPath()), configValue);
                }
            } catch (ReflectiveOperationException ignored) {
                
            }
        }
        return map;
    }

    private static void applyOverride(ForgeConfigSpec.ConfigValue<?> configValue, Object rawValue, String path) throws ReflectiveOperationException {
        Object converted = convert(configValue, rawValue, path);
        if (converted == null) return; 

        Object defaultValue = configValue.getDefault();
        if (Objects.equals(converted, defaultValue)) return;

        cachedValueField().set(configValue, converted);
    }

    private static Object convert(ForgeConfigSpec.ConfigValue<?> configValue, Object rawValue, String path) {
        if (configValue instanceof ForgeConfigSpec.BooleanValue) {
            if (rawValue instanceof Boolean b) return b;
            warnTypeMismatch(path, "a boolean", rawValue);
            return null;
        }
        if (configValue instanceof ForgeConfigSpec.IntValue) {
            if (rawValue instanceof Number n) return n.intValue();
            warnTypeMismatch(path, "an integer", rawValue);
            return null;
        }
        if (configValue instanceof ForgeConfigSpec.LongValue) {
            if (rawValue instanceof Number n) return n.longValue();
            warnTypeMismatch(path, "a long", rawValue);
            return null;
        }
        if (configValue instanceof ForgeConfigSpec.DoubleValue) {
            if (rawValue instanceof Number n) return n.doubleValue();
            warnTypeMismatch(path, "a double", rawValue);
            return null;
        }
        if (rawValue instanceof List<?> list) {

            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }

        return rawValue;
    }

    private static void warnTypeMismatch(String path, String expected, Object actual) {
        PhoenixExcavate.LOGGER.warn("[{}] {} sets '{}' to {}, but expected {} - ignoring that override.",
                PhoenixExcavate.MOD_ID, OVERRIDES_FILE_NAME, path, actual, expected);
    }

    private static Field cachedValueField() throws NoSuchFieldException {
        if (cachedValueField == null) {
            Field f = ForgeConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
            f.setAccessible(true);
            cachedValueField = f;
        }
        return cachedValueField;
    }
}
