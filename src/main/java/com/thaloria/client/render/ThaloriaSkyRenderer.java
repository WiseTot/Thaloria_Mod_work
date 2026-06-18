package com.thaloria.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

public class ThaloriaSkyRenderer {

    private static final ResourceLocation GALAXY_TEXTURE =
            new ResourceLocation("thaloria", "textures/environment/galaxy.png");
    private static final ResourceLocation MOON_1_TEXTURE =
            new ResourceLocation("thaloria", "textures/environment/moon_1.png");
    private static final ResourceLocation MOON_2_TEXTURE =
            new ResourceLocation("thaloria", "textures/environment/moon_2.png");
    private static final ResourceLocation SUN_TEXTURE =
            new ResourceLocation("minecraft", "textures/environment/sun.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (!Minecraft.getInstance().level.dimension().location()
                .toString().equals("thaloria:thaloria_planet")) return;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        float partialTicks = event.getPartialTick();
        float timeOfDay = Minecraft.getInstance().level.getTimeOfDay(partialTicks);
        long totalGameTime = Minecraft.getInstance().level.getGameTime();

        Vec3 skyColor = Minecraft.getInstance().level
                .getSkyColor(event.getCamera().getPosition(), partialTicks);
        float starBrightness = Minecraft.getInstance().level.getStarBrightness(partialTicks);

        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(event.getCamera().getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(event.getCamera().getYRot() + 180.0F));

        Matrix4f projectionMatrix = event.getProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        // 1. БАЗОВОЕ НЕБО
        renderSolidSkybox(poseStack, tesselator, bufferbuilder,
                (float)skyColor.x, (float)skyColor.y, (float)skyColor.z);

