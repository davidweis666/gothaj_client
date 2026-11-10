package team.gothaj.client.api.property.properties.pattern;

import java.util.Random;

public class PatternNoise {

    private float memoryX = 0f, memoryY = 0f;
    private int lastSnapIndex = -50;
    private float tremorPhase = 0f;
    private final Random rand = new Random();

    public Vec2 generateNoise(int index, long seed, int patternIndex, int patternSize, float prevX, float prevY, double fatigue, double focus, double skill, float distance) {
        rand.setSeed(seed ^ ((long) index << 16) ^ patternIndex);

        float errorScale = (float)(1.0 + fatigue * 0.4 - focus * 0.2 - skill * 0.15);
        float baseError = (rand.nextFloat() - 0.5f) * 0.042f * errorScale;
        float baseAimErrorX = baseError + (rand.nextFloat() - 0.5f) * 0.012f * errorScale;
        float baseAimErrorY = baseError * 0.9f + (rand.nextFloat() - 0.5f) * 0.015f * errorScale;

        float memorySpeed = (float)(0.13 + fatigue * 0.07 - focus * 0.05);
        memoryX += ((prevX - memoryX) * memorySpeed) + ((rand.nextFloat() - 0.5f) * 0.019f * errorScale);
        memoryY += ((prevY - memoryY) * memorySpeed) + ((rand.nextFloat() - 0.5f) * 0.017f * errorScale);

        float rapidThreshold = (float)(0.09 - skill * 0.03);
        float rapidChange = Math.abs(prevX - memoryX) + Math.abs(prevY - memoryY);
        if (rapidChange > rapidThreshold) {
            float panicScale = (float)(1.0 + fatigue * 0.5 - focus * 0.3);
            memoryX += ((rand.nextFloat() - 0.5f) * 0.045f * panicScale);
            memoryY += ((rand.nextFloat() - 0.5f) * 0.044f * panicScale);
        }

        float decayRate = (float)(0.98 - fatigue * 0.01 + focus * 0.005);
        memoryX *= decayRate;
        memoryY *= decayRate;

        tremorPhase += 0.0121f;
        float t = tremorPhase + seed * 0.0031f;

        float tremorIntensity = (float)(1.0 + fatigue * 0.6 - skill * 0.3 - focus * 0.2);
        float tremorBase = (float)(
                Math.sin(t * 8.5f) * 0.012f +
                        Math.sin(t * 11.3f) * 0.006f +
                        Math.sin(t * 0.7f) * 0.008f) * tremorIntensity;

        float fatTremorX = tremorBase + (float)Math.sin(t * 5.2f + 0.3f) * 0.004f * tremorIntensity;
        float fatTremorY = tremorBase * 0.85f + (float)Math.sin(t * 5.2f + 1.1f) * 0.004f * tremorIntensity;

        int intervalSinceSnap = index - lastSnapIndex;
        float snapBaseProb = (float)(0.002 + fatigue * 0.001 - skill * 0.0008);
        float snapProbability = Math.min(0.08f, intervalSinceSnap * snapBaseProb);

        int minSnapInterval = (int)(15 - skill * 5 + fatigue * 5);
        float microSnap = 0f;

        if (rand.nextFloat() < snapProbability && intervalSinceSnap > minSnapInterval) {
            float snapIntensity = (float)(1.0 + fatigue * 0.4 - focus * 0.3);
            float snapDir = (prevX > 0.5f) ? -1f : 1f;
            microSnap = snapDir * rand.nextFloat() * 0.01f * snapIntensity;
            lastSnapIndex = index;
        }

        float distanceScale = Math.max(0.4f, Math.min(1.3f, distance / 3.5f));

        float amplitudeScale = (1.0f - Math.min(0.75f, (float) patternSize / 300f)) * distanceScale;

        float noiseX = (baseAimErrorX + memoryX + fatTremorX + microSnap) * amplitudeScale;
        float noiseY = (baseAimErrorY + memoryY + fatTremorY - microSnap * 0.7f) * amplitudeScale;
        float maxNoise = (float)(0.12 + fatigue * 0.03 - skill * 0.04);
        noiseX = Math.max(-maxNoise, Math.min(maxNoise, noiseX));
        noiseY = Math.max(-maxNoise, Math.min(maxNoise, noiseY));

        return new Vec2(noiseX, noiseY);
    }
}
