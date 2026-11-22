package ac.grim.grimac.punishengine;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.manager.PunishGroup;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import lombok.Getter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PunishEngine {
    private static final int MAX_BUFFER_SIZE = 100_000;

    protected static final String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss")
    );

    protected GrimPlayer player;

    private final Set<String> processing = ConcurrentHashMap.newKeySet();

    @Getter
    private final Deque<String> buffer = new ConcurrentLinkedDeque<>();

    public PunishEngine(GrimPlayer player) {
        this.player = player;
    }

    public boolean isProcessing() {
        return !processing.isEmpty();
    }

    protected void add(Runnable execute, long delay) {
        GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runDelayed(
                GrimAPI.INSTANCE.getGrimPlugin(),
                execute,
                delay
        );
    }

    public void add(Runnable execute, String checkName, long ticks) {
        if (!processing.isEmpty()) return;
        processing.add(checkName);

        add(() -> {
            try {
                execute.run();
            } finally {
                remove(checkName);
                saveToFile(checkName);
            }
        }, ticks);
    }

    public void remove(String checkName) {
        processing.remove(checkName);
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        if (processing.isEmpty()) return;

        addToBuffer(formatPacket(event));
    }

    public void onPacketSend(PacketSendEvent event) {
        if (processing.isEmpty()) return;

        addToBuffer(formatPacket(event));
    }

    private void addToBuffer(String packet) {
        buffer.addLast(packet);

        while (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.pollFirst();
        }
    }

    public void saveToFile(String checkName) {
        try {
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
            );

            File file = new File(
                    GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(),
                    "logs/" + player.getName() + "-" + checkName + "-" + timestamp + ".txt"
            );


            file.getParentFile().mkdirs();
            file.createNewFile();

            List<String> packets = bufferString();

            try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                for (String packet : packets) {
                    writer.write(packet);
                    writer.write(System.lineSeparator());
                }
            }

            System.out.println("Buffer size: " + buffer.size());
            System.out.println("Packets size: " + packets.size());

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private String formatPacket(ProtocolPacketEvent event) {
        String packet = event.getPacketType().getName();

        if (event instanceof PacketReceiveEvent receive) {
            if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                try {
                    WrapperPlayClientPlayerFlying flying =
                            new WrapperPlayClientPlayerFlying(receive);

                    packet = String.format(
                            "FLYING; isTeleport=%s, pos=%s, rot=%s, dx=%.6f, dy=%.6f, dz=%.6f, dp=%.6f, dyaw=%.6f",
                            player.packetStateData.lastPacketWasTeleport,
                            flying.hasPositionChanged(),
                            flying.hasRotationChanged(),
                            player.deltaX(),
                            player.deltaY(),
                            player.deltaZ(),
                            player.pitch - player.lastPitch,
                            player.yaw - player.lastYaw
                    );
                } catch (Exception exception) {
                    packet += "; parseError=" + exception.getMessage();
                }
            }
        }

        return packet;
    }

    protected List<String> bufferString() {
        return new ArrayList<>(buffer);
    }
}
