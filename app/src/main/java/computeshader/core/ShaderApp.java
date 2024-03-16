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
import static org.lwjgl.glfw.GLFW.glfwGetTime;
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
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL15C.GL_DYNAMIC_COPY;
import static org.lwjgl.opengl.GL15C.GL_READ_WRITE;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glBufferSubData;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
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
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20C.glUniform1f;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30C.glBindBufferBase;
import static org.lwjgl.opengl.GL30C.glBindFragDataLocation;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glUniform1ui;
import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.GL33.glSamplerParameteri;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_WORK_GROUP_SIZE;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BLOCK;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glCopyImageSubData;
import static org.lwjgl.opengl.GL43.glDispatchCompute;
import static org.lwjgl.opengl.GL43.glGetProgramResourceIndex;
import static org.lwjgl.opengl.GL43.glShaderStorageBlockBinding;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import imgui.app.Application;
import imgui.app.Configuration;

public class ShaderApp extends Application {
    private static final Logger logger = LogManager.getLogger();

    private ShaderAppConfiguration shaderAppConfig;

    private Runnable configuration;
    private Runnable preRun;
    private Runnable gui;
    private List<Runnable> processSteps;

    private Map<String, Integer> textureMap;
    private Map<String, Integer> textureBindingMap;
    private String displayTexture;
    private int samplerId;
    private int vertexArrayObjectId;
    private Map<String, Integer> computeShaderMap;
    private Map<String, Integer> uniformLocationMap;
    private Map<String, Integer> storageBufferMap;
    private Map<String, int[]> workGroupSizeMap;

    private int displayShaderProgramId;

    public ShaderApp(ShaderAppConfiguration shaderAppConfig) {
        this.shaderAppConfig = shaderAppConfig;

        configuration = () -> {
        };
        preRun = () -> {
        };
        gui = () -> {
        };
        processSteps = new ArrayList<>();

        textureMap = new HashMap<>();
        textureBindingMap = new HashMap<>();
        computeShaderMap = new HashMap<>();
        uniformLocationMap = new HashMap<>();
        storageBufferMap = new HashMap<>();
        workGroupSizeMap = new HashMap<>();
    }

    @Override
    public void configure(Configuration config) {
        logger.info("Configuring ShaderApp with {}", shaderAppConfig);
        config.setTitle(shaderAppConfig.title());
        config.setWidth(shaderAppConfig.windowWidth);
        config.setHeight(shaderAppConfig.windowHeight);

        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        this.colorBg.set(0.0f, 0.0f, 0.0f, 1.0f);

        logger.info("Running 'configuration' step");
        configuration.run();
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

        logger.info("Running 'preRun' step");
        preRun.run();

        samplerId = glGenSamplers();
        glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        vertexArrayObjectId = glGenVertexArrays();

        try {
            createDisplayShader();
        } catch (IOException | URISyntaxException e) {
            logger.error("Exception caught when creating shaders!", e);
        }
    }

    @Override
    public void process() {
        if (processSteps.isEmpty()) {
            return;
        }

        gui.run();

        textureBindingMap.entrySet().forEach(entry -> {
            glBindImageTexture(entry.getValue(), textureMap.get(entry.getKey()), 0, false, 0, GL_READ_WRITE,
                    GL_RGBA32F);
        });

        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        processSteps.forEach(step -> {
            step.run();
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        });

        glUseProgram(displayShaderProgramId);
        {
            glBindVertexArray(vertexArrayObjectId);
            glBindTexture(GL_TEXTURE_2D, textureMap.get(displayTexture));
            glBindSampler(0, samplerId);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindSampler(0, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
            glBindVertexArray(0);
        }
        glUseProgram(0);

        textureBindingMap.entrySet().forEach(entry -> {
            glBindImageTexture(entry.getValue(), 0, 0, false, 0, 0, 0);
        });
    }

    public void createTexture(String name) {
        int textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, shaderAppConfig.textureWidth, shaderAppConfig.textureHeight);
        glBindTexture(GL_TEXTURE_2D, 0);

        logger.debug("Created texture {} (id {})", name, textureId);
        textureMap.put(name, textureId);
    }

