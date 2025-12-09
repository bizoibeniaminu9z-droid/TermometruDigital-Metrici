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
    private List<Double> tempHistory = new ArrayList<>();
    private double minTemp = Double.POSITIVE_INFINITY;
    private double maxTemp = Double.NEGATIVE_INFINITY;
    private double sumTemp = 0.0;

    @Override
    public void start(Stage primaryStage) {
        StackPane thermometerStack = new StackPane();
        tempLabel = new Label("Temperatura: -- °C");
        tempLabel.setFont(new Font("Arial", 32));

        thermometerBar = new Rectangle(50, 0, Color.BLUE);
        thermometerBar.setArcWidth(20);
        thermometerBar.setArcHeight(20);

        Rectangle thermometerOutline = new Rectangle(50, 300);
        thermometerOutline.setFill(Color.TRANSPARENT);
        thermometerOutline.setStroke(Color.BLACK);
        thermometerOutline.setStrokeWidth(2);
        thermometerOutline.setArcWidth(20);
        thermometerOutline.setArcHeight(20);

        thermometerStack = new StackPane();
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

        HBox thermoRow = new HBox(5, thermometerStack);
        thermoRow.setAlignment(Pos.CENTER);
        thermoRow.setPadding(new Insets(0, 0, 0, 0));

        // Buton ventilator (manual ON/OFF)
        fanToggle = new ToggleButton(FAN_OFF_TEXT);
        fanToggle.setOnAction(e -> handleFanToggle());

        // Buton Mod manual / Mod automat
        modeToggle = new ToggleButton("Mod manual");
        modeToggle.setSelected(true); // pornim în mod manual
        modeToggle.setOnAction(e -> handleModeToggle());

        // Slider prag pentru mod automat (20°C – 40°C)
        fanThresholdSlider = new Slider(20, 40, 30);
        fanThresholdSlider.setShowTickLabels(true);
        fanThresholdSlider.setShowTickMarks(true);
        fanThresholdSlider.setMajorTickUnit(5);
        fanThresholdSlider.setMinorTickCount(4);
        fanThresholdSlider.setBlockIncrement(1);
        fanThresholdSlider.setDisable(true); // la început suntem în manual

        Label thresholdLabel = new Label("Prag ventilator: 30 °C");
        fanThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            thresholdLabel.setText(String.format("Prag ventilator: %.0f °C", newVal.doubleValue()));
        });

        // Rând pentru butoane (mod + ventilator)
        HBox buttonsRow = new HBox(10, modeToggle, fanToggle);
        buttonsRow.setAlignment(Pos.CENTER);

        // Box pentru modul automat (label + slider)
        VBox autoBox = new VBox(5, thresholdLabel, fanThresholdSlider);
        autoBox.setAlignment(Pos.CENTER);

        // Grafic temperatură (LineChart)
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

        // Buton PDF
        Button pdfButton = new Button("Generează raport PDF");
        pdfButton.setOnAction(e -> generatePdfReport(primaryStage));

        VBox tempBox = new VBox();
        tempBox.setPrefHeight(400);
        tempBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setVgrow(thermoRow, Priority.ALWAYS);
        tempBox.getChildren().addAll(
                thermoRow,
                tempLabel,
                buttonsRow,
                autoBox,
                pdfButton,
                tempChart
        );
        tempBox.setSpacing(10);
        tempBox.setPadding(new Insets(0, 0, 50, 0));

        // Imagini (păstrezi path-urile tale)
        alertImageView = new ImageView();
        alertImageView.setImage(new Image("file:C:/Users/benib/Desktop/elmo-burn.jpg"));
        alertImageView.setFitWidth(400);
        alertImageView.setFitHeight(500);
        alertImageView.setPreserveRatio(false);
        alertImageView.setVisible(false);

        backgroundImageView = new ImageView();
        backgroundImageView.setImage(new Image("file:C:/Users/benib/Desktop/tropical-ground.png"));
        backgroundImageView.setFitWidth(600);
        backgroundImageView.setPreserveRatio(true);
        backgroundImageView.setTranslateY(500);
        backgroundImageView.setVisible(false);

        palmier1 = new ImageView();
        palmier1.setImage(new Image("file:C:/Users/benib/Desktop/palmier1.png"));
        palmier1.setFitWidth(500);
        palmier1.setPreserveRatio(true);
        palmier1.setTranslateX(500);
        palmier1.setTranslateY(-50);
        palmier1.setVisible(false);

        palmier2 = new ImageView();
        palmier2.setImage(new Image("file:C:/Users/benib/Desktop/palmier2.png"));
        palmier2.setFitWidth(600);
        palmier2.setPreserveRatio(true);
        palmier2.setTranslateX(500);
        palmier2.setTranslateY(50);
        palmier2.setVisible(false);

        maimuta = new ImageView();
        maimuta.setImage(new Image("file:C:/Users/benib/Desktop/maimuta.png"));
        maimuta.setFitWidth(300);
        maimuta.setPreserveRatio(true);
        maimuta.setTranslateX(170);
        maimuta.setTranslateY(500);
        maimuta.setVisible(false);

        backgroundImageView1 = new ImageView();
        backgroundImageView1.setImage(new Image("file:C:/Users/benib/Desktop/beach-ground.png"));
        backgroundImageView1.setFitWidth(600);
        backgroundImageView1.setPreserveRatio(true);
        backgroundImageView1.setTranslateY(500);
        backgroundImageView1.setVisible(false);

        sezlong = new ImageView();
        sezlong.setImage(new Image("file:C:/Users/benib/Desktop/sezlong.png"));
        sezlong.setFitWidth(150);
        sezlong.setPreserveRatio(true);
        sezlong.setTranslateX(500);
        sezlong.setTranslateY(130);
        sezlong.setVisible(false);

        soare = new ImageView();
        soare.setImage(new Image("file:C:/Users/benib/Desktop/soare.png"));
        soare.setFitWidth(150);
        soare.setPreserveRatio(true);
        soare.setTranslateY(500);
        soare.setTranslateX(200);
        soare.setVisible(false);

        palmierPlaja = new ImageView();
        palmierPlaja.setImage(new Image("file:C:/Users/benib/Desktop/palmier1.png"));
        palmierPlaja.setFitWidth(700);
        palmierPlaja.setPreserveRatio(true);
        palmierPlaja.setTranslateX(500);
        palmierPlaja.setTranslateY(-50);
        palmierPlaja.setVisible(false);

        backgroundImageView2 = new ImageView();
        backgroundImageView2.setImage(new Image("file:C:/Users/benib/Desktop/desert-ground.png"));
        backgroundImageView2.setFitWidth(800);
        backgroundImageView2.setPreserveRatio(true);
        backgroundImageView2.setTranslateY(500);
        backgroundImageView2.setVisible(false);

        cactus1 = new ImageView();
        cactus1.setImage(new Image("file:C:/Users/benib/Desktop/cactus1.png"));
        cactus1.setFitWidth(250);
        cactus1.setPreserveRatio(true);
        cactus1.setTranslateX(500);
        cactus1.setTranslateY(100);
        cactus1.setVisible(false);

        cactus2 = new ImageView();
        cactus2.setImage(new Image("file:C:/Users/benib/Desktop/cactus1.png"));
        cactus2.setFitWidth(250);
        cactus2.setPreserveRatio(true);
        cactus2.setTranslateX(500);
        cactus2.setTranslateY(90);
        cactus2.setVisible(false);

        camila = new ImageView();
        camila.setImage(new Image("file:C:/Users/benib/Desktop/camila.png"));
        camila.setFitWidth(450);
        camila.setPreserveRatio(true);
        camila.setTranslateY(500);
        camila.setVisible(false);

        StackPane root = new StackPane(
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

        Scene scene = new Scene(root, 600, 820);
        primaryStage.setTitle("Termometru Digital");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(this::setupSerialCommunication).start();
    }

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
        // Dacă suntem în mod automat, ignorăm (oricum butonul e disabled)
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
        // Actualizăm label-ul
        tempLabel.setText(String.format("Temperatura: %.2f °C", temperature));

        // --- Istoric + statistici ---
        tempHistory.add((double) temperature);
        sumTemp += temperature;
        if (temperature < minTemp) minTemp = temperature;
        if (temperature > maxTemp) maxTemp = temperature;
        sampleIndex++;
        tempSeries.getData().add(new XYChart.Data<>(sampleIndex, temperature));

        // --- Animație termometru ---
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

        alertImageView.setVisible(temperature >= 34);

        // ----- LOGICA ANIMAȚIILOR CU FUNDALURI -----

        if ((temperature >= 22 && temperature <= 28) && !backgroundImageView.isVisible()) {
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

        if ((temperature > 29 && temperature <= 31) && !backgroundImageView1.isVisible()) {
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

        if ((temperature >= 32 && temperature <= 34) && !backgroundImageView2.isVisible()) {
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

        // ----- LOGICA PENTRU MOD AUTOMAT -----
        if (!isManualMode && serialComm != null) {
            double threshold = fanThresholdSlider.getValue();
            boolean shouldBeOn = temperature >= threshold;

            if (shouldBeOn != isFanOn) {
                try {
                    serialComm.writeByte((byte) (shouldBeOn ? '1' : '0'));
                    isFanOn = shouldBeOn;

                    // actualizăm și butonul ca să vezi starea reală
                    fanToggle.setSelected(shouldBeOn);
                    fanToggle.setText(shouldBeOn ? FAN_ON_TEXT : FAN_OFF_TEXT);
                } catch (IOException e) {
                    tempLabel.setText("Eroare trimitere comanda ventilator (auto)");
                    e.printStackTrace();
                }
            }
        }
    }

    private void generatePdfReport(Stage owner) {
        if (tempHistory.isEmpty()) {
            tempLabel.setText("Nu există date pentru raport.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvează raport PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try {
            // Snapshot la grafic
            WritableImage fxImage = tempChart.snapshot(null, null);
            BufferedImage bImage = SwingFXUtils.fromFXImage(fxImage, null);
            File chartFile = File.createTempFile("chart_temp_", ".png");
            ImageIO.write(bImage, "png", chartFile);

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("Raport temperatura - sesiune curenta"));
            document.add(new Paragraph(" "));

            int count = tempHistory.size();
            double avg = sumTemp / count;

            document.add(new Paragraph(String.format("Numar masuratori: %d", count)));
            document.add(new Paragraph(String.format("Temperatura minima: %.2f °C", minTemp)));
            document.add(new Paragraph(String.format("Temperatura maxima: %.2f °C", maxTemp)));
            document.add(new Paragraph(String.format("Temperatura medie: %.2f °C", avg)));
            document.add(new Paragraph(" "));

            // Tabel cu datele colectate
            PdfPTable table = new PdfPTable(2);
            table.addCell("Index");
            table.addCell("Temperatura (°C)");

            for (int i = 0; i < tempHistory.size(); i++) {
                table.addCell(Integer.toString(i + 1));
                table.addCell(String.format("%.2f", tempHistory.get(i)));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Imaginea graficului
            com.itextpdf.text.Image chartImg =
                    com.itextpdf.text.Image.getInstance(chartFile.getAbsolutePath());
            chartImg.scaleToFit(500, 400);
            document.add(chartImg);

            document.close();
            chartFile.deleteOnExit();

            tempLabel.setText("Raport PDF generat.");
        } catch (Exception e) {
            e.printStackTrace();
            tempLabel.setText("Eroare la generarea PDF-ului.");
        }
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
        } else {
            return node.translateYProperty();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
