package termometrudigital;

import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainApp extends Application {

    private static final String FAN_ON_TEXT  = "Ventilator ON";
    private static final String FAN_OFF_TEXT = "Ventilator OFF";

    private SerialCommunication serialComm;

    private Label tempLabel;
    private Rectangle thermometerBar;
    private ImageView alertImageView;
    private LineChart<Number, Number> tempChart;
    private XYChart.Series<Number, Number> tempSeries;

    // servicii
    private TemperatureStats temperatureStats;
    private FanController fanController;
    private PdfReportGenerator pdfReportGenerator;
    private SceneManager sceneManager;

    @Override
    public void start(Stage primaryStage) {
        // Panou termometru + butoane + grafic
        VBox tempBox = createTemperaturePanel(primaryStage);

        // Manager scene (junglă, plajă, deșert)
        sceneManager = new SceneManager();

        StackPane root = sceneManager.createRoot(tempBox);

        // imagine alertă (deasupra tuturor)
        alertImageView = createAlertImage();
        root.getChildren().add(alertImageView);

        Scene scene = new Scene(root, 600, 820);
        primaryStage.setTitle("Termometru Digital");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Generator PDF (folosește stats + chart)
        pdfReportGenerator = new PdfReportGenerator(temperatureStats, tempChart, tempLabel);

        // Pornește comunicația serială
        new Thread(this::setupSerialCommunication).start();
    }

    /* =================== UI =================== */

    private VBox createTemperaturePanel(Stage primaryStage) {
        tempLabel = new Label("Temperatura: -- °C");
        tempLabel.setFont(new Font("Arial", 32));

        thermometerBar = createThermometerBar();
        StackPane thermometerStack = createThermometerStack(thermometerBar);

        HBox thermoRow = new HBox(5, thermometerStack);
        thermoRow.setAlignment(Pos.CENTER);

        tempChart = createTemperatureChart();
        tempSeries = tempChart.getData().get(0);

        temperatureStats = new TemperatureStats(tempSeries);

        // controale ventilator
        ToggleButton fanToggle = new ToggleButton(FAN_OFF_TEXT);
        ToggleButton modeToggle = new ToggleButton("Mod manual");
        Slider fanThresholdSlider = createThresholdSlider();

        fanController = new FanController(
                fanToggle,
                modeToggle,
                fanThresholdSlider,
                tempLabel,
                FAN_ON_TEXT,
                FAN_OFF_TEXT
        );

        fanToggle.setOnAction(e -> fanController.handleFanToggle());
        modeToggle.setOnAction(e -> fanController.handleModeToggle());

        Label thresholdLabel = new Label("Prag ventilator: 30 °C");
        bindThresholdLabel(fanThresholdSlider, thresholdLabel);

        HBox buttonsRow = new HBox(10, modeToggle, fanToggle);
        buttonsRow.setAlignment(Pos.CENTER);

        VBox autoBox = new VBox(5, thresholdLabel, fanThresholdSlider);
        autoBox.setAlignment(Pos.CENTER);

        Button pdfButton = new Button("Generează raport PDF");
        pdfButton.setOnAction(e -> pdfReportGenerator.generate(primaryStage));

        VBox controls = new VBox(10, buttonsRow, autoBox, pdfButton);
        controls.setAlignment(Pos.CENTER);

        VBox tempBox = new VBox();
        tempBox.setPrefHeight(400);
        tempBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setVgrow(thermoRow, Priority.ALWAYS);
        tempBox.getChildren().addAll(
                thermoRow,
                tempLabel,
                controls,
                tempChart
        );
        tempBox.setSpacing(10);
        tempBox.setPadding(new Insets(0, 0, 50, 0));

        return tempBox;
    }

    private Rectangle createThermometerBar() {
        Rectangle bar = new Rectangle(50, 0, Color.BLUE);
        bar.setArcWidth(20);
        bar.setArcHeight(20);
        return bar;
    }

    private StackPane createThermometerStack(Rectangle bar) {
        Rectangle outline = new Rectangle(50, 300);
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.BLACK);
        outline.setStrokeWidth(2);
        outline.setArcWidth(20);
        outline.setArcHeight(20);

        StackPane stack = new StackPane();
        stack.setPrefSize(70, 320);
        stack.setMinHeight(320);
        stack.setMaxHeight(320);
        stack.setPadding(new Insets(10));
        stack.setAlignment(Pos.BOTTOM_CENTER);
        stack.getChildren().addAll(outline, bar);

        stack.setBackground(new Background(
                new BackgroundFill(Color.web("#f0f0f0", 0.2), new CornerRadii(15), Insets.EMPTY)));
        stack.setEffect(new javafx.scene.effect.DropShadow(10, Color.GRAY));
        stack.setBorder(new Border(new BorderStroke(
                Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(15),
                new BorderWidths(2)
        )));
        return stack;
    }

    private LineChart<Number, Number> createTemperatureChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Esantion");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temperatura (°C)");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Temperatura");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getData().add(series);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(200);
        return chart;
    }

    private Slider createThresholdSlider() {
        Slider slider = new Slider(20, 40, 30);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(5);
        slider.setMinorTickCount(4);
        slider.setBlockIncrement(1);
        slider.setDisable(true); // la început suntem în manual
        return slider;
    }

    private void bindThresholdLabel(Slider slider, Label thresholdLabel) {
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                thresholdLabel.setText(
                        String.format("Prag ventilator: %.0f °C", newVal.doubleValue())
                )
        );
    }

    private ImageView createAlertImage() {
        ImageView imageView = new ImageView();
        imageView.setImage(new Image("file:C:/Users/benib/Desktop/elmo-burn.jpg"));
        imageView.setFitWidth(400);
        imageView.setFitHeight(500);
        imageView.setPreserveRatio(false);
        imageView.setVisible(false);
        return imageView;
    }

    /* ============ Serial + update ============ */

    private void setupSerialCommunication() {
        serialComm = new SerialCommunication("COM3", 9600);
        fanController.setSerialCommunication(serialComm);

        if (!serialComm.openPort()) {
            Platform.runLater(() -> tempLabel.setText("COM port NOT available"));
            return;
        }

        serialComm.addDataListener(data -> Platform.runLater(() -> {
            try {
                float temp = Float.parseFloat(data.trim());
                updateThermometer(temp);
            } catch (NumberFormatException ex) {
                // ignorăm date invalide
            }
        }));
    }

    private void updateThermometer(float temperature) {
        tempLabel.setText(String.format("Temperatura: %.2f °C", temperature));
        temperatureStats.addSample(temperature);
        animateThermometerBar(temperature);
        alertImageView.setVisible(temperature >= 34);
        sceneManager.updateScenes(temperature);
        fanController.updateAutoFan(temperature);
    }

    private void animateThermometerBar(float temperature) {
        double maxTempValue = 100.0;
        double maxHeight = 300.0;
        double targetHeight = Math.min((temperature / maxTempValue) * maxHeight, maxHeight);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(thermometerBar.heightProperty(), targetHeight)
                )
        );
        timeline.play();

        Color targetColor;
        if (temperature < 30) {
            targetColor = Color.BLUE;
        } else if (temperature < 32) {
            targetColor = Color.GREEN;
        } else if (temperature < 34) {
            targetColor = Color.ORANGE;
        } else {
            targetColor = Color.RED;
        }

        FillTransition fillTransition = new FillTransition(Duration.millis(300), thermometerBar);
        fillTransition.setToValue(targetColor);
        fillTransition.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
