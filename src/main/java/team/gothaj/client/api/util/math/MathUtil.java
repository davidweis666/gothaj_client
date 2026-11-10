package team.gothaj.client.api.util.math;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import team.gothaj.client.main.GothajClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.reverse;
import static net.minecraft.util.MathHelper.clamp;

public class MathUtil {

	public static float calculateGaussianValue(float x, float sigma) {
		double PI = Math.PI;
		double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
		return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
	}

	public static boolean inBetween(double min, double max, double value) {
		return value >= min && value <= max;
	}

	public static double roundToHalf(double d) {
		return Math.round(d * 2) / 2.0;
	}

	public static int getRandomNumber(int min, int max) {
		return (int) ((Math.random() * (max - min)) + min);
	}

	public static double round(double value, double inc) {
		final double halfOfInc = inc / 2.0D;
		final double floored = StrictMath.floor(value / inc) * inc;
		if (value >= floored + halfOfInc)
			return new BigDecimal(StrictMath.ceil(value / inc) * inc, MathContext.DECIMAL64).stripTrailingZeros()
					.doubleValue();
		else
			return new BigDecimal(floored, MathContext.DECIMAL64).stripTrailingZeros().doubleValue();
	}

    public static double roundInc(double value, double inc) {
        if (inc == 0) return value;

        BigDecimal bdValue = BigDecimal.valueOf(value);
        BigDecimal bdInc = BigDecimal.valueOf(inc);

        BigDecimal rounded = bdValue.divide(bdInc, 0, RoundingMode.HALF_UP)
                .multiply(bdInc);

        return rounded.stripTrailingZeros().doubleValue();
    }

	public static double roundDecimal(double value, double inc) {
		final double floored = StrictMath.floor(value / inc) * inc;
		return new BigDecimal(floored, MathContext.DECIMAL64).stripTrailingZeros().doubleValue();
	}


    public static String convertSecondsToHMmSs(long seconds) {
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        return String.format("%01d:%02d", m,s);
    }


    public static String getRandomString(int max) {

        String variety = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVEXYZ1234567890_";

        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < max) {
            int index = (int) (rnd.nextFloat() * variety.length());
            salt.append(variety.charAt(index));
        }

        return salt.toString();
    }

	public static double getBias(double t, double bias) {
		double k = Math.max(bias, 1e-6);
		return t / ((1.0 / k - 2.0) * (1.0 - t) + 1.0);
	}

	public static Vec3 clampingY(Vec3 eyePos, AxisAlignedBB box) {
		return new Vec3(box.getCenter().xCoord,clamp(eyePos.yCoord, box.minY, box.maxY),box.getCenter().zCoord);
	}
	public static Vec3 getRelative(Vec3 point, AxisAlignedBB box) {
		double x = (point.xCoord - box.minX) / (box.maxX - box.minX);
		double y = (point.yCoord - box.minY) / (box.maxY - box.minY);
		double z = (point.zCoord - box.minZ) / (box.maxZ - box.minZ);

		return new Vec3(x, y, z);
	}

    public static double random(double min, double max) {
		if (min == 0 && max == 0)
			return 0;

		return ThreadLocalRandom.current().nextDouble(min, max + 1);
	}

	public static float random(float min, float max) {
		// d
		if (min == 0 && max == 0)
			return 0;

		return ThreadLocalRandom.current().nextFloat(min, max + 1);
	}
}
