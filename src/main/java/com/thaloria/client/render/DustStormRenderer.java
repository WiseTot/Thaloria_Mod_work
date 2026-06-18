package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class DustStormRenderer {

    public static boolean stormActive = false;
    private static long stormStartTime = 0;
    private static long stormEndTime   = 0;
    private static final long FADE_MS  = 4000;

    private static final int   PARTICLE_COUNT = 600;
    private static final Random rng = new Random(55443);

    // Абсолютные мировые координаты частиц
    // Инициализируются при первом кадре относительно позиции игрока
    private static final double[] WX   = new double[PARTICLE_COUNT];
    private static final double[] WY   = new double[PARTICLE_COUNT];
    private static final double[] WZ   = new double[PARTICLE_COUNT];
    private static final float[]  SPEED = new float[PARTICLE_COUNT];
    private static final float[]  LEN   = new float[PARTICLE_COUNT]; // длина полоски

    private static boolean initialized = false;

    private static final double RADIUS_H = 40.0;
    private static final double RADIUS_V = 10.0;

    // Направление ветра (мировые оси, не зависит от камеры)
    private static final double WIND_X = 1.0;
    private static final double WIND_Z = 0.15;

    private static void init(Vec3 origin) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            WX[i] = origin.x + (rng.nextDouble() - 0.5) * RADIUS_H * 2;
            WY[i] = origin.y + (rng.nextDouble() - 0.5) * RADIUS_V * 2;
            WZ[i] = origin.z + (rng.nextDouble() - 0.5) * RADIUS_H * 2;
            SPEED[i] = 0.6f + rng.nextFloat() * 0.8f;
            LEN[i]   = 0.4f + rng.nextFloat() * 0.8f;
        }
        initialized = true;
    }

    private static void respawn(int i, Vec3 playerPos) {
        // Появляется с наветренной стороны (X-)
        WX[i] = playerPos.x - RADIUS_H + (rng.nextDouble() - 0.5) * 4;
        WY[i] = playerPos.y + (rng.nextDouble() - 0.5) * RADIUS_V * 2;
        WZ[i] = playerPos.z + (rng.nextDouble() - 0.5) * RADIUS_H * 2;
        SPEED[i] = 0.6f + rng.nextFloat() * 0.8f;
        LEN[i]   = 0.4f + rng.nextFloat() * 0.8f;
    }

    public static void setStormActive(boolean active) {
        if (active && !stormActive) {
            stormStartTime = System.currentTimeMillis();
            stormEndTime   = 0;
            initialized    = false; // переинициализируем вокруг игрока
        } else if (!active && stormActive) {
            stormEndTime = System.currentTimeMillis();
        }
        stormActive = active;
    }

    public static float getProgress() {
        long now = System.currentTimeMillis();
        if (stormActive && stormStartTime > 0) {
            long elapsed = now - stormStartTime;
            if (elapsed < FADE_MS) return (float) elapsed / FADE_MS;
            return 1f;
        } else if (!stormActive && stormEndTime > 0) {
            long elapsed = now - stormEndTime;
            if (elapsed < FADE_MS) return 1f - (float) elapsed / FADE_MS;
            return 0f;
        }
        return stormActive ? 1f : 0f;
    }

    public static boolean isThaloriaLevel() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().location()
                .toString().equals("thaloria:thaloria_planet");
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        float progress = getProgress();
        if (progress <= 0f) return;
        if (!isThaloriaLevel()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 camPos    = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 playerPos = mc.player.position();

        if (!initialized) init(playerPos);

        // Двигаем частицы по ветру
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            WX[i] += SPEED[i] * WIND_X * 0.35;
            WZ[i] += SPEED[i] * WIND_Z * 0.35;
            WY[i] -= SPEED[i] * 0.005;

            // Если улетела дальше RADIUS_H от игрока — респавним с другой стороны
            double dx = WX[i] - playerPos.x;
            double dz = WZ[i] - playerPos.z;
            double dy = WY[i] - playerPos.y;

            if (dx > RADIUS_H || Math.abs(dz) > RADIUS_H) {
                respawn(i, playerPos);
            }
            if (Math.abs(dy) > RADIUS_V) {
                WY[i] = playerPos.y + (rng.nextDouble() - 0.5) * RADIUS_V * 2;
            }
        }

        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Позиция частицы относительно камеры — это и есть координаты в camera space
            // потому что poseStack в AFTER_TRANSLUCENT_BLOCKS уже имеет только translation камеры
            float rx = (float)(WX[i] - camPos.x);
            float ry = (float)(WY[i] - camPos.y);
            float rz = (float)(WZ[i] - camPos.z);

            // Длина частицы вдоль оси ветра в мировых координатах
            float len  = LEN[i] * SPEED[i];
            float thick = 0.04f + LEN[i] * 0.03f;

            // Хвост частицы — смещаем назад по оси X (против ветра)
            float tx = rx - (float)(len * WIND_X);
            float tz = rz - (float)(len * WIND_Z);

            // Серо-голубой цвет пыли Талории
            float r = 0.52f + rng.nextFloat() * 0.06f;
            float g = 0.60f + rng.nextFloat() * 0.06f;
            float b = 0.68f + rng.nextFloat() * 0.06f;
            float a = progress * (0.55f + rng.nextFloat() * 0.30f);

            var pose = poseStack.last().pose();

            // Голова (яркая) → хвост (прозрачный)
            // Полоска горизонтальная, вытянута по вектору ветра (X, Z)
            builder.vertex(pose, tx,        ry - thick, tz       ).color(r, g, b, a * 0.05f).endVertex();
            builder.vertex(pose, rx,        ry - thick, rz       ).color(r, g, b, a).endVertex();
            builder.vertex(pose, rx,        ry + thick, rz       ).color(r, g, b, a).endVertex();
            builder.vertex(pose, tx,        ry + thick, tz       ).color(r, g, b, a * 0.05f).endVertex();
        }

        Tesselator.getInstance().end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float progress = getProgress();
        if (progress <= 0f) return;
        if (!isThaloriaLevel()) return;

        float stormR = 0.48f, stormG = 0.55f, stormB = 0.63f;
        event.setRed(Mth.lerp(progress,   (float)event.getRed(),   stormR));
        event.setGreen(Mth.lerp(progress, (float)event.getGreen(), stormG));
        event.setBlue(Mth.lerp(progress,  (float)event.getBlue(),  stormB));
    }

    @SubscribeEvent
    public static void onFogDensity(ViewportEvent.RenderFog event) {
        float progress = getProgress();
        if (progress <= 0f) return;
        if (!isThaloriaLevel()) return;

        float normalFar = event.getFarPlaneDistance();
        float stormFar  = Mth.lerp(progress, normalFar, 8f);
        event.setFarPlaneDistance(stormFar);
        event.setNearPlaneDistance(0f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.VIGNETTE.type()) return;

        float progress = getProgress();
        if (progress <= 0f) return;
        if (!isThaloriaLevel()) return;

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        long now = System.currentTimeMillis();
        float pulse = (float)(Math.sin(now * 0.0015) * 0.03);
        float alpha = Mth.clamp(progress * 0.22f + pulse, 0f, 0.35f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float r = 0.48f, g = 0.55f, b = 0.63f;
        builder.vertex(0,       0,       0).color(r, g, b, alpha).endVertex();
        builder.vertex(0,       screenH, 0).color(r, g, b, alpha).endVertex();
        builder.vertex(screenW, screenH, 0).color(r, g, b, alpha).endVertex();
        builder.vertex(screenW, 0,       0).color(r, g, b, alpha).endVertex();

        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }
}