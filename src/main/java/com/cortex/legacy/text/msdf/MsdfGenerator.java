package com.cortex.legacy.text.msdf;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a glyph outline into a multi-channel signed distance field.
 *
 * <p>A single-channel field rounds off sharp corners: near a corner the true distance is
 * the minimum of two edges, and interpolating that minimum between texels cuts the point
 * away. MSDF fixes this by giving neighbouring edges different colour masks, so each
 * channel sees a field that is locally smooth. Taking the median of the three channels at
 * sample time reconstructs the corner exactly.
 *
 * <p>This is the msdfgen algorithm reimplemented against flattened outlines — no native
 * dependency, and accurate enough that the difference is invisible at UI sizes.
 */
public final class MsdfGenerator {

    /** How finely curves are broken into line segments, in em-relative units. */
    private static final double FLATNESS = 0.02;

    /**
     * Direction change that counts as a corner. Flattened curves produce many small turns;
     * this sits well above them and well below the near-right angles found in real glyphs.
     */
    private static final double CORNER_COS = Math.cos(Math.toRadians(40));

    // Channel masks: every edge lights two of the three channels, so any two adjacent
    // edges still share one and their fields agree away from the corner between them.
    private static final int YELLOW = 0b011;  // red + green
    private static final int MAGENTA = 0b101; // red + blue
    private static final int CYAN = 0b110;    // green + blue
    private static final int WHITE = 0b111;

    private MsdfGenerator() {
    }

    private static final class Edge {
        final double ax, ay, bx, by;
        final double dx, dy, lengthSq;
        int color = WHITE;

        Edge(double ax, double ay, double bx, double by) {
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
            this.dx = bx - ax;
            this.dy = by - ay;
            this.lengthSq = dx * dx + dy * dy;
        }

