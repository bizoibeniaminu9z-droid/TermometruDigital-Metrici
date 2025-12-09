package termometrudigital;

import javafx.scene.chart.XYChart;
import java.util.ArrayList;
import java.util.List;

public class TemperatureStats {

    private final XYChart.Series<Number, Number> series;
    private final List<Double> history = new ArrayList<>();
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    private double sum;
    private int sampleIndex;

    public TemperatureStats(XYChart.Series<Number, Number> series) {
        this.series = series;
    }

    public void addSample(double temperature) {
        history.add(temperature);
        sum += temperature;
        min = Math.min(min, temperature);
        max = Math.max(max, temperature);
        sampleIndex++;
        series.getData().add(new XYChart.Data<>(sampleIndex, temperature));
    }

    public List<Double> getHistory() {
        return history;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getAverage() {
        return history.isEmpty() ? 0.0 : sum / history.size();
    }
}