        // 2. ГОРИЗОНТ
        renderGlobalHorizon(poseStack, tesselator, bufferbuilder, timeOfDay);

        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );

        // 3. ГАЛАКТИКА
        if (starBrightness > 0.0F) {
            renderFullSkybox(poseStack, tesselator, bufferbuilder,
                    GALAXY_TEXTURE, 100.0F, starBrightness);
        }

        // 4. АУРА СОЛНЦА
        renderSunAura(poseStack, tesselator, bufferbuilder, timeOfDay, event.getCamera());

        // 5. СОЛНЦЕ
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0F));
        renderCelestialObject(poseStack, tesselator, bufferbuilder, SUN_TEXTURE, 30.0F, 1.0F);
        poseStack.popPose();

        // 6. ЛУНЫ
        if (starBrightness > 0.0F) {
            float moonAlpha = Math.min(0.8F, starBrightness * 0.9F);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0F + 180.0F));
            renderCelestialObject(poseStack, tesselator, bufferbuilder,
                    MOON_1_TEXTURE, 8.0F, moonAlpha);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            float dayCycle = (totalGameTime % 168000L) / 168000.0F;
            float wobble = (float)Math.sin(dayCycle * Math.PI * 2.0D) * 40.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(wobble));
            poseStack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0F * 1.5F + 180.0F));
            renderCelestialObject(poseStack, tesselator, bufferbuilder,
                    MOON_2_TEXTURE, 4.0F, moonAlpha);
            poseStack.popPose();
        }

        RenderSystem.defaultBlendFunc();

        // 7. ТУМАН ШТОРМА ПОВЕРХ НЕБА
        float stormProgress = DustStormRenderer.getProgress();
        if (stormProgress > 0f && DustStormRenderer.isThaloriaLevel()) {
            renderStormSkyFog(poseStack, tesselator, bufferbuilder, stormProgress);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
        Minecraft.getInstance().gameRenderer.resetProjectionMatrix(event.getProjectionMatrix());
    }

    /**
     * Рисует серо-голубой скайбокс поверх неба — закрывает небо и звёзды во время шторма
     */
    private static void renderStormSkyFog(PoseStack poseStack, Tesselator tesselator,
                                          BufferBuilder bufferbuilder, float progress) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float r = 0.48f, g = 0.55f, b = 0.63f;
        float alpha = Mth.clamp(progress * 0.92f, 0f, 0.92f);

        float depth = 150.0F;

        for (int i = 0; i < 6; ++i) {
            poseStack.pushPose();
            if (i == 1) poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (i == 2) poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            if (i == 3) poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            if (i == 4) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            if (i == 5) poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));

            Matrix4f matrix4f = poseStack.last().pose();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(matrix4f, -depth, -depth, -depth).color(r, g, b, alpha).endVertex();
            bufferbuilder.vertex(matrix4f,  depth, -depth, -depth).color(r, g, b, alpha).endVertex();
            bufferbuilder.vertex(matrix4f,  depth,  depth, -depth).color(r, g, b, alpha).endVertex();
            bufferbuilder.vertex(matrix4f, -depth,  depth, -depth).color(r, g, b, alpha).endVertex();
            tesselator.end();

            poseStack.popPose();
        }
    }

    private static float lerpColor(float t, float start, float end) {
        return start + t * (end - start);
    }

    private static void renderGlobalHorizon(PoseStack poseStack, Tesselator tesselator,
                                            BufferBuilder bufferbuilder, float timeOfDay) {
        float sunHeight = (float)Math.cos(timeOfDay * Math.PI * 2.0D);
        float distFromHorizon = Math.abs(sunHeight);

        if (distFromHorizon < 0.4f) {
            float alpha = (0.4f - distFromHorizon) / 0.4f;
            float r = 0.8F, g = 0.05F, b = 0.05F;

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableCull();

            poseStack.pushPose();
            Matrix4f matrix4f = poseStack.last().pose();

            float radius = 100.0F;
            float heightUp = 40.0F;
            float heightDown = -15.0F;

            bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            int segments = 32;
            for (int i = 0; i <= segments; ++i) {
                float angle = (float)i * ((float)Math.PI * 2F) / segments;
                float x = (float)Math.sin(angle) * radius;
                float z = (float)Math.cos(angle) * radius;
                bufferbuilder.vertex(matrix4f, x, heightUp,   z).color(r, g, b, 0.0F).endVertex();
                bufferbuilder.vertex(matrix4f, x, heightDown, z).color(r, g, b, alpha * 0.6F).endVertex();
            }
            tesselator.end();
            poseStack.popPose();
            RenderSystem.enableCull();
        }
    }

    private static void renderSunAura(PoseStack poseStack, Tesselator tesselator,
                                      BufferBuilder bufferbuilder, float timeOfDay, Camera camera) {
        float sunHeight = (float)Math.cos(timeOfDay * Math.PI * 2.0D);
        float distFromHorizon = Math.abs(sunHeight);

        if (distFromHorizon < 0.4f) {
            org.joml.Vector3f lookVec = new org.joml.Vector3f(camera.getLookVector()).normalize();
            org.joml.Vector3f sunVec  = new org.joml.Vector3f(
                    (float)-Math.sin(timeOfDay * Math.PI * 2),
                    (float) Math.cos(timeOfDay * Math.PI * 2),
                    0.0f).normalize();

            float dot = lookVec.dot(sunVec);
            float lookFactor = Math.max(0.0F, (dot - 0.7F) / 0.3F);

            float r = lerpColor(lookFactor, 0.4F, 0.8F);
            float g = lerpColor(lookFactor, 0.0F, 0.4F);
            float b = lerpColor(lookFactor, 0.0F, 0.2F);

            float baseVisibility = (0.4f - distFromHorizon) / 0.4f;
            float alpha = baseVisibility * (0.5F + lookFactor * 0.5F);

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableCull();

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0F));

            Matrix4f matrix4f = poseStack.last().pose();

            bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(matrix4f, 0.0F, 95.0F, 0.0F).color(r, g, b, alpha).endVertex();

            int segments = 64;
            for (int j = 0; j <= segments; ++j) {
                float angle = (float)j * ((float)Math.PI * 2F) / segments;
                float x = (float)Math.sin(angle) * 350.0F;
                float z = (float)Math.cos(angle) * 120.0F;
                bufferbuilder.vertex(matrix4f, x, 95.0F, z).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
            }
            tesselator.end();
            poseStack.popPose();
            RenderSystem.enableCull();
        }
    }

    private static void renderCelestialObject(PoseStack poseStack, Tesselator tesselator,
                                              BufferBuilder bufferbuilder,
                                              ResourceLocation texture, float size, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.disableCull();

        Matrix4f matrix4f = poseStack.last().pose();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, -size, 90.0F, -size).uv(0.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f,  size, 90.0F, -size).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(matrix4f,  size, 90.0F,  size).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex(matrix4f, -size, 90.0F,  size).uv(0.0F, 1.0F).endVertex();
        tesselator.end();
        RenderSystem.enableCull();
    }

    private static void renderSolidSkybox(PoseStack poseStack, Tesselator tesselator,
                                          BufferBuilder bufferbuilder, float r, float g, float b) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < 6; ++i) {
            poseStack.pushPose();
            if (i == 1) poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (i == 2) poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            if (i == 3) poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            if (i == 4) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            if (i == 5) poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));

            Matrix4f matrix4f = poseStack.last().pose();
            float depth = 150.0F;
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(matrix4f, -depth, -depth, -depth).color(r, g, b, 1.0F).endVertex();
            bufferbuilder.vertex(matrix4f,  depth, -depth, -depth).color(r, g, b, 1.0F).endVertex();
            bufferbuilder.vertex(matrix4f,  depth,  depth, -depth).color(r, g, b, 1.0F).endVertex();
            bufferbuilder.vertex(matrix4f, -depth,  depth, -depth).color(r, g, b, 1.0F).endVertex();
            tesselator.end();
            poseStack.popPose();
        }
    }

    private static void renderFullSkybox(PoseStack poseStack, Tesselator tesselator,
                                         BufferBuilder bufferbuilder,
                                         ResourceLocation texture, float depth, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        for (int i = 0; i < 6; ++i) {
            poseStack.pushPose();
            if (i == 1) poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            if (i == 2) poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            if (i == 3) poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            if (i == 4) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            if (i == 5) poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));

            Matrix4f matrix4f = poseStack.last().pose();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.vertex(matrix4f, -depth, -depth, -depth).uv(0.0F, 0.0F).endVertex();
            bufferbuilder.vertex(matrix4f,  depth, -depth, -depth).uv(1.0F, 0.0F).endVertex();
            bufferbuilder.vertex(matrix4f,  depth,  depth, -depth).uv(1.0F, 1.0F).endVertex();
            bufferbuilder.vertex(matrix4f, -depth,  depth, -depth).uv(0.0F, 1.0F).endVertex();
            tesselator.end();
            poseStack.popPose();
        }
    }
}