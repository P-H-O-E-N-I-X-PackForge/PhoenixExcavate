package net.phoenixvine.excavate.vein;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class FacingUtil {

    // How much better-aligned the new horizontal candidate must be than sticking with the previous
    // direction before we actually switch. Without this, a look angle sitting near the 45-degree boundary
    // between two cardinal directions (e.g. north/east) flips back and forth on the tiniest mouse movement,
    // even though the player hasn't moved to a new block - that's what made the preview visibly jiggle while
    // just rotating the camera in place.
    private static final double HYSTERESIS_MARGIN = 0.12;

    private FacingUtil() {}

    public static Direction facingOf(Vec3 look) {
        return facingOf(look, null);
    }

    public static Direction facingOf(Vec3 look, Direction previous) {
        double horizLen = Math.sqrt(look.x * look.x + look.z * look.z);
        if (Math.abs(look.y) > horizLen * 1.5) {
            return look.y > 0 ? Direction.UP : Direction.DOWN;
        }

        Direction candidate = Direction.getNearest(look.x, 0, look.z);
        if (previous == null || previous.getAxis() == Direction.Axis.Y || previous == candidate) {
            return candidate;
        }

        double candScore = horizontalAlignment(look, candidate);
        double prevScore = horizontalAlignment(look, previous);
        return candScore > prevScore + HYSTERESIS_MARGIN ? candidate : previous;
    }

    private static double horizontalAlignment(Vec3 look, Direction dir) {
        return look.x * dir.getStepX() + look.z * dir.getStepZ();
    }
}
