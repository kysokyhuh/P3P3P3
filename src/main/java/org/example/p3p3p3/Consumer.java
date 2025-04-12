package org.example.p3p3p3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Consumer extends Application {

    public static final int PORT = 12345;
    public static final int MAX_QUEUE_SIZE = Integer.parseInt(System.getProperty("consumer.queue", "5"));
    public static final int NUM_CONSUMER_THREADS = Integer.parseInt(System.getProperty("consumer.threads", "3"));

    public static final BlockingQueue<File> fileQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    private TextArea logArea;
    private FlowPane videoThumbnailContainer;

    @Override
    public void start(Stage primaryStage) {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);

        videoThumbnailContainer = new FlowPane();
        videoThumbnailContainer.setHgap(10);
        videoThumbnailContainer.setVgap(10);

        VBox root = new VBox(10, logArea, videoThumbnailContainer);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Consumer Server and Video Preview");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start server to receive files from the Producer
        new Thread(this::startConsumerServer).start();

        // Start worker threads to process files from the queue
        for (int i = 0; i < NUM_CONSUMER_THREADS; i++) {
            new Thread(new FileSaver()).start();
        }
    }

    private void startConsumerServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("Consumer server started on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                log("Accepted connection from: " + socket.getInetAddress());
                new Thread(new ConsumerThread(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log("Error starting Consumer server: " + e.getMessage());
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    // ConsumerThread handles an incoming file upload from Producer.
    public class ConsumerThread implements Runnable {
        private Socket socket;

        public ConsumerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                // Read file metadata (file name and size)
                String fileName = in.readUTF();
                long fileSize = in.readLong();
                log("Receiving file: " + fileName + " (" + fileSize + " bytes)");

                // Save the incoming file to a temporary file
                File tempFile = new File("temp_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    long remaining = fileSize;
                    int read;
                    while (remaining > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                }

                // Enqueue the file for further processing
                if (!fileQueue.offer(tempFile)) {
                    out.writeUTF("QUEUE_FULL");
                    log("Queue full. Dropping file: " + fileName);
                    tempFile.delete();
                } else {
                    out.writeUTF("RECEIVED");
                    log("Enqueued file: " + fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
                log("Error in ConsumerThread: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // FileSaver processes files from the queue: saves, generates previews, and refreshes the UI.
    public class FileSaver implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    File file = fileQueue.take();

                    // Save the file to "uploaded_videos" folder
                    File outDir = new File("uploaded_videos");
                    if (!outDir.exists()) outDir.mkdirs();
                    File savedFile = new File(outDir, file.getName());
                    Files.copy(file.toPath(), savedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log("Saved file: " + savedFile.getName());

                    // Generate a 10-second preview using FFmpeg
                    generatePreview(savedFile);

                    // Refresh the thumbnails UI
                    refreshVideoThumbnails();

                    // Clean up the temporary file
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                    log("Error in FileSaver: " + e.getMessage());
                }
            }
        }

        // Calls FFmpeg to create a 10-second preview from the video file
        private void generatePreview(File inputFile) {
            File previewDir = new File("previews");
            if (!previewDir.exists()) previewDir.mkdirs();
            File previewFile = new File(previewDir, inputFile.getName());

            ProcessBuilder builder = new ProcessBuilder("ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                    "-ss", "00:00:00", "-t", "10", "-c:v", "libx264", "-c:a", "aac", previewFile.getAbsolutePath());
            try {
                Process process = builder.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    log("Preview generated: " + previewFile.getName());
                } else {
                    log("Failed to generate preview for: " + inputFile.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                log("Error generating preview: " + e.getMessage());
            }
        }
    }

    // Refreshes the video thumbnails from the "previews" folder
    private void refreshVideoThumbnails() {
        Platform.runLater(() -> {
            videoThumbnailContainer.getChildren().clear();
            File previewDir = new File("previews");
            log("Refreshing previews from: " + previewDir.getAbsolutePath());
            if (!previewDir.exists() || !previewDir.isDirectory()) {
                log("Preview directory not found.");
                return;
            }

            File[] previewFiles = previewDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov");
            });

            if (previewFiles == null || previewFiles.length == 0) {
                log("No preview files found.");
                return;
            }

            for (File previewFile : previewFiles) {
                StackPane thumbnailPane = new StackPane();
                thumbnailPane.setStyle("-fx-border-color: black; -fx-padding: 5; -fx-background-color: lightgray;");
                Label label = new Label(previewFile.getName());
                thumbnailPane.getChildren().add(label);

                // Set up hover preview using VideoPreviewHelper
                VideoPreviewHelper.setupPreview(thumbnailPane, previewFile.getAbsolutePath());

                // On click, open the full video in the system's default player
                thumbnailPane.setOnMouseClicked(e -> playFullVideo(previewFile));
                videoThumbnailContainer.getChildren().add(thumbnailPane);
            }
        });
    }

    // Open the full video in the default media player (QuickTime, VLC, etc.)
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
        } catch (IOException e) {
            log("Error opening video file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
