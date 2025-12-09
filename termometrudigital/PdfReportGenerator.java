package termometrudigital;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfReportGenerator {

    private final TemperatureStats stats;
    private final LineChart<Number, Number> chart;
    private final Label statusLabel;

    public PdfReportGenerator(TemperatureStats stats,
                              LineChart<Number, Number> chart,
                              Label statusLabel) {
        this.stats = stats;
        this.chart = chart;
        this.statusLabel = statusLabel;
    }

    public void generate(Stage owner) {
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
