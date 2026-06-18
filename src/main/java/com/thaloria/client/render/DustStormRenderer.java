package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

@Mod.EventBusSubscriber(value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class DustStormRenderer {

    public static boolean stormActive = false;

    private static long stormStartTime = 0;
    private static long stormEndTime   = 0;
    private static final long FADE_MS  = 4000;

    // Частицы — позиции в МИРОВОМ пространстве относительно спавна
    // Обновляем каждый кадр сдвигая по ветру
    private static final int   PARTICLE_COUNT = 500;
    private static final Random rng = new Random(99887);

    // Мировые смещения частиц (относительно позиции игрока при старте)
    private static final float[] WX    = new float[PARTICLE_COUNT];
    private static final float[] WY    = new float[PARTICLE_COUNT];
    private static final float[] WZ    = new float[PARTICLE_COUNT];
    private static final float[] SPEED = new float[PARTICLE_COUNT];
    private static final float[] SIZE  = new float[PARTICLE_COUNT];

    // Радиус облака частиц вокруг игрока
    private static final float RADIUS_H = 60f; // горизонтальный
    private static final float RADIUS_V = 15f; // вертикальный

    static {
        for (int i = 0; i < PARTICLE_COUNT; i++) spawnParticle(i);
    }

    private static void spawnParticle(int i) {
        // Случайная позиция в большом объёме вокруг игрока
        WX[i] = (rng.nextFloat() - 0.5f) * RADIUS_H * 2f;
        WY[i] = (rng.nextFloat() - 0.5f) * RADIUS_V * 2f;
        WZ[i] = (rng.nextFloat() - 0.5f) * RADIUS_H * 2f;
        // Скорость ветра — быстро
        SPEED[i] = 0.3f + rng.nextFloat() * 0.5f;
        SIZE[i]  = 0.08f + rng.nextFloat() * 0.20f;
    }

    private static void respawnBehind(int i) {
        // Появляется с наветренной стороны (X-)
        WX[i] = -RADIUS_H + (rng.nextFloat() - 0.5f) * 10f;
        WY[i] = (rng.nextFloat() - 0.5f) * RADIUS_V * 2f;
        WZ[i] = (rng.nextFloat() - 0.5f) * RADIUS_H * 2f;
        SPEED[i] = 0.3f + rng.nextFloat() * 0.5f;
        SIZE[i]  = 0.08f + rng.nextFloat() * 0.20f;
    }

    public static void setStormActive(boolean active) {
        if (active && !stormActive) {
            stormStartTime = System.currentTimeMillis();
            stormEndTime   = 0;
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

    // ── Частицы пыли в мире ──────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        float progress = getProgress();
        if (progress <= 0f) return;
        if (!isThaloriaLevel()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        // Получаем матрицу вида и инвертируем её чтобы рисовать в world space
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Направление ветра в мировом пространстве — строго по оси X
        float windX = 0.35f;
        float windZ = 0.08f;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Двигаем частицу по ветру в мировом пространстве
            WX[i] += SPEED[i] * windX;
            WZ[i] += SPEED[i] * windZ;
            WY[i] -= SPEED[i] * 0.01f; // чуть оседает

            // Если вышла за радиус — появляется с другой стороны
            if (WX[i] > RADIUS_H) respawnBehind(i);
            if (WY[i] < -RADIUS_V) WY[i] = RADIUS_V;

            // Позиция частицы относительно камеры (camera space)
            float rx = (float)(WX[i]);
            float ry = (float)(WY[i]);
            float rz = (float)(WZ[i]);

            float s = SIZE[i];
            float stretch = SPEED[i] * 2.0f;

            // Серо-голубой цвет
            float r = 0.52f + rng.nextFloat() * 0.08f;
            float g = 0.60f + rng.nextFloat() * 0.08f;
            float b = 0.68f + rng.nextFloat() * 0.08f;
            float a = progress * (0.40f + rng.nextFloat() * 0.35f);

            // Рисуем частицу как горизонтальную полоску в camera space
            // Но ориентируем её по реальному направлению ветра в мире
            // Для этого проецируем вектор ветра на плоскость экрана
            var pose = poseStack.last().pose();

            // Полоска вытянута по X (направление ветра) с fade на хвосте
            builder.vertex(pose, rx - stretch, ry - s * 0.4f, rz).color(r, g, b, a * 0.15f).endVertex();
            builder.vertex(pose, rx + s,       ry - s * 0.4f, rz).color(r, g, b, a).endVertex();
            builder.vertex(pose, rx + s,       ry + s * 0.4f, rz).color(r, g, b, a).endVertex();
            builder.vertex(pose, rx - stretch, ry + s * 0.4f, rz).color(r, g, b, a * 0.15f).endVertex();
        }

        Tesselator.getInstance().end();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ── Цвет и плотность тумана ──────────────────────────────────

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

    // ── Оверлей ───────────────────────────────────────────────────

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