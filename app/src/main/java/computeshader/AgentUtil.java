package computeshader;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.random;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.awt.Color;
import java.util.Random;

public class AgentUtil {
    private static final int AGENT_SIZE_F = 6;

    public static float[] nAgentsOneColor(int n, int width, int height, Color color) {
        float[] agents = new float[n * AGENT_SIZE_F];

        for (int i = 0; i < n; i++) {
            agents[i * AGENT_SIZE_F + 0] = (float) (random() * width);
            agents[i * AGENT_SIZE_F + 1] = (float) (random() * height);
            agents[i * AGENT_SIZE_F + 2] = (float) (random() * 360.0);

            // color
            agents[i * AGENT_SIZE_F + 3] = color.getRed() / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = color.getGreen() / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = color.getBlue() / 255.0f;
        }

        return agents;
    }

    public static float[] nAgents(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        Random random = new Random();
        Color colorA = randomColor(random);
        Color colorB = randomColor(random);
        Color colocC = randomColor(random);

        // Color colorA = Color.CYAN;
        // Color colorB = new Color(1.0f, 0.0f, 1.0f);
        // Color colocC = Color.MAGENTA;

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        for (int i = 0; i < n; i++) {
            float radius = min(width, height) / 8.0f * (float) sqrt(random());
            float theta = (float) (random() * 2 * PI);
            float x = (float) (centerX + radius * cos(theta));
            float y = (float) (centerY + radius * sin(theta));

            agents[i * AGENT_SIZE_F + 0] = x;
            agents[i * AGENT_SIZE_F + 1] = y;
            agents[i * AGENT_SIZE_F + 2] = (float) (Math.random() * 360.0);

            float t = ((float) i / n) * 2.0f;
            float r, g, b;
            if (t <= 1.0f) {
                r = colorA.getRed() * t + colorB.getRed() * (1.0f - t);
                g = colorA.getGreen() * t + colorB.getGreen() * (1.0f - t);
                b = colorA.getBlue() * t + colorB.getBlue() * (1.0f - t);
            } else {
                r = colorB.getRed() * (t - 1.0f) + colocC.getRed() * (2.0f - t);
                g = colorB.getGreen() * (t - 1.0f) + colocC.getGreen() * (2.0f - t);
                b = colorB.getBlue() * (t - 1.0f) + colocC.getBlue() * (2.0f - t);
            }

            // color
            agents[i * AGENT_SIZE_F + 3] = r / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = g / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = b / 255.0f;
        }

        return agents;
    }

    // https://stackoverflow.com/a/8739276
    private static Color randomColor(Random random) {
        final float hue = random.nextFloat();
        final float saturation = 1.0f;// 1.0 for brilliant, 0.0 for dull
        final float luminance = 0.5f; // 1.0 for brighter, 0.0 for black
        // Saturation between 0.1 and 0.3
        // final float saturation = (random.nextInt(2000) + 1000) / 10000f;
        // final float luminance = 0.9f;
        return Color.getHSBColor(hue, saturation, luminance);
    }

}
