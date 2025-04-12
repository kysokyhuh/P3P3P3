// ProducerController.java
package org.example.p3p3p3;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ProducerController {

    @FXML
    private TextField serverIPField;

    @FXML
    private TextField portField;

    // Update the label in the FXML or here to indicate multiple folder paths can be entered separated by commas.
    @FXML
    private TextField folderField;

    @FXML
    private TextArea logArea;

    @FXML
    protected void handleStartProducer() {
        String serverIP = serverIPField.getText();
        String port = portField.getText();
        String folders = folderField.getText();

        if (serverIP.isEmpty() || port.isEmpty() || folders.isEmpty()) {
            logArea.appendText("Please fill in all fields before starting Producer.\n");
            return;
        }

        new Thread(() -> {
            logArea.appendText("Starting Producer with args: " + serverIP + " " + port + " and folders: " + folders + "\n");
            // Launch the Producer application. The Producer will now interpret the folder field as a comma-separated list.
            Producer.main(new String[]{serverIP, port, folders});
        }).start();
    }
}
