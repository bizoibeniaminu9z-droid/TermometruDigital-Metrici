package termometrudigital;

import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;

import java.io.IOException;

public class FanController {

    private final ToggleButton fanToggle;
    private final ToggleButton modeToggle;
    private final Slider thresholdSlider;
    private final Label statusLabel;
    private final String fanOnText;
    private final String fanOffText;

    private SerialCommunication serialComm;
    private boolean manualMode = true;
    private boolean fanOn = false;

    public FanController(ToggleButton fanToggle,
                         ToggleButton modeToggle,
                         Slider thresholdSlider,
                         Label statusLabel,
                         String fanOnText,
                         String fanOffText) {

        this.fanToggle = fanToggle;
        this.modeToggle = modeToggle;
        this.thresholdSlider = thresholdSlider;
        this.statusLabel = statusLabel;
        this.fanOnText = fanOnText;
        this.fanOffText = fanOffText;

        modeToggle.setSelected(true); // pornim în mod manual
    }

    public void setSerialCommunication(SerialCommunication serialComm) {
        this.serialComm = serialComm;
    }

    public void handleModeToggle() {
        manualMode = modeToggle.isSelected();

        if (manualMode) {
            modeToggle.setText("Mod manual");
            fanToggle.setDisable(false);
            thresholdSlider.setDisable(true);
        } else {
            modeToggle.setText("Mod automat");
            fanToggle.setDisable(true);
            thresholdSlider.setDisable(false);
        }
    }

    public void handleFanToggle() {
        if (!manualMode || serialComm == null) {
            return;
        }

        boolean on = fanToggle.isSelected();
        try {
            serialComm.writeByte((byte) (on ? '1' : '0'));
            fanToggle.setText(on ? fanOnText : fanOffText);
            fanOn = on;
        } catch (IOException e) {
            statusLabel.setText("Eroare trimitere comanda ventilator");
            e.printStackTrace();
        }
    }

    public void updateAutoFan(float temperature) {
        if (manualMode || serialComm == null) {
            return;
        }

        double threshold = thresholdSlider.getValue();
        boolean shouldBeOn = temperature >= threshold;

        if (shouldBeOn == fanOn) {
            return;
        }

        try {
            serialComm.writeByte((byte) (shouldBeOn ? '1' : '0'));
            fanOn = shouldBeOn;

            fanToggle.setSelected(shouldBeOn);
            fanToggle.setText(shouldBeOn ? fanOnText : fanOffText);
        } catch (IOException e) {
            statusLabel.setText("Eroare trimitere comanda ventilator (auto)");
            e.printStackTrace();
        }
    }
}
