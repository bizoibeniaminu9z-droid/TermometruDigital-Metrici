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
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class MainApp extends Application {

    // constante pentru textul butonului de ventilator
    private static final String FAN_ON_TEXT  = "Ventilator ON";
    private static final String FAN_OFF_TEXT = "Ventilator OFF";

    private SerialCommunication serialComm;
    private Label tempLabel;
    private Rectangle thermometerBar;
    private ImageView alertImageView;
    private ImageView backgroundImageView;
    private ImageView palmier1;
    private ImageView palmier2;
    private ImageView maimuta;
    private ImageView backgroundImageView1;
    private ImageView sezlong;
    private ImageView soare;
    private ImageView palmierPlaja;
    private ImageView backgroundImageView2;
    private ImageView cactus1;
    private ImageView cactus2;
    private ImageView camila;

    // Control ventilator
    private ToggleButton fanToggle;

    // Mod manual / automat + slider prag
    private ToggleButton modeToggle;
    private Slider fanThresholdSlider;
    private boolean isManualMode = true;
    private boolean isFanOn = false;

    // Istoric temperatură + grafic
    private LineChart<Number, Number> tempChart;
    private XYChart.Series<Number, Number> tempSeries;
    private int sampleIndex = 0;
    private final List<Double> tempHistory = new ArrayList<>();
    private double minTemp = Double.POSITIVE_INFINITY;
    private double maxTemp = Double.NEGATIVE_INFINITY;
    private double sumTemp = 0.0;

    @Override
    public void start(Stage primaryStage) {
        VBox tempBox = createTemperaturePanel(primaryStage);
        initImages();
        StackPane root = createRootLayout(tempBox);

        Scene scene = new Scene(root, 600, 820);
        primaryStage.setTitle("Termometru Digital");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(this::setupSerialCommunication).start();
    }

    /* ===================  UI INIT  =================== */

    private VBox createTemperaturePanel(Stage primaryStage) {
        tempLabel = createTempLabel();
        StackPane thermometerStack = createThermometerStack();
        HBox thermoRow = createThermometerRow(thermometerStack);

        LineChart<Number, Number> chart = createTemperatureChart();
        VBox fanControlsBox = createFanControls(primaryStage);

        VBox tempBox = new VBox();
        tempBox.setPrefHeight(400);
        tempBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setVgrow(thermoRow, Priority.ALWAYS);
        tempBox.getChildren().addAll(
                thermoRow,
                tempLabel,
                fanControlsBox,
                chart
        );
        tempBox.setSpacing(10);
        tempBox.setPadding(new Insets(0, 0, 50, 0));
        return tempBox;
    }

    private Label createTempLabel() {
        Label label = new Label("Temperatura: -- °C");
        label.setFont(new Font("Arial", 32));
        return label;
    }

    private StackPane createThermometerStack() {
        thermometerBar = new Rectangle(50, 0, Color.BLUE);
        thermometerBar.setArcWidth(20);
        thermometerBar.setArcHeight(20);

        Rectangle thermometerOutline = new Rectangle(50, 300);
        thermometerOutline.setFill(Color.TRANSPARENT);
        thermometerOutline.setStroke(Color.BLACK);
        thermometerOutline.setStrokeWidth(2);
        thermometerOutline.setArcWidth(20);
        thermometerOutline.setArcHeight(20);

        StackPane thermometerStack = new StackPane();
        thermometerStack.setPrefSize(70, 320);
        thermometerStack.setMinHeight(320);
        thermometerStack.setMaxHeight(320);
        thermometerStack.setPadding(new Insets(10));
        thermometerStack.setAlignment(Pos.BOTTOM_CENTER);
        thermometerStack.getChildren().addAll(thermometerOutline, thermometerBar);

        thermometerStack.setBackground(new Background(
                new BackgroundFill(Color.web("#f0f0f0", 0.2), new CornerRadii(15), Insets.EMPTY)));
        thermometerStack.setEffect(new DropShadow(10, Color.GRAY));
        thermometerStack.setBorder(new Border(new BorderStroke(
                Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(15),
                new BorderWidths(2)
        )));
        return thermometerStack;
    }

    private HBox createThermometerRow(StackPane thermometerStack) {
        HBox thermoRow = new HBox(5, thermometerStack);
        thermoRow.setAlignment(Pos.CENTER);
        thermoRow.setPadding(new Insets(0));
        return thermoRow;
    }

    private LineChart<Number, Number> createTemperatureChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Esantion");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temperatura (°C)");

        tempSeries = new XYChart.Series<>();
        tempSeries.setName("Temperatura");

        tempChart = new LineChart<>(xAxis, yAxis);
        tempChart.getData().add(tempSeries);
        tempChart.setCreateSymbols(false);
        tempChart.setLegendVisible(false);
        tempChart.setPrefHeight(200);
        return tempChart;
    }

    private VBox createFanControls(Stage primaryStage) {
        fanToggle = new ToggleButton(FAN_OFF_TEXT);
        fanToggle.setOnAction(e -> handleFanToggle());

        modeToggle = new ToggleButton("Mod manual");
        modeToggle.setSelected(true); // pornim în mod manual
        modeToggle.setOnAction(e -> handleModeToggle());

        fanThresholdSlider = createThresholdSlider();
        Label thresholdLabel = createThresholdLabel();
        bindThresholdLabel(thresholdLabel);

        HBox buttonsRow = new HBox(10, modeToggle, fanToggle);
        buttonsRow.setAlignment(Pos.CENTER);

        VBox autoBox = new VBox(5, thresholdLabel, fanThresholdSlider);
        autoBox.setAlignment(Pos.CENTER);

        Button pdfButton = new Button("Generează raport PDF");
        pdfButton.setOnAction(e -> generatePdfReport(primaryStage));

        VBox controls = new VBox(10, buttonsRow, autoBox, pdfButton);
        controls.setAlignment(Pos.CENTER);
        return controls;
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

    private Label createThresholdLabel() {
        return new Label("Prag ventilator: 30 °C");
    }

    private void bindThresholdLabel(Label thresholdLabel) {
        fanThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                thresholdLabel.setText(
                        String.format("Prag ventilator: %.0f °C", newVal.doubleValue())
                )
        );
    }

    private void initImages() {
        alertImageView = new ImageView();
        alertImageView.setImage(new Image("file:C:/Users/benib/Desktop/elmo-burn.jpg"));
        alertImageView.setFitWidth(400);
        alertImageView.setFitHeight(500);
        alertImageView.setPreserveRatio(false);
        alertImageView.setVisible(false);

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
    }

    private ImageView createImage(String path,
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

    private StackPane createRootLayout(VBox tempBox) {
        return new StackPane(
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
                tempBox,
                alertImageView
        );
    }

    /* ===================  SERIAL & FAN  =================== */

    private void setupSerialCommunication() {
        serialComm = new SerialCommunication("COM3", 9600);
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

    // Comutare mod manual / automat
    private void handleModeToggle() {
        isManualMode = modeToggle.isSelected();

        if (isManualMode) {
            modeToggle.setText("Mod manual");
            fanToggle.setDisable(false);
            fanThresholdSlider.setDisable(true);
        } else {
            modeToggle.setText("Mod automat");
            fanToggle.setDisable(true);
            fanThresholdSlider.setDisable(false);
        }
    }

    // Control manual ventilator
    private void handleFanToggle() {
        if (!isManualMode || serialComm == null) {
            return;
        }

        boolean on = fanToggle.isSelected();
        try {
            serialComm.writeByte((byte) (on ? '1' : '0'));
            fanToggle.setText(on ? FAN_ON_TEXT : FAN_OFF_TEXT);
            isFanOn = on;
        } catch (IOException e) {
            tempLabel.setText("Eroare trimitere comanda ventilator");
            e.printStackTrace();
        }
    }

    private void updateThermometer(float temperature) {
        updateTemperatureLabel(temperature);
        updateStatisticsAndChart(temperature);
        animateThermometerBar(temperature);
        updateAlertImage(temperature);
        updateScenes(temperature);
        updateAutoFan(temperature);
    }

    private void updateTemperatureLabel(float temperature) {
        tempLabel.setText(String.format("Temperatura: %.2f °C", temperature));
    }

    private void updateStatisticsAndChart(float temperature) {
        tempHistory.add((double) temperature);
        sumTemp += temperature;
        if (temperature < minTemp) {
            minTemp = temperature;
        }
        if (temperature > maxTemp) {
            maxTemp = temperature;
        }
        sampleIndex++;
        tempSeries.getData().add(new XYChart.Data<>(sampleIndex, temperature));
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

    private void updateAlertImage(float temperature) {
        alertImageView.setVisible(temperature >= 34);
    }

    private void updateScenes(float temperature) {
        updateTropicalScene(temperature);
        updateBeachScene(temperature);
        updateDesertScene(temperature);
    }

    private void updateTropicalScene(float temperature) {
        if (temperature >= 22 && temperature <= 28 && !backgroundImageView.isVisible()) {
            showWithBounce(backgroundImageView, "y", 500, -20, 0);
            showWithBounce(palmier1, "x", -500, -180, -200);
            showWithBounce(palmier2, "x", 500, 130, 150);
            showWithBounce(maimuta, "y", -500, -180, -300);
        }
        if ((temperature <= 18 || temperature >= 29) && backgroundImageView.isVisible()) {
            hideWithBounce(backgroundImageView, "y", 0, -20, 500);
            hideWithBounce(palmier1, "x", -200, -180, -500);
            hideWithBounce(palmier2, "x", 150, 130, 500);
            hideWithBounce(maimuta, "y", -200, -180, -500);
        }
    }

    private void updateBeachScene(float temperature) {
        if (temperature > 29 && temperature <= 31 && !backgroundImageView1.isVisible()) {
            showWithBounce(backgroundImageView1, "y", 500, -20, 0);
            showWithBounce(sezlong, "x", 500, 80, 100);
            showWithBounce(soare, "y", -500, -180, -350);
            showWithBounce(palmierPlaja, "x", -500, -130, -300);
        }
        if ((temperature <= 28 || temperature >= 32) && backgroundImageView1.isVisible()) {
            hideWithBounce(backgroundImageView1, "y", 0, -20, 500);
            hideWithBounce(sezlong, "x", 100, 80, 500);
            hideWithBounce(soare, "y", -200, -180, -500);
            hideWithBounce(palmierPlaja, "x", -150, -130, -500);
        }
    }

    private void updateDesertScene(float temperature) {
        if (temperature >= 32 && temperature <= 34 && !backgroundImageView2.isVisible()) {
            showWithBounce(backgroundImageView2, "y", 500, -20, 0);
            showWithBounce(cactus1, "x", 500, 80, 100);
            showWithBounce(cactus2, "x", -500, -100, -120);
            showWithBounce(camila, "y", 500, 80, 100);
        }
        if ((temperature <= 31 || temperature >= 40) && backgroundImageView2.isVisible()) {
            hideWithBounce(backgroundImageView2, "y", 0, -20, 500);
            hideWithBounce(cactus1, "x", 100, 80, 500);
            hideWithBounce(cactus2, "x", -120, -100, -500);
            hideWithBounce(camila, "y", 100, 80, 500);
        }
    }

    private void updateAutoFan(float temperature) {
        if (isManualMode || serialComm == null) {
            return;
        }

        double threshold = fanThresholdSlider.getValue();
        boolean shouldBeOn = temperature >= threshold;

        if (shouldBeOn == isFanOn) {
            return;
        }

        try {
            serialComm.writeByte((byte) (shouldBeOn ? '1' : '0'));
            isFanOn = shouldBeOn;

            fanToggle.setSelected(shouldBeOn);
            fanToggle.setText(shouldBeOn ? FAN_ON_TEXT : FAN_OFF_TEXT);
        } catch (IOException e) {
            tempLabel.setText("Eroare trimitere comanda ventilator (auto)");
            e.printStackTrace();
        }
    }

    /* ===================  PDF  =================== */

    private void generatePdfReport(Stage owner) {
        if (tempHistory.isEmpty()) {
            tempLabel.setText("Nu există date pentru raport.");
            return;
        }

        File file = chooseReportDestination(owner);
        if (file == null) {
            return;
        }

        try {
            File chartFile = createChartImageTempFile();
            writePdfReport(file, chartFile);
            chartFile.deleteOnExit();
            tempLabel.setText("Raport PDF generat.");
        } catch (Exception e) {
            e.printStackTrace();
            tempLabel.setText("Eroare la generarea PDF-ului.");
        }
    }

    private File chooseReportDestination(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvează raport PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(owner);
    }

    private File createChartImageTempFile() throws IOException {
        WritableImage fxImage = tempChart.snapshot(null, null);
        BufferedImage bImage = SwingFXUtils.fromFXImage(fxImage, null);
        File chartFile = File.createTempFile("chart_temp_", ".png");
        ImageIO.write(bImage, "png", chartFile);
        return chartFile;
    }

    private void writePdfReport(File file, File chartFile) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        document.add(new Paragraph("Raport temperatura - sesiune curenta"));
        document.add(new Paragraph(" "));

        addStatisticsParagraphs(document);
        addTableWithMeasurements(document);
        addChartImage(document, chartFile);

        document.close();
    }

    private void addStatisticsParagraphs(Document document) throws Exception {
        int count = tempHistory.size();
        double avg = sumTemp / count;

        document.add(new Paragraph(String.format("Numar masuratori: %d", count)));
        document.add(new Paragraph(String.format("Temperatura minima: %.2f °C", minTemp)));
        document.add(new Paragraph(String.format("Temperatura maxima: %.2f °C", maxTemp)));
        document.add(new Paragraph(String.format("Temperatura medie: %.2f °C", avg)));
        document.add(new Paragraph(" "));
    }

    private void addTableWithMeasurements(Document document) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.addCell("Index");
        table.addCell("Temperatura (°C)");

        for (int i = 0; i < tempHistory.size(); i++) {
            table.addCell(Integer.toString(i + 1));
            table.addCell(String.format("%.2f", tempHistory.get(i)));
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

    /* ===================  ANIMAȚII UTILITARE  =================== */

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

    public static void main(String[] args) {
        launch(args);
    }
}
