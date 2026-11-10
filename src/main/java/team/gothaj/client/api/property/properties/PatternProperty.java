package team.gothaj.client.api.property.properties;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.pattern.AimPattern;
import team.gothaj.client.api.property.properties.pattern.PatternNoise;
import team.gothaj.client.api.property.properties.pattern.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class PatternProperty extends Property<String> {

    private final List<Vec2> patternPoints;
    private final List<Vec2> smoothedPattern;
    private boolean isRecording;
    private float noiseScale;
    private int smoothness;
    private boolean patternDirty = true;

    private float aimHeight;
    private float pitchRange;
    private int jitterTick = 0;

    private int patternPhaseOffset = 0;
    private float extraUniqueOffsetX = 0f;
    private float extraUniqueOffsetY = 0f;
    private int humanHesitateTicks = 0;
    private int lastHumanEventIndex = -128;
    private int lastBehaviorUpdateIndex = 0;

    private float emaVariance = 0.001f;
    private static final float EMA_ALPHA = 0.15f;
    private float recentHitRate = 0.5f;
    private int totalAimAttempts = 0;
    private int recentHits = 0;

    private int frozenIndex = -1;

    private final PatternNoise patternNoiseAI;

    private static final int recentHistorySize = 7;
    private final List<Vec2> recentAimPoints = new ArrayList<>();

    private AimPattern currentPattern = AimPattern.NORMAL;
    private int behaviorTickOffset = 0;

    public PatternProperty(String name, String s) {
        super(name, s);
        this.patternPoints = new ArrayList<>();
        this.smoothedPattern = new ArrayList<>();
        this.isRecording = false;
        this.patternNoiseAI = new PatternNoise();
        randomizeForSpawn();
    }

    public void randomizeForSpawn() {
        Random rand = new Random(System.nanoTime());
        this.noiseScale = 0.17f + (rand.nextFloat() - 0.5f) * 0.04f;
        this.smoothness = 22 + rand.nextInt(7);
        this.aimHeight = 0.74f + (rand.nextFloat() - 0.5f) * 0.02f;
        this.pitchRange = 10.6f + (rand.nextFloat() - 0.5f) * 0.8f;
    }

    public void updateJitter() {
        jitterTick++;
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        float jitterProb = 0.04f + (jitterTick % 50) * 0.001f;
        if (tlr.nextFloat() < jitterProb) {
            this.noiseScale += (tlr.nextFloat() - 0.5f) * 0.008f;
            this.noiseScale = Math.max(0.16f, Math.min(0.20f, this.noiseScale));
            this.aimHeight += (tlr.nextFloat() - 0.5f) * 0.01f;
            this.aimHeight = Math.max(0.72f, Math.min(0.76f, this.aimHeight));
            this.pitchRange += (tlr.nextFloat() - 0.5f) * 0.2f;
            this.pitchRange = Math.max(10.2f, Math.min(11.0f, this.pitchRange));
        }
    }


    public void recordHit(boolean success) {
        totalAimAttempts++;
        if (success) recentHits++;

        if (totalAimAttempts % 50 == 0) {
            recentHitRate = recentHits / 50.0f;
            recentHits = 0;
        }
    }

    public float getAimQualityScore() {
        float varianceScore = Math.max(0f, 1f - emaVariance / 0.005f);
        float hitScore = recentHitRate;
        float smoothnessScore = Math.max(0f, 1f - (recentAimPoints.size() > 1 ?
                calculateRecentMovementMagnitude(recentAimPoints.getLast()) / 0.02f : 0f));

        return (varianceScore + hitScore * 2 + smoothnessScore) / 4.0f;
    }


    public void addPoint(float x, float y) {
        if (!patternPoints.isEmpty()) {
            Vec2 last = patternPoints.getLast();
            float dx = x - last.x;
            float dy = y - last.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 0.005f) return;
        }
        patternPoints.add(new Vec2(x, y));
        if (patternPoints.size() >= 2) {
            patternDirty = true;
        }
    }

    public void clearPattern() {
        patternPoints.clear();
        smoothedPattern.clear();
        recentAimPoints.clear();
        currentPattern = AimPattern.NORMAL;
        humanHesitateTicks = 0;
        lastHumanEventIndex = -128;
        extraUniqueOffsetX = 0f;
        extraUniqueOffsetY = 0f;
        patternPhaseOffset = 0;
        jitterTick = 0;
    }

    public List<Vec2> getSmoothedPattern() {
        if (patternDirty && patternPoints.size() >= 2) {
            rebuildSmoothedPattern();
            patternDirty = false;
        }
        return smoothedPattern;
    }

    public void rebuildSmoothedPattern() {
        smoothedPattern.clear();
        if (patternPoints.size() < 2) return;

        for (int i = 0; i < patternPoints.size() - 1; i++) {
            Vec2 p0 = i > 0 ? patternPoints.get(i - 1) : patternPoints.get(i);
            Vec2 p1 = patternPoints.get(i);
            Vec2 p2 = patternPoints.get(i + 1);
            Vec2 p3 = (i + 2 < patternPoints.size()) ? patternPoints.get(i + 2) : patternPoints.get(i + 1);

            for (int j = 0; j <= smoothness; j++) {
                float t = (float) j / smoothness;
                Vec2 point = catmullRom(p0, p1, p2, p3, t);
                smoothedPattern.add(point);
            }
        }
    }

    private Vec2 catmullRom(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        float x = 0.5f * ((2 * p1.x) +
                (-p0.x + p2.x) * t +
                (2*p0.x - 5*p1.x + 4*p2.x - p3.x) * t2 +
                (-p0.x + 3*p1.x - 3*p2.x + p3.x) * t3);

        float y = 0.5f * ((2 * p1.y) +
                (-p0.y + p2.y) * t +
                (2*p0.y - 5*p1.y + 4*p2.y - p3.y) * t2 +
                (-p0.y + 3*p1.y - 3*p2.y + p3.y) * t3);

        return new Vec2(x, y);
    }

    public Vec2 getRotationAtIndex(int index, long seed, double fatigue, double focus, double skill, float targetDistance, boolean rawPattern) {
        if (!rawPattern) {
            updateJitter();
            updateBehavior(index, seed);
        }

        if (humanHesitateTicks > 0 && !rawPattern) {
            humanHesitateTicks--;
            if (frozenIndex == -1) {
                frozenIndex = index;
            }
            return calculatePointOnly(frozenIndex, seed, fatigue, focus, skill, targetDistance, rawPattern);
        } else {
            frozenIndex = -1;
        }

        return calculatePointOnly(index, seed, fatigue, focus, skill, targetDistance, rawPattern);
    }



    private void updateBehavior(int index, long seed) {
        if (currentPattern == AimPattern.HESITATE && humanHesitateTicks == 0) {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            humanHesitateTicks = 4 + tlr.nextInt(7);
        }
    }

    private Vec2 calculatePointOnly(int index, long seed, double fatigue, double focus, double skill, float targetDistance, boolean rawPattern) {
        List<Vec2> pattern = getSmoothedPattern();
        if (pattern.isEmpty()) return new Vec2(0.5f, 0.5f);

        int size = pattern.size();
        int wrappedIndex = index % size;
        Vec2 basePoint = pattern.get(wrappedIndex);

        if (rawPattern) {
            return new Vec2(basePoint.x, basePoint.y);
        }
        ThreadLocalRandom tlr = ThreadLocalRandom.current();


        int flickInterval = index - lastHumanEventIndex;
        float flickProb = (float)(Math.min(0.025f, flickInterval * 0.0006f) * (1.3 - focus * 0.5));
        boolean triggerFlick = flickInterval > 25 && tlr.nextDouble() < flickProb;

        int pauseInterval = index - lastHumanEventIndex;
        float pauseProb = (float)(Math.min(0.022f, pauseInterval * 0.0005f) * (1.2 - focus * 0.4));
        boolean triggerPause = pauseInterval > 20 && tlr.nextDouble() < pauseProb;

        if (triggerFlick) {
            lastHumanEventIndex = index;
            extraUniqueOffsetX += (tlr.nextFloat() - 0.5f) * 0.08f;
            extraUniqueOffsetY += (tlr.nextFloat() - 0.5f) * 0.09f;
        }
        if (triggerPause) {
            lastHumanEventIndex = index;
            humanHesitateTicks = (int)(Math.log(1 - tlr.nextDouble()) / -0.18) + 3;
            humanHesitateTicks = Math.min(humanHesitateTicks, 22);
        }

        if (wrappedIndex == 0 && index > 0) {
            long driftSeed = generateSeed(index, seed);

            Random driftRand = new Random(driftSeed);

            patternPhaseOffset += driftRand.nextInt(Math.max(3, size / 20)) - (Math.max(1, size / 40));
            if (patternPhaseOffset < 0) patternPhaseOffset += size;
            if (patternPhaseOffset >= size) patternPhaseOffset -= size;

            extraUniqueOffsetX += (driftRand.nextFloat() - 0.5f) * 0.035f + (index / 1739.0f);
            extraUniqueOffsetY += (driftRand.nextFloat() - 0.5f) * 0.035f + (index / 1372.0f);

            if (Math.abs(extraUniqueOffsetX) > 0.11f) extraUniqueOffsetX *= 0.5f;
            if (Math.abs(extraUniqueOffsetY) > 0.11f) extraUniqueOffsetY *= 0.5f;
        }


        int step = 1;
        if (tlr.nextFloat() < 0.15f) step = 2;
        int driftedIndex = ((index + patternPhaseOffset + step - 1) % size + size) % size;

        float moveUnique = (index * 0.00051f);
        float microFlick = (tlr.nextFloat() < 0.02f) ? ((float)Math.sin(index * 3.7f) * 0.005f) : 0f;

        Vec2 noise = patternNoiseAI.generateNoise(index, seed, driftedIndex, size, basePoint.x, basePoint.y, fatigue, focus, skill, targetDistance);

        float penalty = riskPenalty(basePoint.x, basePoint.y);

        float dynamicNoiseScale = noiseScale * (float)(1.0 + fatigue * 0.35 - focus * 0.25);
        float distanceScale = Math.min(1.0f, targetDistance / 4.0f);
        float adjustedNoiseScale = dynamicNoiseScale * (0.6f + distanceScale * 0.4f);

        float finalX = basePoint.x + noise.x * adjustedNoiseScale * (1f - penalty) + extraUniqueOffsetX + (float)Math.sin(moveUnique) * 0.002f + microFlick;
        float finalY = basePoint.y + noise.y * adjustedNoiseScale * (1f - penalty) + extraUniqueOffsetY + (float)Math.cos(moveUnique) * 0.002f - microFlick;

        if (penalty > 0.4f) {
            finalX += (tlr.nextFloat() - 0.5f) * 0.012f;
            finalY += (tlr.nextFloat() - 0.5f) * 0.012f;
        }

        Vec2 candidatePoint = new Vec2(finalX, finalY);
        float recentMove = calculateRecentMovementMagnitude(candidatePoint);
        float maxStep = 0.024f;
        if (recentMove > maxStep) {
            Vec2 lastPoint = recentAimPoints.size() > 1 ? recentAimPoints.getLast() : candidatePoint;
            float interpFactor = maxStep / recentMove;
            finalX = lastPoint.x + (finalX - lastPoint.x) * interpFactor;
            finalY = lastPoint.y + (finalY - lastPoint.y) * interpFactor;
        }

        finalX = clamp(finalX, 0f, 1f);
        finalY = clamp(finalY, 0f, 1f);
        Vec2 finalPoint = new Vec2(finalX, finalY);

        float variance = calculateAimVariance();
        if (variance < 0.00012f && currentPattern == AimPattern.NORMAL) {
            finalPoint.x += (tlr.nextFloat() - 0.5f) * 0.005f;
            finalPoint.y += (tlr.nextFloat() - 0.5f) * 0.005f;
        }
        if (currentPattern == AimPattern.OVERSHOOT) {
            finalPoint.x += (tlr.nextFloat() - 0.5f) * 0.008f;
            finalPoint.y += (tlr.nextFloat() - 0.5f) * 0.008f;
        }

        updateVarianceEMA(finalPoint);
        addRecentPoint(finalPoint);
        return finalPoint;
    }

    private long generateSeed(int index, long baseSeed) {
        long hash = baseSeed;
        hash ^= (long)index * 0x9E3779B97F4A7C15L;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        return hash ^ (hash >>> 31);
    }

    private float riskPenalty(float x, float y) {
        if (recentAimPoints.isEmpty()) return 0f;

        Vec2 recent = recentAimPoints.getLast();
        float dx = x - recent.x;
        float dy = y - recent.y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.1f) {
            return Math.min(0.8f, (dist - 0.1f) * 4.0f);
        }
        return 0f;
    }

    private void updateVarianceEMA(Vec2 newPoint) {
        if (recentAimPoints.isEmpty()) {
            return;
        }

        Vec2 lastPoint = recentAimPoints.getLast();
        float dx = newPoint.x - lastPoint.x;
        float dy = newPoint.y - lastPoint.y;
        float instantVariance = dx * dx + dy * dy;

        emaVariance = EMA_ALPHA * instantVariance + (1 - EMA_ALPHA) * emaVariance;
    }

    private float calculateRecentMovementMagnitude(Vec2 newPoint) {
        if (recentAimPoints.isEmpty()) {
            return 0f;
        }
        Vec2 lastPoint = recentAimPoints.getLast();
        float dx = newPoint.x - lastPoint.x;
        float dy = newPoint.y - lastPoint.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float calculateAimVariance() {
        return emaVariance;
    }

    private void addRecentPoint(Vec2 point) {
        if (recentAimPoints.size() >= recentHistorySize) {
            recentAimPoints.removeFirst();
        }
        recentAimPoints.add(point);
    }

    public void setSmoothness(int smoothness) {
        if (this.smoothness != smoothness) {
            this.smoothness = smoothness;
            patternDirty = true;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public String serializePattern() {
        if (patternPoints.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Vec2 point : patternPoints) {
            sb.append(point.x).append(",").append(point.y).append(";");
        }
        return sb.toString();
    }

    public void deserializePattern(String data) {
        clearPattern();
        if (data == null || data.isEmpty()) return;
        String[] points = data.split(";");
        for (String point : points) {
            if (point.isEmpty()) continue;
            String[] coords = point.split(",");
            if (coords.length == 2) {
                try {
                    float x = Float.parseFloat(coords[0]);
                    float y = Float.parseFloat(coords[1]);
                    patternPoints.add(new Vec2(x, y));
                } catch (NumberFormatException ignore) {}
            }
        }
        rebuildSmoothedPattern();
    }
}
