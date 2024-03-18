package computeshader.raytracer;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RAW_MOUSE_MOTION;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector3f;

import computeshader.core.ShaderApp;
import computeshader.core.ShaderApp.ShaderAppConfiguration;
import imgui.ImGui;
import imgui.app.Application;

public class RayTracer {
    private static final Logger logger = LogManager.getLogger();

    private ShaderApp app;
    private long windowHandle;

    private String title = "RayTracer";
    private int windowWidth = 1600;
    private int windowHeight = 900;
    private float aspectRatio = (float) windowWidth / windowHeight;
    private int textureWidth = 1024;
    private int textureHeight = (int) (textureWidth / aspectRatio);

    private double currentTime = 0.0f;
    private double previousFrameTime = 0.0;
    private float deltaTime = 0.0f;

    private Camera camera;
    private float mouseScroll = 0.0f;
    private double[] xMouse = new double[1];
    private double[] yMouse = new double[1];

    private Scene scene;
    private int[] numSamples = new int[] { 10 };

    public RayTracer() {
        app = new ShaderApp(new ShaderAppConfiguration(title, windowWidth, windowHeight, textureWidth, textureHeight));

        app.configuration(() -> {
            camera = new Camera(textureWidth, textureHeight);

            scene = new Scene();
            int ground = scene.addMaterial(new Material(new Vector3f(0.5f, 0.5f, 0.5f), new float[] { 1.0f }, new float[] { 1.0f }, 0.0f));
            scene.addSphere(new Sphere(new float[] { 0.0f, -1000.0f, 0.0f }, 1000.0f, ground));

            Random random = new Random();

            int n = 5;

            for (int i = -n; i < n; i++) {
                for (int j = -n; j < n; j++) {
                    Vector3f albedo = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
                    int material = scene.addMaterial(new Material(albedo, new float[] { random.nextFloat() }, new float[] { random.nextFloat() }, 0.0f));

                    float[] center = new float[] { i + 0.9f * random.nextFloat(), 0.2f /*+ random.nextInt(10)*/, j + 0.9f * random.nextFloat() };
                    scene.addSphere(new Sphere(center, 0.2f, material));
                }
            }

            // logger.info("Created {}", scene);
            logger.info("Scene materials: {}", scene.materials.size());
            logger.info("Scene spheres: {}", scene.spheres.size());
        });

        app.preRun(() -> {
            app.createTexture("Image");
            // app.createTexture("ImageOut");

            app.addUniform("CameraPosition");
            app.addUniform("InvProjection");
            app.addUniform("InvView");
            app.addUniform("Time");
            app.addUniform("Samples");

            app.createComputeShader("RayShader", "/raytrace.glsl");

            app.createStorageBuffer("RayShader", "ShaderParameters", new float[] {
                    (float) textureWidth,
                    (float) textureHeight,
            });

            app.createStorageBuffer("RayShader", "MaterialParameters", scene.getMaterialParameters());
            app.createStorageBuffer("RayShader", "SphereParameters", scene.getSphereParameters());

            windowHandle = app.getHandle();

            glfwSetScrollCallback(windowHandle, (window, dx, dy) -> {
                mouseScroll = (float) -dy;
            });
        });

        app.gui(() -> {
            currentTime = app.getTime();
            deltaTime = (float) (currentTime - previousFrameTime);
            previousFrameTime = currentTime;

            ImGui.text("Runtime: " + currentTime);
            ImGui.text("Delta  : " + deltaTime);

            if (ImGui.sliderAngle("Vertical FoV", camera.getVerticalFov())) {
                camera.calculateProjection();
            }

            if (ImGui.sliderInt("Samples", numSamples, 1, 100)) {
                
            }            

            if (ImGui.treeNode("Scene")) {
                if (ImGui.treeNode("Materials")) {
                    for (int i = 0; i < scene.materials.size(); i++) {
                        Material material = scene.materials.get(i);
                        if (ImGui.treeNode("Material " + i)) {
                            if (ImGui.sliderFloat("Roughness", material.roughness, 0.0f, 1.0f)) {
                                app.updateStorageBuffer("MaterialParameters", 6 * i + 3, material.roughness);
                            }
                            if (ImGui.sliderFloat("Metallic", material.metallic, 0.0f, 1.0f)) {
                                app.updateStorageBuffer("MaterialParameters", 6 * i + 4, material.metallic);
                            }                            

                            ImGui.treePop();
                        }
                    }
                    ImGui.treePop();
                }
                if (ImGui.treeNode("Spheres")) {
                    for (int i = 0; i < scene.spheres.size(); i++) {
                        if (ImGui.treeNode("Sphere " + i)) {
                            Sphere sphere = scene.spheres.get(i);
                            if (ImGui.dragFloat3("Center", sphere.position)) {
                                app.updateStorageBuffer("SphereParameters", 5 * i + 0, sphere.position);
                            };

                            ImGui.treePop();
                        }
                    }

                    ImGui.treePop();
                }

                ImGui.treePop();
            }
        });

        app.processSteps(List.of(() -> {
            if (ImGui.getIO().getWantCaptureMouse()) {
                return;
            }

            camera.zoom(mouseScroll);
            mouseScroll = 0.0f;

            float lastMouseX = (float) xMouse[0];
            float lastMouseY = (float) yMouse[0];
            glfwGetCursorPos(windowHandle, xMouse, yMouse);
            float currMouseX = (float) xMouse[0];
            float currMouseY = (float) yMouse[0];
            float deltaX = currMouseX - lastMouseX;
            float deltaY = currMouseY - lastMouseY;

            if (!(glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS)) {
                glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
                glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                return;
            }

            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);

            camera.orbit(deltaX * 0.1f, deltaY * 0.1f);
        }, () -> {
            camera.update();

            app.usingProgram("RayShader", () -> {
                app.setVector3fUniform("CameraPosition", camera.getPosition());
                app.setMatrix4fUniform("InvProjection", camera.getInvProjection());
                app.setMatrix4fUniform("InvView", camera.getInvView());
                app.setFloatUniform("Time", (float) currentTime);
                app.setUIntUniform("Samples", numSamples[0]);

                int[] workGroupSize = app.getWorkGroupSize("RayShader");
                app.runComputeShader(textureWidth / workGroupSize[0], textureHeight / workGroupSize[1]);
            });
        }));

