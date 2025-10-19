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
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Node;

public class MainApp extends Application {

    private SerialCommunication serialComm;
    private Label tempLabel;
    private Rectangle thermometerBar;
    private ImageView alertImageView;
    private StackPane thermometerStack;
    private ImageView backgroundImageView;
    private ImageView palmier1;
    private ImageView palmier2;
    private ImageView maimuta;
    private ImageView backgroundImageView1;
    private ImageView sezlong;
    private ImageView soare;
    private ImageView palmier_plaja;
    private ImageView backgroundImageView2;
    private ImageView cactus1;
    private ImageView cactus2;
    private ImageView camila;

    @Override
    public void start(Stage primaryStage) {
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

        thermometerStack.setBackground(new Background(new BackgroundFill(Color.web("#f0f0f0", 0.2), new CornerRadii(15), Insets.EMPTY)));
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

        VBox tempBox = new VBox();
        tempBox.setPrefHeight(400);
        tempBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setVgrow(thermoRow, Priority.ALWAYS);
        tempBox.getChildren().addAll(thermoRow, tempLabel);
        tempBox.setPadding(new Insets(0, 0, 50, 0));

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
        palmier1.setFitWidth(600);
        palmier1.setPreserveRatio(true);
        palmier1.setTranslateX(500);
        palmier1.setVisible(false);
        
        palmier2 = new ImageView();
        palmier2.setImage(new Image("file:C:/Users/benib/Desktop/palmier2.png"));
        palmier2.setFitWidth(600);
        palmier2.setPreserveRatio(true);
        palmier2.setTranslateX(500);
        palmier2.setVisible(false);
        
        maimuta = new ImageView();
        maimuta.setImage(new Image("file:C:/Users/benib/Desktop/maimuta.png"));
        maimuta.setFitWidth(250);
        maimuta.setPreserveRatio(true);
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
        soare.setVisible(false);
        
        palmier_plaja = new ImageView();
        palmier_plaja.setImage(new Image("file:C:/Users/benib/Desktop/palmier1.png"));
        palmier_plaja.setFitWidth(500);
        palmier_plaja.setPreserveRatio(true);
        palmier_plaja.setTranslateX(500);
        palmier_plaja.setVisible(false);
        
        backgroundImageView2 = new ImageView();
        backgroundImageView2.setImage(new Image("file:C:/Users/benib/Desktop/desert-ground.png"));
        backgroundImageView2.setFitWidth(800);
        backgroundImageView2.setPreserveRatio(true);
        backgroundImageView2.setTranslateY(500);
        backgroundImageView2.setVisible(false);
        
        cactus1 = new ImageView();
        cactus1.setImage(new Image("file:C:/Users/benib/Desktop/cactus1.png"));
        cactus1.setFitWidth(150);
        cactus1.setPreserveRatio(true);
        cactus1.setTranslateX(500);
        cactus1.setTranslateY(100);
        cactus1.setVisible(false);
        
        cactus2 = new ImageView();
        cactus2.setImage(new Image("file:C:/Users/benib/Desktop/cactus1.png"));
        cactus2.setFitWidth(150);
        cactus2.setPreserveRatio(true);
        cactus2.setTranslateX(500);
        cactus2.setTranslateY(90);
        cactus2.setVisible(false);
        
        camila = new ImageView();
        camila.setImage(new Image("file:C:/Users/benib/Desktop/camila.png"));
        camila.setFitWidth(300);
        camila.setPreserveRatio(true);
        camila.setTranslateY(500);
        camila.setVisible(false);
        
        

        StackPane root = new StackPane(backgroundImageView,backgroundImageView1,backgroundImageView2,cactus1,cactus2,camila,palmier1,palmier2,palmier_plaja,soare,maimuta,sezlong, tempBox, alertImageView);

        Scene scene = new Scene(root, 400, 500);
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

    private void updateThermometer(float temperature) {
        tempLabel.setText(String.format("Temperatura: %.2f °C", temperature));

        double maxTemp = 100.0;
        double maxHeight = 300.0;
        double targetHeight = Math.min((temperature / maxTemp) * maxHeight, maxHeight);

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

        if ((temperature >= 28 && temperature <= 31) && !backgroundImageView.isVisible()) {
            showWithBounce(backgroundImageView, "y", 500, -20, 0);
            showWithBounce(palmier1, "x", -500, -180, -200);
            showWithBounce(palmier2, "x", 500, 130, 150);
            showWithBounce(maimuta, "y", -500, -180, -200);
        }

        if ((temperature <= 28 || temperature >= 31) && backgroundImageView.isVisible()) {
            hideWithBounce(backgroundImageView, "y", 0, -20, 500);
            hideWithBounce(palmier1, "x", -200, -180, -500);
            hideWithBounce(palmier2, "x", 150, 130, 500);
            hideWithBounce(maimuta, "y", -200, -180, -500);
        }
        
        if ((temperature >= 31 && temperature <= 32) && !backgroundImageView1.isVisible()) {
            showWithBounce(backgroundImageView1, "y", 500, -20, 0);
            showWithBounce(sezlong, "x", 500, 80, 100);
            showWithBounce(soare, "y", -500, -180, -200);
            showWithBounce(palmier_plaja, "x", -500, -130, -150);
        }

        if ((temperature <= 31 || temperature >= 32) && backgroundImageView1.isVisible()) {
            hideWithBounce(backgroundImageView1, "y", 0, -20, 500);
            hideWithBounce(sezlong, "x", 100, 80, 500);
            hideWithBounce(soare, "y", -200, -180, -500);
            hideWithBounce(palmier_plaja, "x", -150, -130, -500);
        }
        
        if ((temperature >= 32 && temperature <= 34) && !backgroundImageView2.isVisible()) {
            showWithBounce(backgroundImageView2, "y", 500, -20, 0);
            showWithBounce(cactus1, "x", 500, 80, 100);
            showWithBounce(cactus2, "x", -500, -100, -120);
            showWithBounce(camila, "y", 500, 80, 100);
        }

        if ((temperature <= 32 || temperature >= 34) && backgroundImageView2.isVisible()) {
            hideWithBounce(backgroundImageView2, "y", 0, -20, 500);
            hideWithBounce(cactus1, "x", 100, 80, 500);
            hideWithBounce(cactus2, "x", -120, -100, -500);
            hideWithBounce(camila, "y", 100, 80, 500);
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