    public void copyTexture(String from, String to) {
        glCopyImageSubData(textureMap.get(from), GL_TEXTURE_2D, 0, 0, 0, 0,
                textureMap.get(to), GL_TEXTURE_2D, 0, 0, 0, 0,
                shaderAppConfig.textureWidth, shaderAppConfig.textureHeight, 1);
    }

    public void addUniform(String name) {
        uniformLocationMap.put(name, -1);
        logger.debug("Added uniform {}", name);
    }

    public void setUIntUniform(String name, int value) {
        glUniform1ui(uniformLocationMap.get(name), value);
    }

    public void setFloatUniform(String name, float value) {
        glUniform1f(uniformLocationMap.get(name), value);
    }

    public void setVector3fUniform(String name, Vector3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = value.get(stack.mallocFloat(3));
            glUniform3fv(uniformLocationMap.get(name), buffer);
        }        
    }

    public void setMatrix4fUniform(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = value.get(stack.mallocFloat(16));
            glUniformMatrix4fv(uniformLocationMap.get(name), false, buffer);
        }
    }

    public void createStorageBuffer(String programName, String name, float[] data) {
        int computeProgramShaderId = computeShaderMap.get(programName);

        glUseProgram(computeProgramShaderId);
        {
            int bufferId = glGenBuffers();

            int bufferLocation = glGetProgramResourceIndex(computeProgramShaderId, GL_SHADER_STORAGE_BLOCK, name);
            glShaderStorageBlockBinding(computeProgramShaderId, bufferLocation, bufferLocation);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, bufferId);
            glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_DYNAMIC_COPY);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bufferLocation, bufferId);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            logger.debug("Created {} storage buffer (id {}, {} bytes)", name, bufferId, data.length * Float.BYTES);
            storageBufferMap.put(name, bufferId);
        }
        glUseProgram(0);
    }

    public void updateStorageBuffer(String name, int index, float[] data) {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, storageBufferMap.get(name));
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, index * Float.BYTES, data);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void createComputeShader(String name, String filePath) {
        int computeProgramShaderId = glCreateProgram();
        logger.debug("Created {} program (id {})", name, computeProgramShaderId);
        computeShaderMap.put(name, computeProgramShaderId);

        int shaderId = glCreateShader(GL_COMPUTE_SHADER);

        try {
            String computeShaderSource = Files.readString(Path.of(getClass().getResource(filePath).toURI()));
            glShaderSource(shaderId, computeShaderSource);
            glCompileShader(shaderId);
        } catch (IOException | URISyntaxException e) {
            logger.error("Exception caught when creating shaders!", e);
        }

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            logger.error("Error compiling Shader code: {}", glGetShaderInfoLog(shaderId, 1024));
            return;
        }

        glAttachShader(computeProgramShaderId, shaderId);
        glLinkProgram(computeProgramShaderId);

        glUseProgram(computeProgramShaderId);
        {
            IntBuffer workGroupSize = BufferUtils.createIntBuffer(3);
            glGetProgramiv(computeProgramShaderId, GL_COMPUTE_WORK_GROUP_SIZE, workGroupSize);
            int workGroupSizeX = workGroupSize.get();
            int workGroupSizeY = workGroupSize.get();
            int workGroupSizeZ = workGroupSize.get();
            logger.debug("  {} work group size: [x {}, y {}, z {}]", name, workGroupSizeX, workGroupSizeY,
                    workGroupSizeZ);
            workGroupSizeMap.put(name, new int[] { workGroupSizeX, workGroupSizeY, workGroupSizeZ });

            int numTextures = textureMap.size();
            IntBuffer textureUniformLocations = BufferUtils.createIntBuffer(numTextures);
            textureMap.keySet().forEach(textureName -> {
                glGetUniformiv(computeProgramShaderId, glGetUniformLocation(computeProgramShaderId, textureName),
                        textureUniformLocations);
                textureBindingMap.put(textureName, textureUniformLocations.get());
            });
            logger.debug("  {} texture uniform bindings: {}", name, textureBindingMap);

            uniformLocationMap.entrySet().forEach(entry -> {
                entry.setValue(glGetUniformLocation(computeProgramShaderId, entry.getKey()));
            });
            logger.debug("  {} uniform locations: {}", name, uniformLocationMap);

            storageBufferMap.entrySet().forEach(bufferEntry -> {
                int bufferId = bufferEntry.getValue();
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, bufferId);
                int bufferLocation = glGetProgramResourceIndex(computeProgramShaderId, GL_SHADER_STORAGE_BLOCK,
                        bufferEntry.getKey());
                glShaderStorageBlockBinding(computeProgramShaderId, bufferLocation, bufferLocation);
            });
        }
        glUseProgram(0);
    }

    public int[] getWorkGroupSize(String name) {
        return workGroupSizeMap.get(name);
    }

    public void runComputeShader(int x, int y, int z) {
        glDispatchCompute(x, y, z);
    }

    public void runComputeShader(int x) {
        runComputeShader(x, 1, 1);
    }

    public void runComputeShader(int x, int y) {
        runComputeShader(x, y, 1);
    }

    private void createDisplayShader() throws IOException, URISyntaxException {
        displayShaderProgramId = glCreateProgram();
        logger.debug("Created DisplayShader program (id {})", displayShaderProgramId);

        int displayVertShaderId = createDisplayVertexShader();
        int displayFragShaderId = createDisplayFragmentShader();
        glAttachShader(displayShaderProgramId, displayVertShaderId);
        glAttachShader(displayShaderProgramId, displayFragShaderId);
        glBindFragDataLocation(displayShaderProgramId, 0, "color");
        glLinkProgram(displayShaderProgramId);
    }

    private int createDisplayVertexShader() throws IOException, URISyntaxException {
        int displayVertShaderId = glCreateShader(GL_VERTEX_SHADER);
        logger.debug("Created DisplayVertexShader (id {})", displayVertShaderId);

        String displayVertShaderSource = Files
                .readString(Path.of(getClass().getResource("/display.vert.glsl").toURI()));

        glShaderSource(displayVertShaderId, displayVertShaderSource);
        glCompileShader(displayVertShaderId);

        if (glGetShaderi(displayVertShaderId, GL_COMPILE_STATUS) == 0) {
            throw new IOException("Error compiling Shader code: " + glGetShaderInfoLog(displayVertShaderId, 1024));
        }

        return displayVertShaderId;
    }

    private int createDisplayFragmentShader() throws IOException, URISyntaxException {
        int displayFragShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        logger.debug("Created DisplayFragmentShader (id {})", displayFragShaderId);

        String displayFragShaderSource = Files
                .readString(Path.of(getClass().getResource("/display.frag.glsl").toURI()));

        glShaderSource(displayFragShaderId, displayFragShaderSource);
        glCompileShader(displayFragShaderId);

        if (glGetShaderi(displayFragShaderId, GL_COMPILE_STATUS) == 0) {
            throw new IOException("Error compiling Shader code: " + glGetShaderInfoLog(displayFragShaderId, 1024));
        }

        return displayFragShaderId;
    }

    public void configuration(Runnable step) {
        configuration = step;
    }

    public void preRun(Runnable step) {
        preRun = step;
    }

    public void gui(Runnable step) {
        gui = step;
    }

    public void processSteps(List<Runnable> steps) {
        processSteps = steps;
    }

    public void usingProgram(String program, Runnable step) {
        int programId = computeShaderMap.get(program);
        glUseProgram(programId);
        step.run();
        glUseProgram(0);
    }

    public void display(String textureName) {
        displayTexture = textureName;
    }

    public float getTime() {
        return (float) glfwGetTime();
    }

    public static record ShaderAppConfiguration(String title, int windowWidth, int windowHeight, int textureWidth,
            int textureHeight) {
    }

}
