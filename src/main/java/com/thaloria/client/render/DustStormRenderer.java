package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class DustStormRenderer {

    public static boolean stormActive = false;

    // Время начала шторма — для плавного нарастания и спада
    private static long stormStartTime  = 0;
    private static long stormEndTime    = 0;
    private static boolean wasActive    = false;

    // Fade in/out — 5 секунд
    private static final long FADE_MS = 5000;

    // Частицы пыли
    private static final int PARTICLE_COUNT = 200;
    private static final Random rng = new Random(12345);
    private static final float[] PX = new float[PARTICLE_COUNT];
    private static final float[] PY = new float[PARTICLE_COUNT];
    private static final float[] PZ = new float[PARTICLE_COUNT];
    private static final float[] SPEED = new float[PARTICLE_COUNT];
    private static final float[] SIZE  = new float[PARTICLE_COUNT];

    static {
        // Инициализируем случайные позиции частиц вокруг игрока
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i);
        }
    }

    private static void resetParticle(int i) {
        PX[i] = (rng.nextFloat() - 0.5f) * 40f;
        PY[i] = (rng.nextFloat() - 0.5f) * 20f;
        PZ[i] = (rng.nextFloat() - 0.5f) * 40f;
        SPEED[i] = 0.02f + rng.nextFloat() * 0.05f;
        SIZE[i]  = 0.05f + rng.nextFloat() * 0.15f;
    }

    public static void setStormActive(boolean active) {
        if (active && !stormActive) {
            stormStartTime = System.currentTimeMillis();
            stormEndTime   = 0;
        } else if (!active && stormActive) {
            stormEndTime = System.currentTimeMillis();
        }
        stormActive = active;
        wasActive   = active;
    }

    /** Альфа с учётом fade in/out */
    private static float getAlpha(float maxAlpha) {
        long now = System.currentTimeMillis();
        float alpha = maxAlpha;

        if (stormActive && stormStartTime > 0) {
            long elapsed = now - stormStartTime;
            if (elapsed < FADE_MS) {
                alpha *= (float) elapsed / FADE_MS;
            }
        } else if (!stormActive && stormEndTime > 0) {
            long elapsed = now - stormEndTime;
            if (elapsed < FADE_MS) {
                alpha *= 1f - (float) elapsed / FADE_MS;
            } else {
                // Fade закончился
                stormEndTime = 0;
                return 0f;
            }
        } else if (!stormActive) {
            return 0f;
        }

        return alpha;
    }

    /** Рендер частиц пыли в мире */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        float alpha = getAlpha(0.4f);
        if (alpha <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Только в измерении Талории
        if (!mc.level.dimension().location()
                .toString().equals("thaloria:thaloria_planet")) return;

        long time = mc.level.getGameTime();
        PoseStack poseStack = event.getPoseStack();

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Двигаем частицу по ветру
            PX[i] += SPEED[i] * 1.5f;
            PY[i] += SPEED[i] * 0.1f;
            PZ[i] += SPEED[i] * 0.3f;

            // Если улетела далеко — сбрасываем
            if (PX[i] > 20f) {
                PX[i] = -20f;
                PY[i] = (rng.nextFloat() - 0.5f) * 20f;
                PZ[i] = (rng.nextFloat() - 0.5f) * 40f;
            }

            float x = (float)(PX[i] - cam.x % 40);
            float y = (float)(PY[i] - cam.y % 20);
            float z = (float)(PZ[i] - cam.z % 40);
            float s = SIZE[i];

            // Цвет пыли — оранжево-коричневый
            float r = 0.75f + rng.nextFloat() * 0.15f;
            float g = 0.45f + rng.nextFloat() * 0.15f;
            float b = 0.15f;
            float a = alpha * (0.4f + rng.nextFloat() * 0.4f);

            poseStack.pushPose();
            var pose = poseStack.last().pose();

            builder.vertex(pose, x - s, y - s, z).color(r, g, b, a).endVertex();
            builder.vertex(pose, x + s, y - s, z).color(r, g, b, a).endVertex();
            builder.vertex(pose, x + s, y + s, z).color(r, g, b, a).endVertex();
            builder.vertex(pose, x - s, y + s, z).color(r, g, b, a).endVertex();

            poseStack.popPose();
        }

        Tesselator.getInstance().end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Накладываем затемнение + оранжевый оверлей на экран во время шторма */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;

        float alpha = getAlpha(0.55f);
        if (alpha <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().location()
                .toString().equals("thaloria:thaloria_planet")) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Пульсация — лёгкое мерцание
        long now = System.currentTimeMillis();
        float pulse = (float)(Math.sin(now * 0.002) * 0.05);
        float finalAlpha = Mth.clamp(alpha + pulse, 0f, 0.7f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Оранжево-коричневый туман
        float r = 0.55f, g = 0.30f, b = 0.05f;
        builder.vertex(0,       0,       0).color(r, g, b, finalAlpha).endVertex();
        builder.vertex(0,       screenH, 0).color(r, g, b, finalAlpha).endVertex();
        builder.vertex(screenW, screenH, 0).color(r, g, b, finalAlpha).endVertex();
        builder.vertex(screenW, 0,       0).color(r, g, b, finalAlpha).endVertex();

        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }
}