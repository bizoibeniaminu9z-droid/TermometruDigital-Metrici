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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.regex.Pattern;

public class MainApp extends Application {

    private static final double MIN_C = -10.0;
    private static final double MAX_C = 50.0;
    private static final double BAR_HEIGHT = 300.0;
    private static final double BAR_WIDTH = 50.0;
    private static final int ANIM_MS = 250;
    private static final Pattern NUMERIC = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?");

    private static final Color[] COLORS = new Color[]{
            Color.DODGERBLUE, // <=0
            Color.LIGHTGREEN, // (0,20]
            Color.GOLD,       // (20,30]
            Color.TOMATO      // >30
    };

    private Label tempLabel;
    private Rectangle barFill;
    private Rectangle barOutline;
    private ComboBox<String> portPicker;

    private final SerialCommunication serial = new SerialCommunication();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Termometru Digital");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildTopBar());
        root.setCenter(buildThermometer());

        stage.setScene(new Scene(root, 540, 420));
        stage.show();

        serial.setOnLine(this::onSerialLine);
        refreshPorts();
    }

    @Override
    public void stop() {
        serial.close();
    }

    private VBox buildTopBar() {
        Label title = new Label("Port serial:");
        title.setFont(Font.font(16));
        portPicker = new ComboBox<>();
        portPicker.setOnAction(e -> openSelectedPort());

        VBox box = new VBox(8, title, portPicker);
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

        StackPane bar = new StackPane(barOutline, barFill);
        VBox v = new VBox(10, tempLabel, bar);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(16));
        return new StackPane(v);
    }

    private void refreshPorts() {
        String[] ports = serial.listPorts();
        portPicker.getItems().setAll(ports);
        if (ports.length > 0) {
            portPicker.getSelectionModel().select(0);
            openSelectedPort();
        }
    }

    private void openSelectedPort() {
        String sel = portPicker.getValue();
        if (sel == null) return;
        if (!serial.open(sel)) tempLabel.setText("Eroare la deschiderea portului");
    }

    private void onSerialLine(String line) {
        int idx = line.indexOf(':');
        String s = idx >= 0 ? line.substring(idx + 1) : line;
        String trimmed = s.trim();
        if (!NUMERIC.matcher(trimmed).matches()) return;
        double c = Double.parseDouble(trimmed);
        Platform.runLater(() -> updateUI(c));
    }

    private void updateUI(double celsius) {
        tempLabel.setText(String.format("Temperatura: %.1f °C", celsius));
        animateBar(toBarHeight(celsius));
        animateBarColor(celsius);
    }

    private double toBarHeight(double c) {
        double clamped = Math.max(MIN_C, Math.min(MAX_C, c));
        double ratio = (clamped - MIN_C) / (MAX_C - MIN_C);
        return ratio * BAR_HEIGHT;
    }

    private void animateBar(double newHeight) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(barFill.heightProperty(), barFill.getHeight())),
                new KeyFrame(Duration.millis(ANIM_MS), new KeyValue(barFill.heightProperty(), Math.max(0, newHeight)))
        );
        t.play();
    }

    private void animateBarColor(double c) {
        Color target = colorFor(c);
        FillTransition ft = new FillTransition(Duration.millis(ANIM_MS), barFill, (Color) barFill.getFill(), target);
        ft.play();
    }

    private static Color colorFor(double c) {
        int bucket = (c <= 0) ? 0 : (c <= 20) ? 1 : (c <= 30) ? 2 : 3;
        return COLORS[bucket];
    }

    public static void main(String[] args) {
        launch(args);
    }
}
