package TermometruDigital;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Single-responsibility serial helper:
 *  - open/close a selected port
 *  - read data and emit complete lines via a listener
 *  - thin, testable methods; early returns; no nested logic
 */
public final class SerialCommunication {

    private static final int BAUD_RATE = 115200;
    private static final int READ_CHUNK_LEN = 512;

    private SerialPort port;
    private final StringBuilder buffer = new StringBuilder();
    private Consumer<String> onLine; // emits one logical line (trimmed, without CR/LF)

    public interface SerialDataListener {
        void onDataReceived(String data);
    }

    public SerialCommunication() {}

    // ---------- Public API

    /** Returns a copy of available ports' system names. */
    public String[] listPorts() {
        return Arrays.stream(SerialPort.getCommPorts())
                .map(SerialPort::getSystemPortName)
                .toArray(String[]::new);
    }

    /** Opens the first port whose system name matches exactly. */
    public boolean open(String systemName) {
        close(); // ensure clean state

        Optional<SerialPort> found = Arrays.stream(SerialPort.getCommPorts())
                .filter(p -> Objects.equals(p.getSystemPortName(), systemName))
                .findFirst();

        if (!found.isPresent()) return false;

        port = found.get();
        configure(port);
        if (!port.openPort()) {
            port = null;
            return false;
        }
        port.addDataListener(makeLineListener());
        return true;
    }

    /** Safe close; idempotent. */
    public void close() {
        if (port == null) return;
        try {
            port.removeDataListener();
            port.closePort();
        } finally {
            port = null;
            buffer.setLength(0);
        }
    }

    /** Register a line listener. New line is delivered without CR/LF. */
    public void setOnLine(Consumer<String> listener) {
        this.onLine = listener;
    }

    // ---------- Internals

    private static void configure(SerialPort sp) {
        sp.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
    }

    private SerialPortDataListener makeLineListener() {
        return new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }
            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE || port == null) return;
                readAvailableIntoBuffer();
                emitCompleteLines();
            }
        };
    }

    private void readAvailableIntoBuffer() {
        int available = port.bytesAvailable();
        if (available <= 0) return;
        byte[] chunk = new byte[Math.min(available, READ_CHUNK_LEN)];
        int n = port.readBytes(chunk, chunk.length);
        if (n > 0) buffer.append(new String(chunk, 0, n, StandardCharsets.UTF_8));
    }

    private void emitCompleteLines() {
        int idx;
        while ((idx = indexOfLineEnd(buffer)) >= 0) {
            String line = buffer.substring(0, idx);
            dropFromBuffer(idx);
            fire(line.trim());
        }
    }

    private static int indexOfLineEnd(CharSequence sb) {
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '\n' || c == '\r') return i;
        }
        return -1;
    }

    private void dropFromBuffer(int idxEndExclusive) {
        int i = idxEndExclusive;
        if (i < buffer.length() && buffer.charAt(i) == '\r') i++;
        if (i < buffer.length() && buffer.charAt(i) == '\n') i++;
        buffer.delete(0, i);
    }

    private void fire(String line) {
        if (onLine != null && !line.isEmpty()) {
            onLine.accept(line);
        }
    }
}
