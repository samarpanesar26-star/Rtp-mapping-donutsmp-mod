package com.donutsmp.rtpmapper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class DonutRtpMapperClient implements ClientModInitializer {
    public static final String MOD_ID = "donutsmp_rtp_mapper";
    public static final RtpStore STORE = new RtpStore();
    private static KeyBinding openMapper;
    private static boolean mappingEnabled = false;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "mapper"));
    private static boolean waitingForRtp = false;
    private static long waitingSince = 0L;
    private static double waitingX, waitingY, waitingZ;

    @Override
    public void onInitializeClient() {
        STORE.load(MinecraftClient.getInstance());

        openMapper = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutsmp_rtp_mapper.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String normalized = command.trim().toLowerCase();
            if (mappingEnabled && (normalized.equals("rtp") || normalized.startsWith("rtp "))) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    waitingForRtp = true;
                    waitingSince = System.currentTimeMillis();
                    waitingX = client.player.getX();
                    waitingY = client.player.getY();
                    waitingZ = client.player.getZ();
                }
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapper.wasPressed()) {
                client.setScreen(new MapperScreen(client.currentScreen));
            }
            if (client.player == null) return;

            if (waitingForRtp) {
                long age = System.currentTimeMillis() - waitingSince;
                double dx = client.player.getX() - waitingX;
                double dy = client.player.getY() - waitingY;
                double dz = client.player.getZ() - waitingZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;

                // DonutSMP RTP is a large teleport. A small threshold avoids recording ordinary movement.
                if (distanceSq >= 200.0 * 200.0 && age <= 15_000L) {
                    STORE.add(client.player.getX(), client.player.getY(), client.player.getZ(),
                            client.world == null ? "unknown" : client.world.getRegistryKey().getValue().toString());
                    STORE.save(client);
                    waitingForRtp = false;
                } else if (age > 15_000L) {
                    waitingForRtp = false;
                }
            }
        });
    }

    public static boolean isMappingEnabled() { return mappingEnabled; }
    public static void setMappingEnabled(boolean enabled) {
        mappingEnabled = enabled;
        if (!enabled) waitingForRtp = false;
    }

    public static void armManualRtpCapture(MinecraftClient client) {
        if (client.player == null) return;
        waitingForRtp = true;
        waitingSince = System.currentTimeMillis();
        waitingX = client.player.getX();
        waitingY = client.player.getY();
        waitingZ = client.player.getZ();
    }
}
