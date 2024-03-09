package computeshader.slime;

import static java.lang.Math.pow;
import static java.lang.Math.toRadians;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import computeshader.core.ShaderApp;
import computeshader.core.ShaderApp.ShaderAppConfiguration;
import imgui.ImGui;
import imgui.app.Application;

public class SlimeMain {
    private static final Logger logger = LogManager.getLogger();

    private ShaderApp app;

    private int numAgents;
    private float[] sensingDistanceParameter;
    private float[] sensingAngleParameter;
    private float[] turningAngleParameter;
    private float[] depositAmountParameter;
    private float[] diffuseAmountParameter;
    private float[] decayAmountParameter;
    private float[] stepSizeParameter;

    private String title = "Slime";
    private int windowWidth = 1600;
    private int windowHeight = 900;
    private int textureWidth = 1920;
    private int textureHeight = 1080;

    private double currentTime = glfwGetTime();
    private double previousFrameTime = 0.0;
    private double deltaTime = 0.0;

    public SlimeMain() {
        app = new ShaderApp(new ShaderAppConfiguration(title, windowWidth, windowHeight, textureWidth, textureHeight));

        app.configuration(() -> {
            numAgents = (int) pow(2, 20);

            sensingDistanceParameter = new float[1];
            sensingDistanceParameter[0] = 50.0f;

            sensingAngleParameter = new float[1];
            sensingAngleParameter[0] = (float) toRadians(45.0f);

            turningAngleParameter = new float[1];
            turningAngleParameter[0] = (float) toRadians(45.0f);

            depositAmountParameter = new float[1];
            depositAmountParameter[0] = 1.0f;

            diffuseAmountParameter = new float[1];
            diffuseAmountParameter[0] = 0.5f;

            decayAmountParameter = new float[1];
            decayAmountParameter[0] = 0.5f;

            stepSizeParameter = new float[1];
            stepSizeParameter[0] = 100.0f;
        });

        app.preRun(() -> {
            app.createTexture("AgentMap");
            app.createTexture("AgentMapOut");
            app.createTexture("TrailMap");
            app.createTexture("TrailMapOut");

            app.addUniform("Stage");
            app.addUniform("Time");
            app.addUniform("Delta");

            try {
                app.createComputeShader("AgentShader", "/compute.glsl");

                float[] shaderParameters = new float[] {
                        (float) textureWidth,
                        (float) textureHeight,
                        sensingDistanceParameter[0],
                        sensingAngleParameter[0],
                        turningAngleParameter[0],
                        depositAmountParameter[0],
                        diffuseAmountParameter[0],
                        decayAmountParameter[0],
                        stepSizeParameter[0]
                };
                app.createStorageBuffer("ShaderParameters", shaderParameters);

                logger.info("Creating {} agents", numAgents);
                float[] computeShaderAgents = AgentUtil.nAgentsRainbow(numAgents, textureWidth, textureHeight);
                app.createStorageBuffer("AgentData", computeShaderAgents);
            } catch (IOException | URISyntaxException e) {
                logger.error("Exception caught when creating shaders!", e);
            }
        });

        app.gui(() -> {
            currentTime = app.getTime();
            deltaTime = currentTime - previousFrameTime;
            previousFrameTime = currentTime;

            ImGui.text("Runtime: " + currentTime);
            ImGui.text("Delta  : " + deltaTime);
            ImGui.text("Particle Count: " + numAgents);

            if (ImGui.sliderFloat("Sensing Distance", sensingDistanceParameter, 1.0f, 1000.0f)) {
                sensingDistanceParameter[0] = (float) Math.floor(sensingDistanceParameter[0]);
                app.updateStorageBuffer("ShaderParameters", 2, sensingDistanceParameter);
            }

            if (ImGui.sliderAngle("Sensing Angle", sensingAngleParameter, 0.0f, 180.0f)) {
                app.updateStorageBuffer("ShaderParameters", 3, sensingAngleParameter);
            }

            if (ImGui.sliderAngle("Turning Angle", turningAngleParameter, 0.0f, 180.0f)) {
                app.updateStorageBuffer("ShaderParameters", 4, turningAngleParameter);
            }

            if (ImGui.sliderFloat("Deposit Amount", depositAmountParameter, 0.01f, 1.0f)) {
                app.updateStorageBuffer("ShaderParameters", 5, depositAmountParameter);
            }

            if (ImGui.sliderFloat("Diffuse Amount", diffuseAmountParameter, 0.0f, 1.0f)) {
                app.updateStorageBuffer("ShaderParameters", 6, diffuseAmountParameter);
            }

            if (ImGui.sliderFloat("Decay Amount", decayAmountParameter, 0.0f, 1.0f)) {
                app.updateStorageBuffer("ShaderParameters", 7, decayAmountParameter);
            }

            if (ImGui.sliderFloat("Step Size", stepSizeParameter, 0.0f, 10000.0f)) {
                stepSizeParameter[0] = (float) Math.floor(stepSizeParameter[0]);
                app.updateStorageBuffer("ShaderParameters", 8, stepSizeParameter);
            }
        });

        app.processSteps(List.of(
                () -> {
                    app.setUIntUniform("Stage", 0);
                    app.setFloatUniform("Time", (float) currentTime);
                    app.setFloatUniform("Delta", (float) deltaTime);

                    int[] workGroupSize = app.getWorkGroupSize("AgentShader");
                    app.runComputeShader(numAgents / workGroupSize[0]);
                },

                () -> {
                    app.copyTexture("TrailMapOut", "TrailMap");
                    app.copyTexture("AgentMapOut", "AgentMap");
                },

                () -> {
                    app.setUIntUniform("Stage", 1);

                    int[] workGroupSize = app.getWorkGroupSize("AgentShader");
                    app.runComputeShader(textureWidth * textureHeight / workGroupSize[0]);
                },

                () -> {
                    app.copyTexture("TrailMapOut", "TrailMap");
                }));

        app.display("TrailMap");
    }

    public static void main(String[] args) {
        SlimeMain main = new SlimeMain();
        Application.launch(main.app);
    }

}
