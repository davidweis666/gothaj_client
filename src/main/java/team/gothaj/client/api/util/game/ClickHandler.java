package team.gothaj.client.api.util.game;

import lombok.Getter;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.math.FastNoiseLite;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ClickHandler {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final FastNoiseLite behaviorNoise;

    private final double playerSkillLevel;
    private double currentFocusLevel = 0.6;
    private double sessionFatigueLevel = 0.0;
    private int behaviorCycle = 0;

    private double adrenalineLevel = 0.0;
    private int burstCounter = 0;
    private int targetBurstSize = 4;
    private double microTremorOffset = 0.0;

    private int butterflyStep = 0;

    private final List<Integer> recordedPattern = new ArrayList<>();
    @Getter private boolean isRecording = false;
    private int playbackIndex = 0;
    private long lastRecordTime = 0;

    private record ClickMemory(double delay, long timestamp, int contextType) {}
    private final Deque<ClickMemory> clickHistory = new LinkedList<>();
    private static final int HISTORY_SIZE = 150;

    @Getter private double currentDistributionSkewness = 0;
    @Getter private double currentDistributionKurtosis = 3.0;
    @Getter private int clicksSinceLastPause = 0;

    private long lastClickTime = 0;
    private int pauseThreshold;
    private long sessionStartTime;
    private int sessionClickCount = 0;

    private boolean inFatigueMode = false;
    private long fatigueStartTime;
    private long attentionLapseStartTime;
    private int fatigueDuration = 0;
    private double fatigueLevel = 0.0;
    private long lastAttentionLapseTime;
    private boolean inAttentionLapse = false;
    private double recentAccuracy = 0.95;

    private int currentBehaviorMode = 0;
    private long behaviorModeSwitchTime;
    private double lastDelay = 0;
    private double secondLastDelay = 0;
    private double rhythmOffset = 0;
    private long lastRhythmDriftTime = 0;

    private final double chaosInjectionProb;
    private final double fatigueProbBase;
    private final double attentionLapseProb;
    private final int sessionLengthMin;
    private final int sessionLengthMax;
    private final int attentionLapseDurationMin;
    private final int attentionLapseDurationMax;
    private final int pauseRangeMin;
    private final int pauseRangeMax;

    public ClickHandler() {
        this.behaviorNoise = new FastNoiseLite();
        this.behaviorNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.behaviorNoise.SetFrequency(0.04f);

        this.playerSkillLevel = 0.4 + random.nextDouble() * 0.45;

        double chaosBase = 0.035 + playerSkillLevel * 0.035;
        double fatigueBase = 0.12 + (1.0 - playerSkillLevel) * 0.16;
        double lapseBase = 0.035 + (1.0 - playerSkillLevel) * 0.055;

        this.chaosInjectionProb = chaosBase;
        this.fatigueProbBase = fatigueBase;
        this.attentionLapseProb = lapseBase;

        this.sessionLengthMin = 4 * 60 * 1000 + random.nextInt(2 * 60 * 1000);
        this.sessionLengthMax = 20 * 60 * 1000 + random.nextInt(8 * 60 * 1000);

        this.attentionLapseDurationMin = 500 + random.nextInt(400);
        this.attentionLapseDurationMax = 1700 + random.nextInt(500);

        this.pauseRangeMin = 18 + random.nextInt(6);
        this.pauseRangeMax = 35 + random.nextInt(7);

        this.pauseThreshold = random.nextInt(pauseRangeMin, pauseRangeMax);
        this.sessionStartTime = System.currentTimeMillis();
        this.lastAttentionLapseTime = System.currentTimeMillis();
        this.behaviorModeSwitchTime = System.currentTimeMillis();
        this.lastRhythmDriftTime = System.currentTimeMillis();
        this.targetBurstSize = random.nextInt(3, 6);
    }

    public int getDelay(String mode, double minCPS, double maxCPS, boolean isHurt, int hurtTime) {
        behaviorCycle++;
        updateFocusLevel();
        updateRhythmDrift();
        updatePhysiology(isHurt, hurtTime);

        int delay = switch (mode) {
            case "Randomized", "Smart" -> calculateSmartDelay(minCPS, maxCPS, isHurt, hurtTime);
            case "Butterfly" -> calculateButterflyDelay(minCPS, maxCPS);
            case "Recorded" -> calculateRecordedDelay(minCPS, maxCPS);
            case "Double Click" -> calculateDoubleClickDelay(minCPS, maxCPS);
            case "Jitter" -> calculateJitterDelay(minCPS, maxCPS);
            case "Drag" -> calculateDragDelay(minCPS, maxCPS);
            default -> calculateBiomechanicalDelay(minCPS, maxCPS);
        };

        secondLastDelay = lastDelay;
        lastDelay = delay;
        return delay;
    }

    private void updatePhysiology(boolean isHurt, int hurtTime) {
        double breathing = Math.sin(behaviorCycle * 0.05) * 2.5;
        double pulse = Math.cos(behaviorCycle * 0.22) * 1.1;
        microTremorOffset = breathing + pulse;

        if (isHurt && hurtTime > 0) {
            adrenalineLevel = Math.min(1.0, adrenalineLevel + 0.14);
        } else {
            adrenalineLevel = Math.max(0.0, adrenalineLevel - 0.025);
        }

        if (sessionClickCount > 400) {
            sessionFatigueLevel = Math.min(1.0, sessionFatigueLevel + 0.00012);
        }
    }

    private int calculateBiomechanicalDelay(double minCPS, double maxCPS) {
        double meanCPS = (minCPS + maxCPS) / 2.0;
        double activeCPS = meanCPS + (adrenalineLevel * (maxCPS - meanCPS) * 1.05);
        activeCPS -= (sessionFatigueLevel * (meanCPS * 0.18));

        int targetDelay = (int) (1000.0 / activeCPS);
        targetDelay += (int) microTremorOffset;

        burstCounter++;
        if (burstCounter >= targetBurstSize) {
            targetDelay += (int) (random.nextInt(16, 36) * (1.0 + sessionFatigueLevel));
            burstCounter = 0;
            targetBurstSize = random.nextInt(3, 7);
        } else {
            targetDelay -= random.nextInt(1, 5);
        }

        if (lastDelay > 0) {
            targetDelay = (int) (targetDelay * 0.45 + lastDelay * 0.55);
            
        }
        return enforceStrictBounds(targetDelay, minCPS, maxCPS);
    }

    private int calculateSmartDelay(double minCPS, double maxCPS, boolean isHurt, int hurtTime) {
        validateInputs(minCPS, maxCPS);
        updateBehaviorMode();
        simulateSessionFatigue();
        simulateAttentionLapse();

        double chaosRoll = (behaviorNoise.GetNoise(behaviorCycle * 0.006f, 40.0f) + 1.0) * 0.5;
        double chaosThreshold = chaosInjectionProb * (1.1 - currentFocusLevel * 0.2);

        int delay;
        if (chaosRoll < (1.0 - chaosThreshold)) {
            delay = calculateBaseSmartDelay(minCPS, maxCPS, isHurt, hurtTime);
        } else {
            delay = calculateChaosSmartDelay(minCPS, maxCPS, isHurt, hurtTime);
        }

        if (inFatigueMode) delay = applyFatigueMultiplier(delay, minCPS, maxCPS);
        if (inAttentionLapse) delay = applyAttentionLapseDelay(delay);

        delay = applyAccuracyVariation(delay, minCPS, maxCPS);
        delay = (int) (delay + rhythmOffset + microTremorOffset);
        delay = breakAutocorrelation(delay, minCPS, maxCPS);

        burstCounter++;
        if (burstCounter >= targetBurstSize) {
            delay += random.nextInt(12, 28);
            burstCounter = 0;
            targetBurstSize = random.nextInt(4, 8);
        }

        delay = enforceStrictBounds(delay, minCPS + 1, maxCPS + 3);

        recordClickDelay(delay, isHurt ? 1 : 0);
        breakPerfectDistribution(delay, minCPS, maxCPS);

        return delay;
    }

    private int calculateBaseSmartDelay(double minCPS, double maxCPS, boolean isHurt, int hurtTime) {
        double range = maxCPS - minCPS;
        double targetCPS;
        double variance;

        if (isHurt && hurtTime > 5) {
            double hurtNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.01f, 50.0f) + 1.0) * 0.5;
            targetCPS = minCPS + range * (0.25 + hurtNoise * 0.3) + (adrenalineLevel * range * 0.2);
            variance = range * (0.12 + hurtNoise * 0.06);
        } else {
            double blendExp = 1.15 + playerSkillLevel * 0.25 + currentFocusLevel * 0.15;
            double blend = Math.pow(random.nextDouble(), blendExp);
            targetCPS = minCPS + range * blend;
            variance = range * (0.10 + (1.0 - currentFocusLevel) * 0.09);
        }

        double logNormalNoise = Math.exp(random.nextGaussian() * 0.38) - 1.0;
        double varSign = random.nextBoolean() ? 1 : -1;
        double overallNoise = varSign * logNormalNoise * variance * 0.55;

        double spikeNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.015f, 60.0f) + 1.0) * 0.5;
        if (spikeNoise > 0.93) {
            overallNoise *= (1.5 + random.nextDouble() * 1.1);
        }

        double cps = clamp(targetCPS + overallNoise, minCPS, maxCPS);
        int delay = (int) (1000.0 / cps);

        double jitterNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.02f, 70.0f) + 1.0) * 0.5;
        if (jitterNoise > 0.84) {
            int jitterRange = (int) (10 + random.nextDouble() * random.nextDouble() * 25);
            delay += random.nextInt(-jitterRange, jitterRange + 1);
        }

        return delay;
    }

    private int calculateChaosSmartDelay(double minCPS, double maxCPS, boolean isHurt, int hurtTime) {
        if (isHurt && hurtTime > 8) {
            double midCPS = (minCPS + maxCPS) / 2.0;
            double reducedCPS = Math.max(minCPS * 0.75, midCPS * (0.6 + 0.1 * random.nextDouble()));
            int delay = (int) (1000.0 / reducedCPS) + random.nextInt(-45, 46);
            return enforceStrictBounds(delay, minCPS, maxCPS);
        }

        int chaosType = currentBehaviorMode % 3;
        return switch (chaosType) {
            case 0 -> {
                double randomCPS = minCPS + (maxCPS - minCPS) * random.nextDouble();
                int jitter = (int) (random.nextGaussian() * 24);
                yield (int) (1000.0 / randomCPS) + jitter;
            }
            case 1 -> {
                double slowFactor = 0.76 + random.nextDouble() * 0.22;
                double slowCPS = minCPS + (maxCPS - minCPS) * 0.28 * slowFactor;
                yield (int) (1000.0 / slowCPS) + random.nextInt(-24, 25);
            }
            case 2 -> {
                double fastFactor = 0.85 + random.nextDouble() * 0.15;
                double fastCPS = minCPS + (maxCPS - minCPS) * 0.82 * fastFactor;
                yield (int) (1000.0 / fastCPS) + random.nextInt(-18, 19);
            }
            default -> (int) (1000.0 / ((minCPS + maxCPS) / 2.0));
        };
    }

    private int calculateButterflyDelay(double minCPS, double maxCPS) {
        butterflyStep++;
        double meanCPS = (minCPS + maxCPS) / 2.0;
        int baseDelay = (int) (1000 / meanCPS);
        int delay;

        boolean isIndexDominant = butterflyStep % 2 == 0;
        if (isIndexDominant) {
            delay = (int) (baseDelay * (0.72 + random.nextGaussian() * 0.08));
        } else {
            delay = (int) (baseDelay * (1.32 + random.nextGaussian() * 0.14));
        }

        if (random.nextDouble() < 0.09) {
            delay = random.nextInt(11, 24);
        }

        return enforceStrictBounds(delay, minCPS, maxCPS * 1.4);
    }

    private int calculateJitterDelay(double minCPS, double maxCPS) {
        double targetCPS = maxCPS + (adrenalineLevel * 1.2) - (sessionFatigueLevel * 2.5);
        int delay = (int) (1000.0 / targetCPS);

        double spasm = behaviorNoise.GetNoise(behaviorCycle * 0.45f, 200.0f);
        delay += (int) (spasm * 14);

        if (random.nextDouble() < 0.022 + (sessionFatigueLevel * 0.04)) {
            delay += random.nextInt(35, 75);
        }

        return enforceStrictBounds(delay, minCPS - 1.0, maxCPS + 2.0);
    }

    private int calculateDragDelay(double minCPS, double maxCPS) {
        if (random.nextDouble() < 0.14 || behaviorCycle % 9 == 0) {
            return random.nextInt((int) (1000.0 / minCPS), (int) (1000.0 / minCPS) + 55);
        } else {
            return random.nextInt(11, 32);
        }
    }

    private int calculateNormalDelay(double minCPS, double maxCPS) {
        double meanCPS = (minCPS + maxCPS) / 2.0;
        double stdDev = (maxCPS - minCPS) / 4.5;

        double u1 = 1.0 - random.nextDouble();
        double u2 = 1.0 - random.nextDouble();
        double randStdNormal = Math.sqrt(-2.0 * Math.log(u1)) * Math.sin(2.0 * Math.PI * u2);

        double targetCPS = meanCPS + randStdNormal * stdDev;
        int delay = (int) (1000.0 / clamp(targetCPS, minCPS, maxCPS));

        if (lastDelay > 0 && secondLastDelay > 0) {
            delay = (int) (delay * 0.50 + lastDelay * 0.38 + secondLastDelay * 0.12);
        }

        return enforceStrictBounds(delay, minCPS, maxCPS);
    }

    private int calculateDoubleClickDelay(double minCPS, double maxCPS) {
        double biasNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.01f, 15.0f) + 1.0) * 0.5;
        double doubleChance = 0.33 + biasNoise * 0.14;

        if (random.nextDouble() < doubleChance) {
            return random.nextInt(11, 26);
        }

        return calculateNormalDelay(minCPS, maxCPS);
    }

    private int calculateRecordedDelay(double minCPS, double maxCPS) {
        if (recordedPattern.isEmpty()) return 50;

        if (playbackIndex >= recordedPattern.size()) {
            playbackIndex = 0;
        }

        int delay = recordedPattern.get(playbackIndex++);
        int variation = (int) (random.nextGaussian() * (1.8 + sessionFatigueLevel * 4.0));

        return enforceStrictBounds(delay + variation, minCPS, maxCPS);
    }

    private void updateFocusLevel() {
        double focusNoise = behaviorNoise.GetNoise(behaviorCycle * 0.004f, 10.0f);
        currentFocusLevel += focusNoise * 0.012 * (1.0 - sessionFatigueLevel * 0.4);
        currentFocusLevel = clamp(currentFocusLevel, 0.2, 0.95);
    }

    private void updateBehaviorMode() {
        long now = System.currentTimeMillis();
        long timeSinceSwitch = now - behaviorModeSwitchTime;

        double switchProbability = 1.0 - Math.exp(-timeSinceSwitch / 90000.0);
        double switchNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.001f, 100.0f) + 1.0) * 0.5;
        double switchThreshold = 0.025 * (1.0 + switchNoise * 0.4);

        if (timeSinceSwitch > 40000 && random.nextDouble() < switchProbability * switchThreshold) {
            int previousMode = currentBehaviorMode;
            double modeNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.003f, 110.0f) + 1.0) * 0.5;
            double skillBias = playerSkillLevel * 0.15;

            double normalBias = 0.38 + skillBias;
            if (modeNoise < normalBias) currentBehaviorMode = 0;
            else if (modeNoise < (normalBias + 0.24)) currentBehaviorMode = 1;
            else if (modeNoise < (normalBias + 0.44)) currentBehaviorMode = 2;
            else if (modeNoise < (normalBias + 0.54)) currentBehaviorMode = 3;
            else currentBehaviorMode = 4;

            if (currentBehaviorMode == previousMode && random.nextDouble() < (0.65 + currentFocusLevel * 0.15)) {
                currentBehaviorMode = (currentBehaviorMode + 1 + random.nextInt(3)) % 5;
            }
            behaviorModeSwitchTime = now;
        }
    }

    private void simulateSessionFatigue() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        if (sessionDuration > sessionLengthMin) {
            fatigueLevel = Math.min(1.0, (sessionDuration - sessionLengthMin) / (double) (sessionLengthMax - sessionLengthMin));
            sessionFatigueLevel = fatigueLevel;

            double fatigueProb = fatigueProbBase * (1.0 + (0.95 - recentAccuracy) * 1.5) * (1.0 + sessionFatigueLevel * 0.5);

            if (sessionFatigueLevel > 0.5 && !inFatigueMode && random.nextDouble() < fatigueProb) {
                inFatigueMode = true;
                fatigueStartTime = System.currentTimeMillis();
                fatigueDuration = (int) (35000 + random.nextInt(70000) * (1.0 + playerSkillLevel * 0.3));
            }

            if (inFatigueMode) {
                long currentFatigueDuration = System.currentTimeMillis() - fatigueStartTime;
                if (random.nextDouble() < (1.0 - Math.exp(-currentFatigueDuration / (double) fatigueDuration)) * 0.02) {
                    inFatigueMode = false;
                }
            }
        }
    }

    private void simulateAttentionLapse() {
        long timeSinceLastLapse = System.currentTimeMillis() - lastAttentionLapseTime;
        double lapseProb = attentionLapseProb * (1.0 + (0.95 - recentAccuracy) * 2.0) * (1.0 + sessionFatigueLevel * 0.4);

        if (timeSinceLastLapse > 60000 && random.nextDouble() < (1.0 - Math.exp(-timeSinceLastLapse / 180000.0)) * lapseProb) {
            inAttentionLapse = true;
            attentionLapseStartTime = System.currentTimeMillis();
            lastAttentionLapseTime = System.currentTimeMillis();
        }

        if (inAttentionLapse) {
            long lapseDuration = System.currentTimeMillis() - attentionLapseStartTime;
            int expectedDuration = Math.min(attentionLapseDurationMin + (int) (Math.log(1.0 / (1.0 - random.nextDouble() * 0.95)) * 400), attentionLapseDurationMax);
            if (lapseDuration > expectedDuration) {
                inAttentionLapse = false;
            }
        }
    }

    private int applyFatigueMultiplier(int delay, double minCPS, double maxCPS) {
        double fatigueMultiplier = 1.0 + (Math.pow(fatigueLevel, 1.3) * 0.4);
        if (fatigueLevel > 0.3) {
            fatigueMultiplier += random.nextGaussian() * fatigueLevel * 0.08;
        }
        return Math.min((int) (delay * fatigueMultiplier), (int) Math.floor(1000.0 / minCPS));
    }

    private int applyAttentionLapseDelay(int delay) {
        return delay + Math.min(80 + (int) (Math.log(1.0 / (1.0 - random.nextDouble() * 0.9)) * 60), 250);
    }

    private int applyAccuracyVariation(int delay, double minCPS, double maxCPS) {
        double adjustNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.0035f, 160.0f) + 1.0) * 0.5;
        if (random.nextDouble() < (0.02 + adjustNoise * 0.02) && sessionClickCount > 0) {
            if (recentAccuracy < 0.85) {
                delay = (int) (delay * (1.06 + (0.85 - recentAccuracy) * 0.35));
            } else if (recentAccuracy > 0.96) {
                delay = (int) (delay * (0.97 + random.nextDouble() * 0.025));
            }
        }
        return delay;
    }

    private int breakAutocorrelation(int delay, double minCPS, double maxCPS) {
        if (lastDelay > 0 && secondLastDelay > 0) {
            double diff1 = Math.abs(delay - lastDelay);
            double diff2 = Math.abs(lastDelay - secondLastDelay);
            double avgDelay = (1000.0 / minCPS + 1000.0 / maxCPS) / 2.0;

            if (diff1 < avgDelay * 0.08 && diff2 < avgDelay * 0.08) {
                if ((behaviorNoise.GetNoise(behaviorCycle * 0.0045f, 170.0f) + 1.0) * 0.5 > 0.55) {
                    delay = (int) (delay * (0.84 + random.nextDouble() * 0.32));
                }
            }
        }
        return delay;
    }

    private int enforceStrictBounds(int delay, double minCPS, double maxCPS) {
        if (delay <= 24) return delay;
        int minDelay = (int) Math.ceil(1000.0 / (maxCPS));
        int maxDelay = minCPS > 1.0
                ? (int) Math.floor(1000.0 / (minCPS - 1.0))
                : (int) Math.floor(1000.0 / minCPS) + 50;
        
        return (int) clamp(delay, minDelay, maxDelay);
    }

    private void breakPerfectDistribution(int delay, double minCPS, double maxCPS) {
        if (clickHistory.size() > 40) {
            updateDistributionStatistics();

            if (Math.abs(currentDistributionSkewness) < 0.35 && Math.abs(currentDistributionKurtosis) < 1.0) {
                if ((behaviorNoise.GetNoise(behaviorCycle * 0.005f, 180.0f) + 1.0) * 0.5 > 0.66 && !clickHistory.isEmpty()) {
                    ClickMemory last = clickHistory.removeLast();
                    int chaosDelay = (int) (1000.0 / (minCPS + (maxCPS - minCPS) * (0.1 + random.nextDouble() * 0.6)));
                    recordClickDelay(chaosDelay + (int) (random.nextGaussian() * 25), last.contextType());
                }
            }

            if (Math.abs(currentDistributionKurtosis) > 4.0) {
                if ((behaviorNoise.GetNoise(behaviorCycle * 0.006f, 190.0f) + 1.0) * 0.5 > 0.7 && !clickHistory.isEmpty()) {
                    recordClickDelay((int) (1000.0 / ((minCPS + maxCPS) / 2.0)) + random.nextInt(-10, 11), 0);
                }
            }
        }
    }

    private void updateDistributionStatistics() {
        if (clickHistory.size() < 15) return;

        double sum = 0;
        for (ClickMemory c : clickHistory) sum += c.delay();
        double mean = sum / clickHistory.size();

        double varianceSum = 0, skewSum = 0, kurtSum = 0;
        for (ClickMemory c : clickHistory) varianceSum += Math.pow(c.delay() - mean, 2);

        double variance = varianceSum / clickHistory.size();
        double stdDev = Math.sqrt(variance);

        if (stdDev > 0.001) {
            for (ClickMemory c : clickHistory) {
                double standardized = (c.delay() - mean) / stdDev;
                skewSum += Math.pow(standardized, 3);
                kurtSum += Math.pow(standardized, 4);
            }
            currentDistributionSkewness = skewSum / clickHistory.size();
            currentDistributionKurtosis = (kurtSum / clickHistory.size()) - 3.0;
        }
    }

    private void updateRhythmDrift() {
        long now = System.currentTimeMillis();
        double intervalNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.0008f, 120.0f) + 1.0) * 0.5;

        if (now - lastRhythmDriftTime > (long) (9000 + intervalNoise * 16000)) {
            rhythmOffset += behaviorNoise.GetNoise(behaviorCycle * 0.002f, 130.0f) * (3.5 + (1.0 - currentFocusLevel) * 2.5);
            rhythmOffset *= (0.82 + ((behaviorNoise.GetNoise(behaviorCycle * 0.0012f, 140.0f) + 1.0) * 0.5) * 0.08);
            rhythmOffset = clamp(rhythmOffset, -(13 + playerSkillLevel * 6), (13 + playerSkillLevel * 6));
            lastRhythmDriftTime = now;
        }
    }

    public void registerClick() {
        lastClickTime = System.currentTimeMillis();
        clicksSinceLastPause++;
        sessionClickCount++;
        if (clicksSinceLastPause >= pauseThreshold) {
            pauseThreshold = Math.min(pauseRangeMin + (int) (Math.log(1.0 / (1.0 - random.nextDouble() * 0.95)) * 8), pauseRangeMax);
            clicksSinceLastPause = 0;
        }
    }

    public void startRecording() {
        recordedPattern.clear();
        playbackIndex = 0;
        lastRecordTime = System.currentTimeMillis();
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
        if (recordedPattern.isEmpty()) recordedPattern.add(50);
    }

    public void handleRealClick() {
        if (isRecording) {
            long now = System.currentTimeMillis();
            if (lastRecordTime != 0) {
                int delay = (int) (now - lastRecordTime);
                if (delay >= 20 && delay <= 500) {
                    recordedPattern.add(delay);
                    Logger.clientChat("Click recorded: " + delay + "ms");
                }
            }
            lastRecordTime = now;
        }
    }

    public void recordHit() {
        recentAccuracy = recentAccuracy * 0.9 + 0.1;
    }

    public void recordMiss() {
        recentAccuracy = recentAccuracy * 0.9;
    }

    private void recordClickDelay(int delay, int contextType) {
        clickHistory.addLast(new ClickMemory(delay, System.currentTimeMillis(), contextType));
        if (clickHistory.size() > HISTORY_SIZE) {
            clickHistory.removeFirst();
        }
    }

    public void reset() {
        clicksSinceLastPause = 0;
        lastClickTime = 0;
        clickHistory.clear();
        sessionStartTime = System.currentTimeMillis();
        sessionClickCount = 0;
        butterflyStep = 0;
        playbackIndex = 0;
        lastRhythmDriftTime = System.currentTimeMillis();
        pauseThreshold = random.nextInt(pauseRangeMin, pauseRangeMax);
        currentBehaviorMode = 0;
        behaviorModeSwitchTime = System.currentTimeMillis();
        lastDelay = 0;
        secondLastDelay = 0;
        rhythmOffset = 0;
        inFatigueMode = false;
        inAttentionLapse = false;
        fatigueLevel = 0.0;
        sessionFatigueLevel = 0.0;
        recentAccuracy = 0.95;
        adrenalineLevel = 0.0;
        burstCounter = 0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void validateInputs(double minCPS, double maxCPS) {
        if (minCPS <= 0 || maxCPS <= 0 || minCPS > maxCPS) {
            throw new IllegalArgumentException("Invalid CPS bounds provided.");
        }
    }
}