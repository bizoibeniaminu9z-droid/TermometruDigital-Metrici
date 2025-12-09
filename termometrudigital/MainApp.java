package termometrudigital;

import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/* ============================================================
 *                      MAIN APP (UI + WIRING)
 * ============================================================ */

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
    private SceneManager sceneManager;
    private FanController fanController;
    private PdfReportGenerator pdfReportGenerator;

    @Override
    public void start(Stage primaryStage) {
        // panoul cu termometrul + controale ventilator + grafic
        VBox tempBox = createTemperaturePanel(primaryStage);

        // manager pentru scene (junglă, plajă, deșert)
        sceneManager = new SceneManager(tempBox);

        // root-ul principal
        StackPane root = sceneManager.getRoot();

        Scene scene = new Scene(root, 600, 820);
        primaryStage.setTitle("Termometru Digital");
        primaryStage.setScene(scene);
        primaryStage.show();

        // generator PDF (folosește stats + chart)
        pdfReportGenerator = new PdfReportGenerator(temperatureStats, tempChart, tempLabel);

        // pornim comunicația serială
        new Thread(this::setupSerialCommunication).start();
    }

    /* ===================  UI INIT  =================== */

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

        alertImageView = createAlertImage();

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
        stack.setEffect(new DropShadow(10, Color.GRAY));
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

    /* ===================  SERIAL + UPDATE  =================== */

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
                // Ignorăm datele invalide
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

/* ============================================================
 *                    TEMPERATURE STATS
 * ============================================================ */

class TemperatureStats {

    private final XYChart.Series<Number, Number> series;
    private final java.util.List<Double> history = new java.util.ArrayList<>();
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    private double sum;
    private int sampleIndex;

    TemperatureStats(XYChart.Series<Number, Number> series) {
        this.series = series;
    }

    void addSample(double temperature) {
        history.add(temperature);
        sum += temperature;
        min = Math.min(min, temperature);
        max = Math.max(max, temperature);
        sampleIndex++;
        series.getData().add(new XYChart.Data<>(sampleIndex, temperature));
    }

    java.util.List<Double> getHistory() {
        return history;
    }

    double getMin() {
        return min;
    }

    double getMax() {
        return max;
    }

    double getAverage() {
        return history.isEmpty() ? 0.0 : sum / history.size();
    }
}

/* ============================================================
 *                      FAN CONTROLLER
 * ============================================================ */

class FanController {

    private final ToggleButton fanToggle;
    private final ToggleButton modeToggle;
    private final Slider thresholdSlider;
    private final Label statusLabel;
    private final String fanOnText;
    private final String fanOffText;

    private SerialCommunication serialComm;
    private boolean manualMode = true;
    private boolean fanOn = false;