        app.display("Image");
    }

    public static void main(String[] args) {
        RayTracer main = new RayTracer();
        Application.launch(main.app);
    }

    private class Camera {
        private Matrix4f projection = new Matrix4f();
        private Matrix4f view = new Matrix4f();
        private Matrix4f invProjection = new Matrix4f();
        private Matrix4f invView = new Matrix4f();

        private float[] verticalFov = new float[] { Math.toRadians(45.0f) };
        private float nearClip = 0.1f;
        private float farClip = 100.0f;
        private Vector3f center = new Vector3f(0.0f, 0.0f, 0.0f);
        private Vector3f position = new Vector3f(0.0f, 0.0f, 0.0f);
        private Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
        private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        private float distance = 10.0f;
        private float latitude = 0.0f;
        private float longitude = 0.0f;
        private Vector3f euclidean = new Vector3f();
        private int viewportWidth;
        private int viewportHeight;

        private Camera(int viewportWidth, int viewportHeight) {
            this.viewportWidth = viewportWidth;
            this.viewportHeight = viewportHeight;

            calculateProjection();
            update();

            logger.info("Created {}", this);
        }

        private void update() {
            calculatePosition();
            calculateView();
        }

        private void calculatePosition() {
            calculateEuclidean();

            center.add(euclidean.mul(distance), position);
        }

        private void calculateView() {
            view.setLookAt(position, center, up);
            view.invert(invView);
        }

        private void calculateProjection() {
            float aspectRatio = viewportWidth / viewportHeight;
            projection.setPerspective(verticalFov[0], aspectRatio, nearClip, farClip);
            projection.invert(invProjection);
        }

        private void calculateEuclidean() {
            float latitudeRad = Math.toRadians(latitude);
            float longitudeRad = Math.toRadians(longitude);
            euclidean.set(Math.cos(latitudeRad) * Math.sin(longitudeRad), Math.sin(latitudeRad),
                    Math.cos(latitudeRad) * Math.cos(longitudeRad));
        }

        private void orbit(float deltaX, float deltaY) {
            longitude -= deltaX;

            if (longitude < 0.0f) {
                longitude += 360.0f;
            }

            if (longitude > 360.0f) {
                longitude -= 360.0f;
            }

            latitude = Math.clamp(-85.0f, 85.0f, latitude + deltaY);
        }

        private void zoom(float zoom) {
            distance = Math.clamp(2.0f, 100.0f, distance + zoom);
        }

        private Vector3f getPosition() {
            return position;
        }

        private Matrix4f getInvProjection() {
            return invProjection;
        }

        private Matrix4f getInvView() {
            return invView;
        }

        private float[] getVerticalFov() {
            return verticalFov;
        }

        @Override
        public String toString() {
            return "Camera [verticalFov=" + verticalFov + ", nearClip=" + nearClip + ", farClip=" + farClip
                    + ", forward=" + forward + ", up=" + up + ", viewportWidth=" + viewportWidth + ", viewportHeight="
                    + viewportHeight + "]";
        }
    }

    private record Material(Vector3f albedo, float[] roughness, float[] metallic, float refraction) {
    }

    private record Sphere(float[] position, float radius, int materialIndex) {
    }

    private class Scene {
        private List<Material> materials;
        private List<Sphere> spheres;

        public Scene() {
            materials = new ArrayList<>();
            spheres = new ArrayList<>();
        }

        public int addMaterial(Material material) {
            materials.add(material);
            return materials.indexOf(material);
        }

        public void addSphere(Sphere sphere) {
            spheres.add(sphere);
        }

        public float[] getMaterialParameters() {
            int size = 6;
            float[] params = new float[size * materials.size()];
            for (int i = 0; i < materials.size(); i++) {
                Material material = materials.get(i);
                Vector3f albedo = material.albedo;
                params[size * i + 0] = albedo.x;
                params[size * i + 1] = albedo.y;
                params[size * i + 2] = albedo.z;
                params[size * i + 3] = material.roughness[0];
                params[size * i + 4] = material.metallic[0];
                params[size * i + 5] = material.refraction;
            }

            return params;
        }

        public float[] getSphereParameters() {
            float[] params = new float[5 * spheres.size()];
            for (int i = 0; i < spheres.size(); i++) {
                Sphere sphere = spheres.get(i);
                float[] position = sphere.position;
                params[5 * i + 0] = position[0];
                params[5 * i + 1] = position[1];
                params[5 * i + 2] = position[2];
                params[5 * i + 3] = sphere.radius;
                params[5 * i + 4] = sphere.materialIndex;
            }

            return params;
        }

        @Override
        public String toString() {
            return "Scene [materials=" + materials + ", spheres=" + spheres + "]";
        }
    }
}
