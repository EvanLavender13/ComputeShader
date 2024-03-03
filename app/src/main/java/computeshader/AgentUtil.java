package computeshader;

import java.awt.Color;

public class AgentUtil {
    private static final int AGENT_SIZE_F = 6;

    public static float[] nAgentsOneColor(int n, int width, int height, Color color) {
        float[] agents = new float[n * AGENT_SIZE_F];

        for (int i = 0; i < n; i++) {
            agents[i * AGENT_SIZE_F + 0] = (float) (Math.random() * width);
            agents[i * AGENT_SIZE_F + 1] = (float) (Math.random() * height);
            agents[i * AGENT_SIZE_F + 2] = (float) (Math.random() * 360.0);

            // color
            agents[i * AGENT_SIZE_F + 3] = color.getRed() / 255.0f;
            agents[i * AGENT_SIZE_F + 4] = color.getGreen() / 255.0f;
            agents[i * AGENT_SIZE_F + 5] = color.getBlue() / 255.0f;
        }

        return agents;
    }

    public static float[] nAgents(int n, int width, int height) {
        float[] agents = new float[n * AGENT_SIZE_F];

        Color colorA = Color.CYAN;
        Color colorB = Color.MAGENTA;
        Color colocC = Color.YELLOW;

        for (int i = 0; i < n; i++) {
            agents[i * AGENT_SIZE_F + 0] = (float) (Math.random() * width);
            agents[i * AGENT_SIZE_F + 1] = (float) (Math.random() * height);
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

}