    FanController(ToggleButton fanToggle,
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

    void setSerialCommunication(SerialCommunication serialComm) {
        this.serialComm = serialComm;
    }

    void handleModeToggle() {
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

    void handleFanToggle() {
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

    void updateAutoFan(float temperature) {
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

/* ============================================================
 *                      PDF REPORT GENERATOR
 * ============================================================ */

class PdfReportGenerator {

    private final TemperatureStats stats;
    private final LineChart<Number, Number> chart;
    private final Label statusLabel;

    PdfReportGenerator(TemperatureStats stats,
                       LineChart<Number, Number> chart,
                       Label statusLabel) {
        this.stats = stats;
        this.chart = chart;
        this.statusLabel = statusLabel;
    }

    void generate(Stage owner) {
        if (stats.getHistory().isEmpty()) {
            statusLabel.setText("Nu există date pentru raport.");
            return;
        }

        File file = chooseDestination(owner);
        if (file == null) {
            return;
        }

        try {
            File chartFile = createChartImageTempFile();
            writePdf(file, chartFile);
            chartFile.deleteOnExit();
            statusLabel.setText("Raport PDF generat.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare la generarea PDF-ului.");
        }
    }

    private File chooseDestination(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvează raport PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(owner);
    }

    private File createChartImageTempFile() throws IOException {
        WritableImage fxImage = chart.snapshot(null, null);
        BufferedImage bImage = SwingFXUtils.fromFXImage(fxImage, null);
        File chartFile = File.createTempFile("chart_temp_", ".png");
        ImageIO.write(bImage, "png", chartFile);
        return chartFile;
    }

    private void writePdf(File file, File chartFile) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        document.add(new Paragraph("Raport temperatura - sesiune curenta"));
        document.add(new Paragraph(" "));

        addStatistics(document);
        addTable(document);
        addChartImage(document, chartFile);

        document.close();
    }

    private void addStatistics(Document document) throws Exception {
        int count = stats.getHistory().size();
        document.add(new Paragraph(String.format("Numar masuratori: %d", count)));
        document.add(new Paragraph(String.format("Temperatura minima: %.2f °C", stats.getMin())));
        document.add(new Paragraph(String.format("Temperatura maxima: %.2f °C", stats.getMax())));
        document.add(new Paragraph(String.format("Temperatura medie: %.2f °C", stats.getAverage())));
        document.add(new Paragraph(" "));
    }

    private void addTable(Document document) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.addCell("Index");
        table.addCell("Temperatura (°C)");

        java.util.List<Double> history = stats.getHistory();
        for (int i = 0; i < history.size(); i++) {
            table.addCell(Integer.toString(i + 1));
            table.addCell(String.format("%.2f", history.get(i)));
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addChartImage(Document document, File chartFile) throws Exception {
        com.itextpdf.text.Image chartImg =
                com.itextpdf.text.Image.getInstance(chartFile.getAbsolutePath());
        chartImg.scaleToFit(500, 400);
        document.add(chartImg);
    }
}

/* ============================================================
 *                        SCENE MANAGER
 * ============================================================ */

class SceneManager {

    private final StackPane root;
    private final ImageView backgroundImageView;
    private final ImageView palmier1;
    private final ImageView palmier2;
    private final ImageView maimuta;
    private final ImageView backgroundImageView1;
    private final ImageView sezlong;
    private final ImageView soare;
    private final ImageView palmierPlaja;
    private final ImageView backgroundImageView2;
    private final ImageView cactus1;
    private final ImageView cactus2;
    private final ImageView camila;
    private final ImageView alertImageView; // doar ca să fie deasupra (primim din MainApp)

    SceneManager(VBox tempBox) {
        // imaginile
        alertImageView = null; // este adăugat de MainApp, aici nu avem nevoie de referință

        backgroundImageView = createImage(
                "file:C:/Users/benib/Desktop/tropical-ground.png",
                600, true, 500, 0, false);

        palmier1 = createImage(
                "file:C:/Users/benib/Desktop/palmier1.png",
                500, true, -50, 500, false);
        palmier2 = createImage(
                "file:C:/Users/benib/Desktop/palmier2.png",
                600, true, 50, 500, false);

        maimuta = createImage(
                "file:C:/Users/benib/Desktop/maimuta.png",
                300, true, 500, 170, false);

        backgroundImageView1 = createImage(
                "file:C:/Users/benib/Desktop/beach-ground.png",
                600, true, 500, 0, false);

        sezlong = createImage(
                "file:C:/Users/benib/Desktop/sezlong.png",
                150, true, 130, 500, false);

        soare = createImage(
                "file:C:/Users/benib/Desktop/soare.png",
                150, true, 500, 200, false);

        palmierPlaja = createImage(
                "file:C:/Users/benib/Desktop/palmier1.png",
                700, true, -50, 500, false);

        backgroundImageView2 = createImage(
                "file:C:/Users/benib/Desktop/desert-ground.png",
                800, true, 500, 0, false);

        cactus1 = createImage(
                "file:C:/Users/benib/Desktop/cactus1.png",
                250, true, 100, 500, false);
        cactus2 = createImage(
                "file:C:/Users/benib/Desktop/cactus1.png",
                250, true, 90, 500, false);

        camila = createImage(
                "file:C:/Users/benib/Desktop/camila.png",
                450, true, 500, 0, false);

        root = new StackPane(
                backgroundImageView,
                backgroundImageView1,
                backgroundImageView2,
                cactus1,
                cactus2,
                camila,
                palmier1,
                palmier2,
                palmierPlaja,
                soare,
                maimuta,
                sezlong,
                tempBox
        );
    }

    StackPane getRoot() {
        return root;
    }

    void updateScenes(float temperature) {
        updateScene(
                temperature,
                22, 28,
                backgroundImageView,
                () -> {
                    showWithBounce(backgroundImageView, "y", 500, -20, 0);
                    showWithBounce(palmier1, "x", -500, -180, -200);
                    showWithBounce(palmier2, "x", 500, 130, 150);
                    showWithBounce(maimuta, "y", -500, -180, -300);
                },
                () -> {
                    hideWithBounce(backgroundImageView, "y", 0, -20, 500);
                    hideWithBounce(palmier1, "x", -200, -180, -500);
                    hideWithBounce(palmier2, "x", 150, 130, 500);
                    hideWithBounce(maimuta, "y", -200, -180, -500);
                }
        );

        updateScene(
                temperature,
                30, 31,
                backgroundImageView1,
                () -> {
                    showWithBounce(backgroundImageView1, "y", 500, -20, 0);
                    showWithBounce(sezlong, "x", 500, 80, 100);
                    showWithBounce(soare, "y", -500, -180, -350);
                    showWithBounce(palmierPlaja, "x", -500, -130, -300);
                },
                () -> {
                    hideWithBounce(backgroundImageView1, "y", 0, -20, 500);
                    hideWithBounce(sezlong, "x", 100, 80, 500);
                    hideWithBounce(soare, "y", -200, -180, -500);
                    hideWithBounce(palmierPlaja, "x", -150, -130, -500);
                }
        );

        updateScene(
                temperature,
                32, 34,
                backgroundImageView2,
                () -> {
                    showWithBounce(backgroundImageView2, "y", 500, -20, 0);
                    showWithBounce(cactus1, "x", 500, 80, 100);
                    showWithBounce(cactus2, "x", -500, -100, -120);
                    showWithBounce(camila, "y", 500, 80, 100);
                },
                () -> {
                    hideWithBounce(backgroundImageView2, "y", 0, -20, 500);
                    hideWithBounce(cactus1, "x", 100, 80, 500);
                    hideWithBounce(cactus2, "x", -120, -100, -500);
                    hideWithBounce(camila, "y", 100, 80, 500);
                }
        );
    }

    private void updateScene(
            float temperature,
            double minShow,
            double maxShow,
            ImageView background,
            Runnable showAction,
            Runnable hideAction) {

        boolean shouldBeVisible = temperature >= minShow && temperature <= maxShow;

        if (shouldBeVisible == background.isVisible()) {
            return;
        }

        if (shouldBeVisible) {
            showAction.run();
        } else {
            hideAction.run();
        }
    }

    private ImageView createImage(
            String path,
            double fitWidth,
            boolean preserveRatio,
            double translateY,
            double translateX,
            boolean visible) {

        ImageView imageView = new ImageView();
        imageView.setImage(new Image(path));
        imageView.setFitWidth(fitWidth);
        imageView.setPreserveRatio(preserveRatio);
        imageView.setTranslateY(translateY);
        imageView.setTranslateX(translateX);
        imageView.setVisible(visible);
        return imageView;
    }

    private void showWithBounce(Node node, String axis, double startPos, double bouncePos, double finalPos) {
        node.setVisible(true);

        Timeline bounceIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(getTranslateProperty(node, axis), startPos)
                ),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(getTranslateProperty(node, axis), bouncePos)
                ),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(getTranslateProperty(node, axis), finalPos)
                )
        );
        bounceIn.play();
    }

    private void hideWithBounce(Node node, String axis, double startPos, double bouncePos, double endPos) {
        Timeline bounceOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(getTranslateProperty(node, axis), startPos)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(getTranslateProperty(node, axis), bouncePos)
                ),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(getTranslateProperty(node, axis), endPos)
                )
        );
        bounceOut.setOnFinished(event -> node.setVisible(false));
        bounceOut.play();
    }

    private javafx.beans.property.DoubleProperty getTranslateProperty(Node node, String axis) {
        if (axis.equalsIgnoreCase("x")) {
            return node.translateXProperty();
        }
        return node.translateYProperty();
    }
}
