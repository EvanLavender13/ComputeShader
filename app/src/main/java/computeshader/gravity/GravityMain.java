package computeshader.gravity;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import computeshader.core.ShaderApp;
import computeshader.core.ShaderApp.ShaderAppConfiguration;
import imgui.ImGui;
import imgui.app.Application;

public class GravityMain {
    private static final Logger logger = LogManager.getLogger();

    private ShaderApp app;

    private String title = "Gravity";
    private int windowWidth = 1600;
    private int windowHeight = 900;
    private int textureWidth = 1920;
    private int textureHeight = 1080;

    private int numObjects = (int)  Math.pow(2, 12);

    private double currentTime = 0.0f;
    private double previousFrameTime = 0.0;
    private double deltaTime = 0.0;

    public GravityMain() {
        app = new ShaderApp(new ShaderAppConfiguration(title, windowWidth, windowHeight, textureWidth, textureHeight));

        app.preRun(() -> {
            app.createTexture("Display");

            app.addUniform("Stage");

            app.createComputeShader("GravityShader", "/gravity.glsl");

            app.createStorageBuffer("ShaderParameters", new float[] {
                    (float) textureWidth,
                    (float) textureHeight,
            });

            logger.info("Creating {} objects", numObjects);
            app.createStorageBuffer("ObjectData", ObjectUtil.createObjects(numObjects, textureWidth, textureHeight));
        });

        app.gui(() -> {
            currentTime = app.getTime();
            deltaTime = currentTime - previousFrameTime;
            previousFrameTime = currentTime;

            ImGui.text("Runtime: " + currentTime);
            ImGui.text("Delta  : " + deltaTime);
            ImGui.text("Object Count: " + numObjects);
        });

        app.processSteps(List.of(() -> {
            app.setUIntUniform("Stage", 0);

            int[] workGroupSize = app.getWorkGroupSize("GravityShader");
            app.runComputeShader(numObjects / workGroupSize[0]);
        }, () -> {
            app.setUIntUniform("Stage", 1);

            int[] workGroupSize = app.getWorkGroupSize("GravityShader");
            app.runComputeShader(numObjects / workGroupSize[0]);
        }, () -> {
            app.setUIntUniform("Stage", 2);

            int[] workGroupSize = app.getWorkGroupSize("GravityShader");
            app.runComputeShader(textureWidth / workGroupSize[0], textureHeight / workGroupSize[1]);
        }));

        app.display("Display");
    }

    public static void main(String[] args) {
        GravityMain main = new GravityMain();
        Application.launch(main.app);
    }
}
