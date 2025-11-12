package TermometruDigital;

import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Optional;

/**
 * JavaFX app structured for low complexity:
 *  - small, single-purpose methods
 *  - pure UI builders vs. side-effect logic
 *  - early returns; no deep nesting
 */
public class MainApp extends Application {

    // UI
    private Label tempLabel;
    private Rectangle barFill;
    private Rectangle barOutline;
    private ComboBox<String> portPicker;

    // Model
    private double currentTempC = Double.NaN;

    // Services
    private final SerialCommunication serial = new SerialCommunication();

    // Constants
    private static final double MIN_C = -10.0;
    private static final double MAX_C = 50.0;
    private static final double BAR_HEIGHT = 300.0;
    private static final double BAR_WIDTH = 50.0;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Termometru Digital");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        root.setTop(buildTopBar());
        root.setCenter(buildThermometer());

        Scene scene = new Scene(root, 540, 420);
        stage.setScene(scene);
        stage.show();

        wireSerial();
        refreshPorts();
    }

    @Override
    public void stop() {
        serial.close();
    }

    private HBox buildTopBar() {
        portPicker = new ComboBox<>();
        portPicker.setOnAction(e -> openSelectedPort());
        Label title = new Label("Port serial:");
        title.setFont(Font.font(16));

        HBox box = new HBox(10, title, portPicker);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private StackPane buildThermometer() {
        tempLabel = new Label("Temperatura: -- °C");
        tempLabel.setFont(Font.font(28));

        barFill = new Rectangle(BAR_WIDTH, 0, Color.DODGERBLUE);
        barFill.setArcWidth(20);
        barFill.setArcHeight(20);

        barOutline = new Rectangle(BAR_WIDTH, BAR_HEIGHT);
        barOutline.setFill(Color.TRANSPARENT);
        barOutline.setStroke(Color.BLACK);
        barOutline.setArcWidth(20);
        barOutline.setArcHeight(20);

        VBox thermometer = new VBox(10, tempLabel, new StackPane(barOutline, barFill));
        thermometer.setAlignment(Pos.CENTER);
        thermometer.setPadding(new Insets(16));

        StackPane container = new StackPane(thermometer);
        container.setPrefSize(420, 360);
        return container;
    }

    // ---------- Serial

    private void wireSerial() {
        serial.setOnLine(this::onSerialLine);
    }

    private void refreshPorts() {
        portPicker.getItems().setAll(serial.listPorts());
        if (!portPicker.getItems().isEmpty()) {
            portPicker.getSelectionModel().selectFirst();
            openSelectedPort();
        }
    }

    private void openSelectedPort() {
        String sel = portPicker.getValue();
        if (sel == null) return;
        boolean ok = serial.open(sel);
        if (!ok) {
            tempLabel.setText("Eroare la deschiderea portului");
        }
    }

    private void onSerialLine(String line) {
        // Accept either "T:23.5" or raw number
        parseTemperature(line).ifPresent(this::setTemperature);
    }

    private Optional<Double> parseTemperature(String line) {
        String s = line;
        int idx = line.indexOf(':');
        if (idx >= 0 && idx + 1 < line.length()) s = line.substring(idx + 1);
        try {
            return Optional.of(Double.parseDouble(s.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    // ---------- UI updates

    private void setTemperature(double celsius) {
        currentTempC = celsius;
        Platform.runLater(() -> {
            tempLabel.setText(String.format("Temperatura: %.1f °C", currentTempC));
            animateBar(toBarHeight(currentTempC));
            animateBarColor(currentTempC);
        });
    }

    private double toBarHeight(double c) {
        double clamped = Math.max(MIN_C, Math.min(MAX_C, c));
        double ratio = (clamped - MIN_C) / (MAX_C - MIN_C);
        return ratio * BAR_HEIGHT;
    }

    private void animateBar(double newHeight) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(barFill.heightProperty(), barFill.getHeight())),
                new KeyFrame(Duration.millis(250), new KeyValue(barFill.heightProperty(), Math.max(0, newHeight)))
        );
        t.play();
    }

    private void animateBarColor(double c) {
        Color target = colorForTemp(c);
        FillTransition ft = new FillTransition(Duration.millis(250), barFill, (Color) barFill.getFill(), target);
        ft.play();
    }

    private static Color colorForTemp(double c) {
        if (c <= 0) return Color.DODGERBLUE;
        if (c <= 20) return Color.LIGHTGREEN;
        if (c <= 30) return Color.GOLD;
        return Color.TOMATO;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
