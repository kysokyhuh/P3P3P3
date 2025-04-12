// Producer.java
package org.example.p3p3p3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.File;

public class Producer extends Application {

    private TextField serverIPField;
    private TextField portField;
    private TextField folderField;
    private TextArea logArea;
    private Stage primaryStage; // For the FileChooser

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Input for Server IP
        serverIPField = new TextField();
        serverIPField.setPromptText("Server IP (e.g., 127.0.0.1)");

        // Input for Server Port
        portField = new TextField();
        portField.setPromptText("Server Port (e.g., 12345)");

        // Field to show selected folder path(s)
        // Updated prompt indicates that multiple folders can be entered (separated by commas)
        folderField = new TextField();
        folderField.setPromptText("Enter folder path(s), separated by commas");

        // Button to choose a folder (optional, if you want a directory chooser instead of text input)
        Button chooseFolderButton = new Button("Choose Folder");
        chooseFolderButton.setOnAction(e -> chooseFolder());

        // Button to start sending the files
        Button startButton = new Button("Start Producer");
        startButton.setOnAction(e -> startProducer());

        // TextArea to display log messages
        logArea = new TextArea();
        logArea.setEditable(false);

        VBox root = new VBox(10, serverIPField, portField, folderField, chooseFolderButton, startButton, logArea);
        root.setStyle("-fx-padding: 20;");
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("Producer GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Opens a DirectoryChooser dialog to select a folder and set it in the folderField.
    private void chooseFolder() {
        // DirectoryChooser allows selecting only one folder, so if you need multiple folders,
        // you could repeatedly use it and then append the chosen paths separated by commas.
        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Select Folder");
        File selectedFolder = directoryChooser.showDialog(primaryStage);
        if (selectedFolder != null) {
            // Append to the text field (if not empty, add a comma separator)
            String currentText = folderField.getText();
            if (currentText != null && !currentText.isEmpty()) {
                folderField.setText(currentText + ", " + selectedFolder.getAbsolutePath());
            } else {
                folderField.setText(selectedFolder.getAbsolutePath());
            }
            log("Folder selected: " + selectedFolder.getAbsolutePath());
        }
    }

    // Starts the producer logic which sends files from each specified folder to the Consumer.
    private void startProducer() {
        String serverIP = serverIPField.getText().trim();
        String portStr = portField.getText().trim();
        String foldersInput = folderField.getText().trim();

        if (serverIP.isEmpty() || portStr.isEmpty() || foldersInput.isEmpty()) {
            log("Please fill in all fields (server IP, port, and folder paths).");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            log("Invalid port number.");
            return;
        }

        log("Starting Producer with: " + serverIP + " " + port + " and folders: " + foldersInput);

        // Start the producer logic on a background thread.
        new Thread(() -> runProducerLogic(serverIP, port, foldersInput)).start();
    }

    /**
     * Modified runProducerLogic to support multiple folder paths separated by commas.
     * Each folder is processed by its own thread.
     */
    private void runProducerLogic(String serverIP, int port, String foldersInput) {
        // Expect foldersInput as a comma-separated list of folder paths.
        String[] folders = foldersInput.split(",");
        for (String folderPath : folders) {
            final String trimmedFolderPath = folderPath.trim();
            // Spawn a new thread for each folder.
            new Thread(() -> {
                File folder = new File(trimmedFolderPath);
                if (!folder.exists() || !folder.isDirectory()) {
                    log("Invalid folder: " + trimmedFolderPath);
                    return;
                }
                File[] files = folder.listFiles();
                if (files == null || files.length == 0) {
                    log("No files found in folder: " + trimmedFolderPath);
                    return;
                }
                for (File file : files) {
                    if (file.isFile()) {
                        log("Sending file: " + file.getAbsolutePath());
                        ProducerThread producerThread = new ProducerThread(file.getAbsolutePath(), serverIP, port);
                        new Thread(producerThread).start();
                    }
                }
            }).start();
        }
    }

    // Helper method to append log messages to the TextArea.
    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
