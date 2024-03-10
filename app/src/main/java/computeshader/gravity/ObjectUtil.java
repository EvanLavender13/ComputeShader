package computeshader.gravity;

import static java.lang.Math.random;

public class ObjectUtil {
    private static final int OBJECT_SIZE_F = 5;

    public static float[] createObjects(int n, int width, int height) {
        float[] objects = new float[n * OBJECT_SIZE_F];

        objects[0 * OBJECT_SIZE_F + 0] = (float) width / 2.0f;
        objects[0 * OBJECT_SIZE_F + 1] = (float) height / 2.0f;
        objects[0 * OBJECT_SIZE_F + 2] = 0.0f;
        objects[0 * OBJECT_SIZE_F + 3] = 0.0f;
        objects[0 * OBJECT_SIZE_F + 4] = 10000.0f;

        for (int i = 1; i < n; i++) {
            objects[i * OBJECT_SIZE_F + 0] = (float) (random() * width);
            objects[i * OBJECT_SIZE_F + 1] = (float) (random() * height);
            objects[i * OBJECT_SIZE_F + 2] = (float) (0.5 - random()) * 2.0f * 200.0f;
            objects[i * OBJECT_SIZE_F + 3] = (float) (0.5 - random()) * 2.0f * 200.0f;
            objects[i * OBJECT_SIZE_F + 4] = (float) random() * 10.0f;
        }

        return objects;
    }

}
