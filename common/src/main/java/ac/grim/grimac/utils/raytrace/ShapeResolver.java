package ac.grim.grimac.utils.raytrace;

import ac.grim.grimac.utils.raytrace.StateBoxes.Box;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ShapeResolver {
    private static final double P = 1.0 / 16.0;

    private static Box px(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Box(x1 * P, y1 * P, z1 * P, x2 * P, y2 * P, z2 * P);
    }

    private static final Box FULL = StateBoxes.FULL;
    private static final Box SLAB_BOTTOM = new Box(0, 0, 0, 1, 0.5, 1);
    private static final Box SLAB_TOP = new Box(0, 0.5, 0, 1, 1, 1);

    public static List<Box> getBoxes(WrappedBlockState s, int x, int z) {
        StateType t = s.getType();
        if (StateBoxes.isEmpty(t)) return Collections.emptyList();
        if (!StateBoxes.isStateDependent(t)) return Collections.singletonList(StateBoxes.getBox(t, x, z));

        List<Box> r = resolve(s, t.getName().toLowerCase());
        return r != null ? r : Collections.singletonList(StateBoxes.getBox(t));
    }

    private static List<Box> resolve(WrappedBlockState s, String n) {
        if (n.endsWith("wall_torch")) return one(wallTorch(s.getFacing()));
        if (n.endsWith("wall_sign")) return one(wallSign(s.getFacing()));
        if (n.endsWith("wall_banner")) return one(wallBanner(s.getFacing()));
        if (n.endsWith("wall_head") || n.endsWith("wall_skull")) return one(wallHead(s.getFacing()));

        if (n.endsWith("_slab")) return slab(s);
        if (n.endsWith("_stairs")) return stairs(s);
        if (n.endsWith("_trapdoor")) return one(trapdoor(s));
        if (n.endsWith("_door")) return one(door(s));
        if (n.endsWith("_fence_gate")) return one(gate(s));
        if (n.endsWith("_fence")) return fence(s);
        if (n.endsWith("_wall")) return wall(s);
        if (n.endsWith("_pane") || n.endsWith("bars")) return bars(s);
        if (n.endsWith("chain")) return one(chain(s));
        if (n.endsWith("ladder")) return one(ladder(s.getFacing()));
        if (n.endsWith("_button")) return one(button(s));
        if (n.equals("lever")) return one(lever(s));
        if (n.equals("snow")) return one(snow(s));
        if (n.endsWith("candle")) return one(candle(s));
        if (n.endsWith("anvil")) return anvil(s);
        if (n.endsWith("lantern")) return lantern(s);
        if (n.equals("end_rod")) return one(endRod(s.getFacing()));
        if (n.endsWith("cauldron")) return cauldron();
        if (n.equals("hopper")) return hopper();
        if (n.equals("composter")) return composter();
        return null;
    }

    private static List<Box> one(Box b) {
        return Collections.singletonList(b);
    }

    private static boolean isEast(Enum<?> e) {
        String nm = e.name();
        return !nm.equals("FALSE") && !nm.equals("NONE");
    }

    private static boolean isX(BlockFace f) {
        return f == BlockFace.EAST || f == BlockFace.WEST;
    }

    private static List<Box> slab(WrappedBlockState s) {
        switch (s.getTypeData().name()) {
            case "TOP": return one(SLAB_TOP);
            case "DOUBLE": return one(FULL);
            default: return one(SLAB_BOTTOM);
        }
    }

    private static List<Box> stairs(WrappedBlockState s) {
        boolean top = s.getHalf().name().equals("TOP");
        BlockFace f = s.getFacing();
        String shape = s.getShape().name();

        List<Box> out = new ArrayList<>(3);
        out.add(top ? SLAB_TOP : SLAB_BOTTOM);

        double y0 = top ? 0 : 0.5, y1 = top ? 0.5 : 1;
        BlockFace left = left(f);
        BlockFace side = shape.endsWith("LEFT") ? left : left.getOppositeFace();

        if (shape.startsWith("OUTER")) {
            out.add(quarter(f, side, y0, y1));
        } else {
            out.add(half(f, y0, y1));
            if (shape.startsWith("INNER")) out.add(quarter(f.getOppositeFace(), side, y0, y1));
        }
        return out;
    }

    private static BlockFace left(BlockFace f) {
        switch (f) {
            case NORTH: return BlockFace.WEST;
            case SOUTH: return BlockFace.EAST;
            case EAST: return BlockFace.NORTH;
            default: return BlockFace.SOUTH;
        }
    }

    private static Box half(BlockFace d, double y0, double y1) {
        switch (d) {
            case NORTH: return new Box(0, y0, 0, 1, y1, 0.5);
            case SOUTH: return new Box(0, y0, 0.5, 1, y1, 1);
            case WEST: return new Box(0, y0, 0, 0.5, y1, 1);
            default: return new Box(0.5, y0, 0, 1, y1, 1);
        }
    }

    private static Box quarter(BlockFace a, BlockFace b, double y0, double y1) {
        double minX = 0, maxX = 1, minZ = 0, maxZ = 1;
        for (BlockFace d : new BlockFace[]{a, b}) {
            switch (d) {
                case WEST: maxX = 0.5; break;
                case EAST: minX = 0.5; break;
                case NORTH: maxZ = 0.5; break;
                case SOUTH: minZ = 0.5; break;
                default: break;
            }
        }
        return new Box(minX, y0, minZ, maxX, y1, maxZ);
    }

    private static Box trapdoor(WrappedBlockState s) {
        if (!s.isOpen()) {
            return s.getHalf().name().equals("TOP") ? px(0, 13, 0, 16, 16, 16) : px(0, 0, 0, 16, 3, 16);
        }
        switch (s.getFacing()) {
            case NORTH: return px(0, 0, 13, 16, 16, 16);
            case SOUTH: return px(0, 0, 0, 16, 16, 3);
            case WEST: return px(13, 0, 0, 16, 16, 16);
            default: return px(0, 0, 0, 3, 16, 16);
        }
    }

    private static Box door(WrappedBlockState s) {
        final Box south = px(0, 0, 0, 16, 16, 3), north = px(0, 0, 13, 16, 16, 16);
        final Box west = px(13, 0, 0, 16, 16, 16), east = px(0, 0, 0, 3, 16, 16);
        boolean closed = !s.isOpen();
        boolean hingeRight = s.getHinge().name().equals("RIGHT");
        switch (s.getFacing()) {
            case EAST: return closed ? east : (hingeRight ? north : south);
            case SOUTH: return closed ? south : (hingeRight ? east : west);
            case WEST: return closed ? west : (hingeRight ? south : north);
            default: return closed ? north : (hingeRight ? west : east);
        }
    }

    private static Box gate(WrappedBlockState s) {
        boolean inWall = s.isInWall();
        double h = inWall ? 13 : 16;
        return isX(s.getFacing()) ? px(6, 0, 0, 10, h, 16) : px(0, 0, 6, 16, h, 10);
    }

    private static List<Box> fence(WrappedBlockState s) {
        List<Box> l = new ArrayList<>(5);
        l.add(px(6, 0, 6, 10, 16, 10));
        if (isEast(s.getNorth())) l.add(px(7, 6, 0, 9, 15, 6));
        if (isEast(s.getSouth())) l.add(px(7, 6, 10, 9, 15, 16));
        if (isEast(s.getWest())) l.add(px(0, 6, 7, 6, 15, 9));
        if (isEast(s.getEast())) l.add(px(10, 6, 7, 16, 15, 9));
        return l;
    }

    private static List<Box> bars(WrappedBlockState s) {
        List<Box> l = new ArrayList<>(5);
        l.add(px(7, 0, 7, 9, 16, 9));
        if (isEast(s.getNorth())) l.add(px(7, 0, 0, 9, 16, 7));
        if (isEast(s.getSouth())) l.add(px(7, 0, 9, 9, 16, 16));
        if (isEast(s.getWest())) l.add(px(0, 0, 7, 7, 16, 9));
        if (isEast(s.getEast())) l.add(px(9, 0, 7, 16, 16, 9));
        return l;
    }

    private static List<Box> wall(WrappedBlockState s) {
        List<Box> l = new ArrayList<>(5);
        if (s.isUp()) l.add(px(4, 0, 4, 12, 16, 12));
        double nH = wallH(s.getNorth()), sH = wallH(s.getSouth()), wH = wallH(s.getWest()), eH = wallH(s.getEast());
        if (nH > 0) l.add(px(5, 0, 0, 11, nH, 8));
        if (sH > 0) l.add(px(5, 0, 8, 11, sH, 16));
        if (wH > 0) l.add(px(0, 0, 5, 8, wH, 11));
        if (eH > 0) l.add(px(8, 0, 5, 16, eH, 11));
        if (l.isEmpty()) l.add(px(4, 0, 4, 12, 16, 12));
        return l;
    }

    private static double wallH(Enum<?> e) {
        switch (e.name()) {
            case "LOW": return 14;
            case "TALL":
            case "TRUE": return 16;
            default: return 0;
        }
    }

    private static Box chain(WrappedBlockState s) {
        switch (s.getAxis().name()) {
            case "X": return px(0, 6.5, 6.5, 16, 9.5, 9.5);
            case "Z": return px(6.5, 6.5, 0, 9.5, 9.5, 16);
            default: return px(6.5, 0, 6.5, 9.5, 16, 9.5);
        }
    }

    private static Box ladder(BlockFace f) {
        switch (f) {
            case NORTH: return px(0, 0, 13, 16, 16, 16);
            case SOUTH: return px(0, 0, 0, 16, 16, 3);
            case WEST: return px(13, 0, 0, 16, 16, 16);
            default: return px(0, 0, 0, 3, 16, 16);
        }
    }

    private static Box button(WrappedBlockState s) {
        double t = s.isPowered() ? 1 : 2;
        BlockFace f = s.getFacing();
        switch (s.getFace().name()) {
            case "FLOOR":
                return isX(f) ? px(6, 0, 5, 10, t, 11) : px(5, 0, 6, 11, t, 10);
            case "CEILING":
                return isX(f) ? px(6, 16 - t, 5, 10, 16, 11) : px(5, 16 - t, 6, 11, 16, 10);
            default:
                switch (f) {
                    case NORTH: return px(5, 6, 16 - t, 11, 10, 16);
                    case SOUTH: return px(5, 6, 0, 11, 10, t);
                    case WEST: return px(16 - t, 6, 5, 16, 10, 11);
                    default: return px(0, 6, 5, t, 10, 11);
                }
        }
    }

    private static Box lever(WrappedBlockState s) {
        BlockFace f = s.getFacing();
        switch (s.getFace().name()) {
            case "FLOOR":
                return isX(f) ? px(4, 0, 5, 12, 6, 11) : px(5, 0, 4, 11, 6, 12);
            case "CEILING":
                return isX(f) ? px(4, 10, 5, 12, 16, 11) : px(5, 10, 4, 11, 16, 12);
            default:
                switch (f) {
                    case NORTH: return px(5, 4, 10, 11, 12, 16);
                    case SOUTH: return px(5, 4, 0, 11, 12, 6);
                    case WEST: return px(10, 4, 5, 16, 12, 11);
                    default: return px(0, 4, 5, 6, 12, 11);
                }
        }
    }

    private static Box wallTorch(BlockFace f) {
        switch (f) {
            case NORTH: return px(5.5, 3, 11, 10.5, 13, 16);
            case SOUTH: return px(5.5, 3, 0, 10.5, 13, 5);
            case WEST: return px(11, 3, 5.5, 16, 13, 10.5);
            default: return px(0, 3, 5.5, 5, 13, 10.5);
        }
    }

    private static Box wallSign(BlockFace f) {
        switch (f) {
            case NORTH: return px(0, 4.5, 14, 16, 12.5, 16);
            case SOUTH: return px(0, 4.5, 0, 16, 12.5, 2);
            case WEST: return px(14, 4.5, 0, 16, 12.5, 16);
            default: return px(0, 4.5, 0, 2, 12.5, 16);
        }
    }

    private static Box wallBanner(BlockFace f) {
        switch (f) {
            case NORTH: return px(0, 0, 14, 16, 12.5, 16);
            case SOUTH: return px(0, 0, 0, 16, 12.5, 2);
            case WEST: return px(14, 0, 0, 16, 12.5, 16);
            default: return px(0, 0, 0, 2, 12.5, 16);
        }
    }

    private static Box wallHead(BlockFace f) {
        switch (f) {
            case NORTH: return px(4, 4, 8, 12, 12, 16);
            case SOUTH: return px(4, 4, 0, 12, 12, 8);
            case WEST: return px(8, 4, 4, 16, 12, 12);
            default: return px(0, 4, 4, 8, 12, 12);
        }
    }

    private static Box snow(WrappedBlockState s) {
        int layers = Math.max(1, Math.min(8, s.getLayers()));
        return new Box(0, 0, 0, 1, layers * 2 * P, 1);
    }

    private static Box candle(WrappedBlockState s) {
        switch (s.getCandles()) {
            case 2: return px(5, 0, 6, 11, 6, 9);
            case 3: return px(5, 0, 6, 10, 6, 11);
            case 4: return px(5, 0, 5, 11, 6, 11);
            default: return px(7, 0, 7, 9, 6, 9);
        }
    }

    private static List<Box> anvil(WrappedBlockState s) {
        if (isX(s.getFacing())) {
            return Arrays.asList(px(2, 0, 2, 14, 4, 14), px(3, 4, 4, 13, 5, 12),
                    px(4, 5, 6, 12, 10, 10), px(0, 10, 3, 16, 16, 13));
        }
        return Arrays.asList(px(2, 0, 2, 14, 4, 14), px(4, 4, 3, 12, 5, 13),
                px(6, 5, 4, 10, 10, 12), px(3, 10, 0, 13, 16, 16));
    }

    private static List<Box> lantern(WrappedBlockState s) {
        if (s.isHanging()) return Arrays.asList(px(5, 1, 5, 11, 8, 11), px(6, 8, 6, 10, 10, 10));
        return Arrays.asList(px(5, 0, 5, 11, 7, 11), px(6, 7, 6, 10, 9, 10));
    }

    private static Box endRod(BlockFace f) {
        switch (f) {
            case NORTH:
            case SOUTH: return px(6, 6, 0, 10, 10, 16);
            case EAST:
            case WEST: return px(0, 6, 6, 16, 10, 10);
            default: return px(6, 0, 6, 10, 16, 10);
        }
    }

    private static List<Box> cauldron() {
        return Arrays.asList(px(0, 0, 0, 16, 4, 16), px(0, 4, 0, 2, 16, 16), px(14, 4, 0, 16, 16, 16),
                px(2, 4, 0, 14, 16, 2), px(2, 4, 14, 14, 16, 16));
    }

    private static List<Box> hopper() {
        return Arrays.asList(px(0, 10, 0, 16, 11, 16), px(4, 4, 4, 12, 10, 12), px(6, 0, 6, 10, 4, 10),
                px(0, 11, 0, 2, 16, 16), px(14, 11, 0, 16, 16, 16),
                px(2, 11, 0, 14, 16, 2), px(2, 11, 14, 14, 16, 16));
    }

    private static List<Box> composter() {
        return Arrays.asList(px(0, 0, 0, 16, 2, 16), px(0, 2, 0, 2, 16, 16), px(14, 2, 0, 16, 16, 16),
                px(2, 2, 0, 14, 16, 2), px(2, 2, 14, 14, 16, 16));
    }
}
