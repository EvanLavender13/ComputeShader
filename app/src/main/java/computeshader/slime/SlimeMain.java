package computeshader.slime;

import static java.lang.Math.pow;
import static java.lang.Math.toRadians;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import computeshader.core.ShaderApp;
import computeshader.core.ShaderApp.ShaderAppConfiguration;
import imgui.app.Application;

public class SlimeMain {
    private static final Logger logger = LogManager.getLogger();

    private ShaderApp shaderApp;

    private int numAgents;
    private float[] sensingDistanceParameter;
    private float[] sensingAngleParameter;
    private float[] turningAngleParameter;
    private float[] depositAmountParameter;
    private float[] decayAmountParameter;
    private float[] stepSizeParameter;

    private String title = "Slime";
    private int width = 1600;
    private int height = 900;

    public SlimeMain() {
        shaderApp = new ShaderApp(new ShaderAppConfiguration(title, width, height));
        shaderApp.addConfigurationStep(createConfigFunction());
        shaderApp.addPreRunStep(createPreRunFunction());
    }

    private Runnable createConfigFunction() {
        return () -> {
            logger.info("Configuring SlimeMain");
            numAgents = (int) pow(2, 24);

            sensingDistanceParameter = new float[1];
            sensingDistanceParameter[0] = 10.0f;

            sensingAngleParameter = new float[1];
            sensingAngleParameter[0] = (float) toRadians(45.0f);

            turningAngleParameter = new float[1];
            turningAngleParameter[0] = (float) toRadians(45.0f);

            depositAmountParameter = new float[1];
            depositAmountParameter[0] = 0.1f;

            decayAmountParameter = new float[1];
            decayAmountParameter[0] = 0.1f;

            stepSizeParameter = new float[1];
            stepSizeParameter[0] = 1.0f;
        };
    }

    private Runnable createPreRunFunction() {
        return () -> {
            logger.info("Creating Textures");
            shaderApp.createTexture("AgentMap");
            shaderApp.createTexture("AgentMapOut");
            shaderApp.createTexture("TrailMap");
            shaderApp.createTexture("TrailMapOut");

            logger.info("Creating Sampler");
            shaderApp.createSampler("Sampler");

            logger.info("Creating VAO");
            shaderApp.createVertexArrayObject("VAO");

            logger.info("Adding Uniforms");
            shaderApp.addUniform("Stage", Integer.class);
            shaderApp.addUniform("Time", Float.class);

            logger.info("Creating Storage Buffer");
            float[] shaderParameters = new float[] {
                    (float) width,
                    (float) height,
                    sensingDistanceParameter[0],
                    sensingAngleParameter[0],
                    turningAngleParameter[0],
                    depositAmountParameter[0],
                    decayAmountParameter[0],
                    stepSizeParameter[0]
            };
            shaderApp.createStorageBuffer("ShaderParameters", shaderParameters);
            shaderApp.createStorageBuffer("Agents", shaderParameters);

            try {
                logger.info("Creating agent shader");
                shaderApp.createComputeShader("AgentShader", "/compute.glsl");
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        };
    }

    public ShaderApp getApp() {
        return shaderApp;
    }

    public static void main(String[] args) {
        SlimeMain main = new SlimeMain();
        Application.launch(main.getApp());
    }

}
