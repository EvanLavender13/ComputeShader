package computeshader.core;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL15C.GL_DYNAMIC_COPY;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetProgramiv;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glGetUniformiv;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.GL33.glSamplerParameteri;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_WORK_GROUP_SIZE;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glGetProgramResourceIndex;
import static org.lwjgl.opengl.GL43.glShaderStorageBlockBinding;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;

import imgui.app.Application;
import imgui.app.Configuration;

public class ShaderApp extends Application {
    private static final Logger logger = LogManager.getLogger();

    private ShaderAppConfiguration shaderAppConfig;
    private List<Runnable> configurationSteps;
    private List<Runnable> preRunSteps;

    private Map<String, Integer> textureMap;
    private Map<String, Integer> textureBindingMap;
    private Map<String, Integer> samplerMap;
    private Map<String, Integer> vaoMap;
    private Map<String, Class<?>> uniformTypeMap;
    private Map<String, Integer> uniformLocationMap;
    private Map<String, Integer> storageBufferMap;
    private Map<String, Integer> storageBufferLocationMap;
    private Map<String, int[]> workGroupSizeMap;

    public ShaderApp(ShaderAppConfiguration shaderAppConfig) {
        this.shaderAppConfig = shaderAppConfig;

        configurationSteps = new ArrayList<>();
        preRunSteps = new ArrayList<>();

        textureMap = new HashMap<>();
        textureBindingMap = new HashMap<>();
        samplerMap = new HashMap<>();
        vaoMap = new HashMap<>();
        uniformTypeMap = new HashMap<>();
        uniformLocationMap = new HashMap<>();
        storageBufferMap = new HashMap<>();
        storageBufferLocationMap = new HashMap<>();
        workGroupSizeMap = new HashMap<>();
    }

    @Override
    public void configure(Configuration config) {
        logger.info("Configuring ShaderApp with {}", shaderAppConfig);
        config.setTitle(shaderAppConfig.title());
        config.setWidth(shaderAppConfig.width());
        config.setHeight(shaderAppConfig.height());

        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        this.colorBg.set(0.0f, 0.0f, 0.0f, 1.0f);

        logger.info("Running {} configuration step(s)", configurationSteps.size());
        configurationSteps.forEach(Runnable::run);
    }

    @Override
    public void preRun() {
        // add shutdown hook
        glfwSetKeyCallback(getHandle(), (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(getHandle(), true);
            }
        });

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glAlphaFunc(GL_GREATER, 0.1f);

        logger.info("Running {} pre-run step(s)", preRunSteps.size());
        preRunSteps.forEach(Runnable::run);
    }

    @Override
    public void process() {

    }

    public void createTexture(String name) {
        int textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, shaderAppConfig.width(), shaderAppConfig.height());
        glBindTexture(GL_TEXTURE_2D, 0);

        logger.debug("Created texture {} (id {})", name, textureId);
        textureMap.put(name, textureId);
    }

    public void createSampler(String name) {
        int samplerId = glGenSamplers();

        glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        logger.debug("Created sampler {} (id {})", name, samplerId);
        samplerMap.put(name, samplerId);
    }

    public void createVertexArrayObject(String name) {
        int vaoId = glGenVertexArrays();

        logger.debug("Created VAO {} (id {})", name, vaoId);
        vaoMap.put(name, vaoId);
    }

    public void addUniform(String name, Class<?> type) {
        uniformTypeMap.put(name, type);
        logger.debug("Added uniform {} (type {})", name, type);
    }

    public void createStorageBuffer(String name, float[] data) {
        int bufferId = glGenBuffers();

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, bufferId);
        glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_COPY);
        //glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, bufferId);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        logger.debug("Created {} storage buffer (id {}, bytes {})", name, bufferId, data.length * Float.BYTES);
        storageBufferMap.put(name, bufferId);
    }

    public void createComputeShader(String name, String filePath) throws IOException, URISyntaxException {
        int programId = glCreateProgram();
        logger.debug("Created {} program (id {})", name, programId);

        int shaderId = glCreateShader(GL_COMPUTE_SHADER);

        String computeShaderSource = Files.readString(Path.of(getClass().getResource("/compute.glsl").toURI()));
        glShaderSource(shaderId, computeShaderSource);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new IOException("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);
        glLinkProgram(programId);

        glUseProgram(programId);
        {
            IntBuffer workGroupSize = BufferUtils.createIntBuffer(3);
            glGetProgramiv(programId, GL_COMPUTE_WORK_GROUP_SIZE, workGroupSize);
            int workGroupSizeX = workGroupSize.get();
            int workGroupSizeY = workGroupSize.get();
            int workGroupSizeZ = workGroupSize.get();
            logger.debug("{} work group size: [x {}, y {}, z {}]", name, workGroupSizeX, workGroupSizeY,
                    workGroupSizeZ);
            workGroupSizeMap.put(name, new int[] { workGroupSizeX, workGroupSizeY, workGroupSizeZ });

            int numTextures = textureMap.size();
            IntBuffer textureUniformLocations = BufferUtils.createIntBuffer(numTextures);
            textureMap.keySet().forEach(textureName -> {
                glGetUniformiv(programId, glGetUniformLocation(programId, textureName), textureUniformLocations);
                textureBindingMap.put(textureName, textureUniformLocations.get());
            });
            logger.debug("{} texture uniform bindings: {}", name, textureBindingMap);

            uniformTypeMap.keySet().forEach(uniformName -> {
                uniformLocationMap.put(uniformName, glGetUniformLocation(programId, uniformName));
            });
            logger.debug("{} uniform locations: {}", name, uniformLocationMap);

            storageBufferMap.entrySet().forEach(bufferEntry -> {
                int bufferId = bufferEntry.getValue();
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, bufferId);
                int bufferLocation = glGetProgramResourceIndex(programId, GL_SHADER_STORAGE_BLOCK, bufferEntry.getKey());
                glShaderStorageBlockBinding(programId, bufferLocation, bufferLocation);
                logger.info(bufferLocation);
            });
        }
        glUseProgram(0);
    }

    public void addConfigurationStep(Runnable step) {
        configurationSteps.add(step);
    }

    public void addPreRunStep(Runnable step) {
        preRunSteps.add(step);
    }

    public static record ShaderAppConfiguration(String title, int width, int height) {
    }

}