        /** Squared distance from a point to this segment. */
        double distanceSq(double px, double py) {
            double t = lengthSq <= 1e-12 ? 0.0
                    : Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy) / lengthSq));
            double cx = ax + t * dx - px;
            double cy = ay + t * dy - py;
            return cx * cx + cy * cy;
        }
    }

    /**
     * Rasterises {@code outline} into RGB bytes.
     *
     * @param outline    glyph outline, already positioned so it fits the cell
     * @param cellSize   width and height of the square output
     * @param pxRange    width of the distance ramp, in cell pixels
     * @return {@code cellSize * cellSize * 3} bytes of RGB
     */
    public static byte[] generate(Shape outline, int cellSize, double pxRange) {
        List<List<Edge>> contours = flatten(outline);
        for (List<Edge> contour : contours) {
            assignColors(contour);
        }

        List<Edge> all = new ArrayList<>();
        for (List<Edge> contour : contours) {
            all.addAll(contour);
        }

        byte[] output = new byte[cellSize * cellSize * 3];
        Edge[] edges = all.toArray(new Edge[0]);

        for (int y = 0; y < cellSize; y++) {
            for (int x = 0; x < cellSize; x++) {
                // Sample at the texel centre.
                double px = x + 0.5;
                double py = y + 0.5;

                double nearestRed = Double.MAX_VALUE;
                double nearestGreen = Double.MAX_VALUE;
                double nearestBlue = Double.MAX_VALUE;
                int winding = 0;

                // One pass over the edges serves both the per-channel distances and the
                // inside test, which is what keeps a full ASCII bake under a second.
                for (Edge edge : edges) {
                    double distSq = edge.distanceSq(px, py);

                    if ((edge.color & 0b001) != 0 && distSq < nearestRed) nearestRed = distSq;
                    if ((edge.color & 0b010) != 0 && distSq < nearestGreen) nearestGreen = distSq;
                    if ((edge.color & 0b100) != 0 && distSq < nearestBlue) nearestBlue = distSq;

                    winding += windingContribution(edge, px, py);
                }

                // Positive inside, negative outside — the msdfgen convention the sampling
                // shader assumes when it turns the decoded distance straight into coverage.
                double sign = winding != 0 ? 1.0 : -1.0;
                int index = (y * cellSize + x) * 3;

                output[index] = encode(sign * Math.sqrt(nearestRed), pxRange);
                output[index + 1] = encode(sign * Math.sqrt(nearestGreen), pxRange);
                output[index + 2] = encode(sign * Math.sqrt(nearestBlue), pxRange);
            }
        }

        return output;
    }

    /** Maps a signed distance onto a byte, with the outline's edge landing at 0.5. */
    private static byte encode(double distance, double pxRange) {
        double normalised = 0.5 + distance / pxRange;
        int value = (int) (Math.max(0.0, Math.min(1.0, normalised)) * 255.0 + 0.5);
        return (byte) value;
    }

    /** Non-zero winding: counts which way the outline crosses a ray cast to the right. */
    private static int windingContribution(Edge edge, double px, double py) {
        if (edge.ay <= py) {
            if (edge.by > py && isLeft(edge, px, py) > 0) return 1;
        } else {
            if (edge.by <= py && isLeft(edge, px, py) < 0) return -1;
        }
        return 0;
    }

    private static double isLeft(Edge edge, double px, double py) {
        return edge.dx * (py - edge.ay) - (px - edge.ax) * edge.dy;
    }

    /** Breaks the outline into contours of line segments. */
    private static List<List<Edge>> flatten(Shape outline) {
        List<List<Edge>> contours = new ArrayList<>();
        List<Edge> current = null;

        double[] coords = new double[6];
        double startX = 0, startY = 0, lastX = 0, lastY = 0;

        PathIterator it = outline.getPathIterator(null, FLATNESS);
        for (; !it.isDone(); it.next()) {
            switch (it.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    closeContour(contours, current, lastX, lastY, startX, startY);
                    current = new ArrayList<>();
                    startX = lastX = coords[0];
                    startY = lastY = coords[1];
                }
                case PathIterator.SEG_LINETO -> {
                    if (current != null) {
                        current.add(new Edge(lastX, lastY, coords[0], coords[1]));
                    }
                    lastX = coords[0];
                    lastY = coords[1];
                }
                case PathIterator.SEG_CLOSE -> {
                    closeContour(contours, current, lastX, lastY, startX, startY);
                    current = null;
                    lastX = startX;
                    lastY = startY;
                }
                default -> {
                    // Flattening leaves only MOVETO, LINETO and CLOSE.
                }
            }
        }
        closeContour(contours, current, lastX, lastY, startX, startY);

        return contours;
    }

    private static void closeContour(List<List<Edge>> contours, List<Edge> contour,
                                     double lastX, double lastY, double startX, double startY) {
        if (contour == null) return;

        // A contour must be a closed loop for the winding test to be meaningful.
        if (Math.abs(lastX - startX) > 1e-9 || Math.abs(lastY - startY) > 1e-9) {
            contour.add(new Edge(lastX, lastY, startX, startY));
        }
        if (!contour.isEmpty()) {
            contours.add(contour);
        }
    }

    /**
     * Gives each run of edges between corners its own channel mask, rotating so that no
     * two runs meeting at a corner share both channels.
     */
    private static void assignColors(List<Edge> contour) {
        int size = contour.size();
        if (size == 0) return;

        List<Integer> corners = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Edge previous = contour.get((i + size - 1) % size);
            Edge current = contour.get(i);
            if (isCorner(previous, current)) {
                corners.add(i);
            }
        }

        // A fully smooth contour — an 'o' or the bowl of a 'b' — has no corner to protect,
        // so every channel can carry the same field.
        if (corners.isEmpty()) {
            for (Edge edge : contour) {
                edge.color = WHITE;
            }
            return;
        }

        int[] rotation = {YELLOW, CYAN, MAGENTA};

        // A single corner — a teardrop, or the tail of a 'j' — is its own case. The runs on
        // either side of it are the same run, so walking corner-to-corner the way the
        // general case does covers no edges at all and the whole contour stays WHITE, which
        // is exactly the single-channel field whose rounded corners MSDF exists to avoid.
        // Splitting the lap in half gives the corner two differently masked runs to sit
        // between; the seam at the far side falls on smooth outline, where the two masks
        // describe near-identical fields and the median crosses without a visible join.
        if (corners.size() == 1) {
            int start = corners.get(0);

            for (int step = 0; step < size; step++) {
                contour.get((start + step) % size).color =
                        step * 2 < size ? rotation[0] : rotation[2];
            }
            return;
        }

        int colorIndex = 0;

        for (int c = 0; c < corners.size(); c++) {
            int from = corners.get(c);
            int to = corners.get((c + 1) % corners.size());

            for (int i = from; i != to; i = (i + 1) % size) {
                contour.get(i).color = rotation[colorIndex % rotation.length];
            }
            colorIndex++;
        }
    }

    private static boolean isCorner(Edge previous, Edge current) {
        double previousLength = Math.sqrt(previous.lengthSq);
        double currentLength = Math.sqrt(current.lengthSq);
        if (previousLength <= 1e-9 || currentLength <= 1e-9) return false;

        double dot = (previous.dx * current.dx + previous.dy * current.dy)
                / (previousLength * currentLength);
        return dot < CORNER_COS;
    }
}
