package com.tterrag.blur;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tterrag.blur.config.BlurConfig;
import ladysnake.satin.api.managed.ManagedCoreShader;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class DualKawaseEffect {
    private static final ManagedCoreShader DOWN = ShaderEffectManager
            .getInstance()
            .manageCoreShader(
                    new Identifier(Blur.MODID, "dual_kawase/down"),
                    VertexFormats.POSITION_TEXTURE
            );
    private static final ManagedCoreShader UP = ShaderEffectManager
            .getInstance()
            .manageCoreShader(
                    new Identifier(Blur.MODID, "dual_kawase/up"),
                    VertexFormats.POSITION_TEXTURE
            );
    private static WindowFramebuffer[] FRAMEBUFFERS = new WindowFramebuffer[8];

    public static void updateBuffers(int screenWidth, int screenHeight) {
        for (int i = 0; i < FRAMEBUFFERS.length; i++) {
            var buffer = FRAMEBUFFERS[i];
            if (buffer != null) {
                buffer.delete();
            }
            var width = screenWidth >> (i + 1);
            var height = screenHeight >> (i + 1);
            FRAMEBUFFERS[i] = buffer = new WindowFramebuffer(width, height);
            buffer.setTexFilter(GL_LINEAR);
            buffer.beginRead();
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            buffer.endRead();
        }
    }

    public static void maybeRedoBuffers() {
        int targetLength = BlurConfig.iterations;
        if (targetLength != FRAMEBUFFERS.length) {
            for (WindowFramebuffer buffer : FRAMEBUFFERS) {
                if (buffer != null) {
                    buffer.delete();
                }
            }
            FRAMEBUFFERS = new WindowFramebuffer[targetLength];
            var framebuffer = MinecraftClient.getInstance().getFramebuffer();
            updateBuffers(framebuffer.textureWidth, framebuffer.textureHeight);
        }
    }

    public static void render(float offset) {
        maybeRedoBuffers();

        RenderSystem.resetTextureMatrix();

        var displayBuffer = MinecraftClient.getInstance().getFramebuffer();
        double width = displayBuffer.textureWidth;
        double height = displayBuffer.textureHeight;

        // Scale down
        {
            var bufferFrom = displayBuffer;
            for (WindowFramebuffer bufferTo : FRAMEBUFFERS) {
                runShader(DOWN, bufferFrom, bufferTo, width, height, offset);
                bufferFrom = bufferTo;
            }
        }

        // Scale up
        for (int i = (FRAMEBUFFERS.length - 1); i >= 0; i--) {
            var bufferFrom = FRAMEBUFFERS[i];
            var bufferTo = i == 0 ? displayBuffer : FRAMEBUFFERS[i - 1];
            runShader(UP, bufferFrom, bufferTo, width, height, offset);
        }

        displayBuffer.beginWrite(true);
    }

    private static void runShader(ManagedCoreShader shader, Framebuffer from, Framebuffer to, double width, double height, float offset) {
        shader.findSampler("DiffuseSampler").set(from);
        shader.findUniform2f("InSize").set(from.textureWidth, from.textureHeight);
        shader.findUniform1f("Offset").set(offset);
        RenderSystem.setShader(shader::getProgram);

        from.endWrite();
        to.clear(MinecraftClient.IS_SYSTEM_MAC);
        to.beginWrite(false);

        var tessellator = Tessellator.getInstance();

        var bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(0, 0, 0).texture(0, 1).next();
        bufferBuilder.vertex(0, height, 0).texture(0, 0).next();
        bufferBuilder.vertex(width, height, 0).texture(1, 0).next();
        bufferBuilder.vertex(width, 0, 0).texture(1, 1).next();
        tessellator.draw();

        from.endRead();
    }
}
