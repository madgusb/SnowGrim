package ac.grim.grimac.utils.collisions;

public record BlockPush(int x, int z) {
    public static final BlockPush NONE = new BlockPush(0, 0);

    public boolean isNone() {
        return x == 0 && z == 0;
    }
}
