package computeshader.raytracer;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RAW_MOUSE_MOTION;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Math;
import org.joml.Matrix4f;
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
    private int windowWidth = 900;
    private int windowHeight = 900;
    private int textureWidth = 900;
    private int textureHeight = 900;

    private double currentTime = 0.0f;
    private double previousFrameTime = 0.0;
    private float deltaTime = 0.0f;

    private Camera camera;
    private float mouseScroll = 0.0f;
    private boolean mouseLocked = false;
    private double[] xMouse = new double[1];
    private double[] yMouse = new double[1];

    public RayTracer() {
        app = new ShaderApp(new ShaderAppConfiguration(title, windowWidth, windowHeight, textureWidth, textureHeight));

        app.configuration(() -> {
            camera = new Camera(textureWidth, textureHeight);
        });

        app.preRun(() -> {
            app.createTexture("Image");
            // app.createTexture("ImageOut");

            app.addUniform("CameraPosition");
            app.addUniform("InvProjection");
            app.addUniform("InvView");

            app.createComputeShader("RayShader", "/raytrace.glsl");

            app.createStorageBuffer("RayShader", "ShaderParameters", new float[] {
                    (float) textureWidth,
                    (float) textureHeight,
            });

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
        });

        app.processSteps(List.of(() -> {
            if (!mouseLocked && glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
                mouseLocked = true;
                glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            }

            if (mouseLocked && glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_1) == GLFW_RELEASE) {
                mouseLocked = false;
                glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
                glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }

            float lastMouseX = (float) xMouse[0];
            float lastMouseY = (float) yMouse[0];
            glfwGetCursorPos(windowHandle, xMouse, yMouse);
            float currMouseX = (float) xMouse[0];
            float currMouseY = (float) yMouse[0];
            if (mouseLocked) {
                float deltaX = currMouseX - lastMouseX;
                float deltaY = currMouseY - lastMouseY;
                camera.orbit(deltaX, deltaY);
            }

            camera.zoom(mouseScroll);
            mouseScroll = 0.0f;
        }, () -> {
            camera.update();

            app.usingProgram("RayShader", () -> {
                app.setVector3fUniform("CameraPosition", camera.getPosition());
                app.setMatrix4fUniform("InvProjection", camera.getInvProjection());
                app.setMatrix4fUniform("InvView", camera.getInvView());
                app.runComputeShader(textureWidth, textureHeight);
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

        private float verticalFov = Math.toRadians(45.0f);
        private float nearClip = 0.1f;
        private float farClip = 100.0f;
        private Vector3f center = new Vector3f();
        private Vector3f position = new Vector3f(0.0f, 0.0f, 0.0f);
        private Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
        private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        private float distance = 3.0f;
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

        private void calculateRays() {
            // TODO: do in shader
        }

        private void calculateProjection() {
            float aspectRatio = viewportWidth / viewportHeight;
            projection.setPerspective(verticalFov, aspectRatio, nearClip, farClip);
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

        @Override
        public String toString() {
            return "Camera [verticalFov=" + verticalFov + ", nearClip=" + nearClip + ", farClip=" + farClip
                    + ", forward=" + forward + ", up=" + up + ", viewportWidth=" + viewportWidth + ", viewportHeight="
                    + viewportHeight + "]";
        }
    }
}
