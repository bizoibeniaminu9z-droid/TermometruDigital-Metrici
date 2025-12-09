package termometrudigital;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SceneManager {

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

    public SceneManager() {
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

    public StackPane createRoot(VBox tempBox) {
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
                tempBox
        );
    }

    public void updateScenes(float temperature) {
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

    /* ===== helpers ===== */

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

    private DoubleProperty getTranslateProperty(Node node, String axis) {
        if (axis.equalsIgnoreCase("x")) {
            return node.translateXProperty();
        }
        return node.translateYProperty();
    }
}
