package TermometruDigital;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public final class SerialCommunication {

    private static final int BAUD_RATE = 115200;
    private static final int READ_CHUNK_LEN = 512;

    private SerialPort port;
    private final StringBuilder buffer = new StringBuilder();
    private Consumer<String> onLine;

    public String[] listPorts() {
        return Arrays.stream(SerialPort.getCommPorts())
                .map(SerialPort::getSystemPortName)
                .toArray(String[]::new);
    }

    public boolean open(String systemName) {
        close();

        SerialPort found = Arrays.stream(SerialPort.getCommPorts())
                .filter(p -> Objects.equals(p.getSystemPortName(), systemName))
                .findFirst()
                .orElse(null);

        if (found == null) return false;

        port = found;
        configure(port);
        if (!port.openPort()) {
            port = null;
            return false;
        }
        port.addDataListener(new DataAvailableListener());
        return true;
    }

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

    public void setOnLine(Consumer<String> listener) {
        this.onLine = listener;
    }

    private static void configure(SerialPort sp) {
        sp.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
    }

    private final class DataAvailableListener implements SerialPortDataListener {
        @Override public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
        @Override public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE || port == null) return;
            readAndEmitLines();
        }
    }

    private void readAndEmitLines() {
        int available = port.bytesAvailable();
        if (available <= 0) return;

        byte[] chunk = new byte[Math.min(available, READ_CHUNK_LEN)];
        int n = port.readBytes(chunk, chunk.length);
        if (n <= 0) return;

        buffer.append(new String(chunk, 0, n, StandardCharsets.UTF_8));

        String str = buffer.toString();
        String[] parts = str.split("\\R", -1);
        int complete = parts.length - 1;
        for (int i = 0; i < complete; i++) emit(parts[i].trim());
        buffer.setLength(0);
        buffer.append(parts[parts.length - 1]);
    }

    private void emit(String line) {
        if (onLine != null && !line.isEmpty()) onLine.accept(line);
    }
}
