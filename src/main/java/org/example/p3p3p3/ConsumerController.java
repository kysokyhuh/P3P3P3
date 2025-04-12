package org.example.p3p3p3;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import java.io.File;

public class ConsumerController {

    @FXML
    private TextArea logArea;

    @FXML
    private TextField threadField;   // For consumer thread count

    @FXML
    private TextField queueField;    // For queue capacity

    @FXML
    private FlowPane videoThumbnailContainer;  // FlowPane to display video thumbnails

    @FXML
    protected void handleStartConsumer() {
        String threads = threadField.getText();
        String queueSize = queueField.getText();

        if (threads.isEmpty() || queueSize.isEmpty()) {
            logArea.appendText("Please enter both the number of consumer threads and the queue size.\n");
            return;
        }

        // Set system properties so that Consumer.java can pick them up
        System.setProperty("consumer.threads", threads);
        System.setProperty("consumer.queue", queueSize);

        // Start the Consumer server in a new thread
        new Thread(() -> {
            System.out.println("Starting Consumer with " + threads + " threads and queue size " + queueSize + "...");
            Consumer.main(new String[]{});  // Launch the Consumer server
        }).start();
    }

    // Call this method to update the video thumbnails in the FlowPane
    public void updateThumbnails() {
        File previewDir = new File("previews");
        File[] previewFiles = previewDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));

        if (previewFiles != null && previewFiles.length > 0) {
            for (File previewFile : previewFiles) {
                StackPane thumbnailPane = new StackPane();
                thumbnailPane.setStyle("-fx-border-color: black; -fx-padding: 5; -fx-background-color: lightgray;");
                Label label = new Label(previewFile.getName());
                thumbnailPane.getChildren().add(label);

                // On click, open the full video in the system's default player
                thumbnailPane.setOnMouseClicked((MouseEvent event) -> {
                    playFullVideo(previewFile);
                });

                videoThumbnailContainer.getChildren().add(thumbnailPane);
            }
        }
    }

    // Opens the full video in the default media player (QuickTime, VLC, etc.)
    private void playFullVideo(File videoFile) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                // macOS: open with the default application (QuickTime)
                String command = "open " + videoFile.getAbsolutePath();
                Runtime.getRuntime().exec(command);
            } else {
                // For other OS (Windows/Linux), use Desktop API to open the file
                java.awt.Desktop.getDesktop().open(videoFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
