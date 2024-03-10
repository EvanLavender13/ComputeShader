package computeshader.slime;

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

    public static float[] nAgentsSatGradient(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        float hue = (float) Math.random();
        float brightness = 1.0f;

        for (int i = 0; i < n; i++) {
            float radius = min(width, height) / 256.0f * (float) sqrt(random());
            float theta = (float) (random() * 2 * PI);
            float x = (float) (centerX + radius * cos(theta));
            float y = (float) (centerY + radius * sin(theta));

            // position + rotation
            agents[i * AGENT_SIZE_F + 0] = x;
            agents[i * AGENT_SIZE_F + 1] = y;
            agents[i * AGENT_SIZE_F + 2] = (float) (random() * 360.0);

            float t = ((float) i / n);
            float saturation = t;

            Color color = Color.getHSBColor(hue, saturation, brightness);

            // color
            agents[i * AGENT_SIZE_F + 3] = color.getRed() / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = color.getGreen() / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = color.getBlue() / 255.0f;
        }

        return agents;
    }

    public static float[] nAgentsHueGradient(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        float saturation = 1.0f;
        float brightness = 1.0f;

        for (int i = 0; i < n; i++) {
            float radius = min(width, height) / 256.0f * (float) sqrt(random());
            float theta = (float) (random() * 2 * PI);
            float x = (float) (centerX + radius * cos(theta));
            float y = (float) (centerY + radius * sin(theta));

            // position + rotation
            agents[i * AGENT_SIZE_F + 0] = x;
            agents[i * AGENT_SIZE_F + 1] = y;
            agents[i * AGENT_SIZE_F + 2] = (float) (random() * 360.0);

            float t = ((float) i / n);
            float hue = t;

            Color color = Color.getHSBColor(hue, saturation, brightness);

            // color
            agents[i * AGENT_SIZE_F + 3] = color.getRed() / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = color.getGreen() / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = color.getBlue() / 255.0f;
        }

        return agents;
    }

    public static float[] nAgentsRainbow(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        Color[] colors = new Color[] {
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA
        };

        Random random = new Random();

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        for (int i = 0; i < n; i++) {
            float radius = min(width, height) / 256.0f * (float) sqrt(random());
            float theta = (float) (random() * 2 * PI);
            float x = (float) (centerX + radius * cos(theta));
            float y = (float) (centerY + radius * sin(theta));

            // position + rotation
            agents[i * AGENT_SIZE_F + 0] = x;
            agents[i * AGENT_SIZE_F + 1] = y;
            agents[i * AGENT_SIZE_F + 2] = (float) (random() * 360.0);

            Color color = colors[random.nextInt(colors.length)];

            // color
            agents[i * AGENT_SIZE_F + 3] = color.getRed() / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = color.getGreen() / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = color.getBlue() / 255.0f;
        }

        return agents;
    }

    public static float[] nAgents(int n, int width, int height, Color color) {
        float[] agents = new float[n * AGENT_SIZE_F];

        for (int i = 0; i < n; i++) {
            // position + rotation
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

    public static float[] nAgentsRandomColor(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        Random random = new Random();

        for (int i = 0; i < n; i++) {
            // position + rotation
            agents[i * AGENT_SIZE_F + 0] = (float) (random() * width);
            agents[i * AGENT_SIZE_F + 1] = (float) (random() * height);
            agents[i * AGENT_SIZE_F + 2] = (float) (random() * 360.0);

            Color color = randomColor(random);

            // color
            agents[i * AGENT_SIZE_F + 3] = color.getRed() / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = color.getGreen() / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = color.getBlue() / 255.0f;
        }

        return agents;
    }

    public static float[] nAgentsGradient(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        // Random random = new Random();
        // Color colorA = randomColor(random);
        // Color colorB = randomColor(random);

        Color colorA = Color.RED;
        Color colorB = Color.BLUE;

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        for (int i = 0; i < n; i++) {
            float radius = min(width, height) / 64.0f * (float) sqrt(random());
            float theta = (float) (random() * 2 * PI);
            float x = (float) (centerX + radius * cos(theta));
            float y = (float) (centerY + radius * sin(theta));

            // position + rotation
            agents[i * AGENT_SIZE_F + 0] = x;
            agents[i * AGENT_SIZE_F + 1] = y;
            agents[i * AGENT_SIZE_F + 2] = (float) (Math.random() * 360.0);

            float t = ((float) i / n);
            float r, g, b;
            r = colorA.getRed() * t + colorB.getRed() * (1.0f - t);
            g = colorA.getGreen() * t + colorB.getGreen() * (1.0f - t);
            b = colorA.getBlue() * t + colorB.getBlue() * (1.0f - t);

            // color
            agents[i * AGENT_SIZE_F + 3] = r / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = g / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = b / 255.0f;
        }

        return agents;
    }

    public static float[] nAgentsRandom4Colors(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        Random random = new Random();
        Color colorA = randomColor(random);
        Color colorB = randomColor(random);
        Color colorC = randomColor(random);
        Color colorD = randomColor(random);

        // Color colorA = Color.RED;
        // Color colorB = Color.BLUE;
        // Color colorC = Color.RED;
        // Color colorD = Color.WHITE;

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        for (int i = 0; i < n; i++) {
            float radius = min(width, height) / 64.0f * (float) sqrt(random());
            float theta = (float) (random() * 2 * PI);
            float x = (float) (centerX + radius * cos(theta));
            float y = (float) (centerY + radius * sin(theta));

            // position + rotation
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
                r = colorC.getRed() * (t - 1.0f) + colorD.getRed() * (2.0f - t);
                g = colorC.getGreen() * (t - 1.0f) + colorD.getGreen() * (2.0f - t);
                b = colorC.getBlue() * (t - 1.0f) + colorD.getBlue() * (2.0f - t);
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
        final float saturation = 1.0f;
        final float luminance = random.nextFloat();
        return Color.getHSBColor(hue, saturation, luminance);
    }

}
