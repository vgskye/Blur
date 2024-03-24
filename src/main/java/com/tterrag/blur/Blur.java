package com.tterrag.blur;

import com.tterrag.blur.config.BlurConfig;
import eu.midnightdust.lib.util.MidnightColorUtil;
import ladysnake.satin.api.event.ResolutionChangeCallback;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.Objects;

public class Blur implements ClientModInitializer {

    public static final String MODID = "blur";
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static long start;
    public static String prevScreen;
    public static boolean screenHasBackground;

    @Override
    public void onInitializeClient() {
        BlurConfig.init("blur", BlurConfig.class);

        ResolutionChangeCallback.EVENT.register(DualKawaseEffect::updateBuffers);

        ShaderEffectRenderCallback.EVENT.register((deltaTick) -> {
            if (start > 0) {
                DualKawaseEffect.render(getProgress(client.currentScreen != null));
            }
        });
    }

    private static boolean doFade = false;

    public static void onScreenChange(Screen newGui) {
        if (client.world != null) {
            boolean excluded = newGui == null || BlurConfig.blurExclusions.stream().anyMatch(exclusion -> newGui.getClass().getName().contains(exclusion));
            if (!excluded) {
                screenHasBackground = false;
                if (BlurConfig.showScreenTitle) System.out.println(newGui.getClass().getName());
                if (doFade) {
                    start = System.currentTimeMillis();
                    doFade = false;
                }
                prevScreen = newGui.getClass().getName();
            } else if (newGui == null && BlurConfig.fadeOutTimeMillis > 0 && !Objects.equals(prevScreen, "")) {
                start = System.currentTimeMillis();
                doFade = true;
            } else {
                screenHasBackground = false;
                start = -1;
                doFade = true;
                prevScreen = "";
            }
        }
    }

    private static float getProgress(boolean fadeIn) {
        float x;
        if (fadeIn) {
            x = Math.min((System.currentTimeMillis() - start) / (float) BlurConfig.fadeTimeMillis, 1);
            if (BlurConfig.ease) x *= (2 - x);  // easeInCubic
        }
        else {
            x = Math.max(1 + (start - System.currentTimeMillis()) / (float) BlurConfig.fadeOutTimeMillis, 0);
            if (BlurConfig.ease) x *= (2 - x);  // easeOutCubic
            if (x <= 0) {
                start = 0;
                screenHasBackground = false;
            }
        }
        return x;
    }

    public static int getBackgroundColor(boolean second, boolean fadeIn) {
        int a = second ? BlurConfig.gradientEndAlpha : BlurConfig.gradientStartAlpha;
        var col = MidnightColorUtil.hex2Rgb(second ? BlurConfig.gradientEnd : BlurConfig.gradientStart);
        int r = (col.getRGB() >> 16) & 0xFF;
        int b = (col.getRGB() >> 8) & 0xFF;
        int g = col.getRGB() & 0xFF;
        float prog = getProgress(fadeIn);
        a *= prog;
        r *= prog;
        g *= prog;
        b *= prog;
        return a << 24 | r << 16 | b << 8 | g;
    }
}
