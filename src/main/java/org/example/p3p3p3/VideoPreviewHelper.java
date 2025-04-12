package org.example.p3p3p3;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;

public class VideoPreviewHelper {

    /**
     * Configures the given container to show a hover preview of the provided video.
     * On mouse enter, the preview media is played; on mouse exit, it is paused and hidden.
     *
     * @param container The container (such as a thumbnail StackPane) to attach the preview.
     * @param previewFilePath The absolute path to the preview video file
     * @return The MediaPlayer instance, if needed for further control.
     */
    public static MediaPlayer setupPreview(StackPane container, String previewFilePath) {
        File previewFile = new File(previewFilePath);
        if (!previewFile.exists()) {
            System.out.println("Preview file not found: " + previewFilePath);
            return null;
        }
        Media media = new Media(previewFile.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);

        // Set dimensions for the preview; adjust these values as needed.
        mediaView.setFitWidth(300);
        mediaView.setFitHeight(200);
        mediaView.setVisible(false);

        container.getChildren().add(mediaView);

        // When the mouse enters, display and start the preview
        container.setOnMouseEntered((MouseEvent e) -> {
            mediaView.setVisible(true);
            mediaPlayer.play();
        });
        // When the mouse exits, pause and hide the preview
        container.setOnMouseExited((MouseEvent e) -> {
            mediaPlayer.pause();
            mediaView.setVisible(false);
        });

        return mediaPlayer;
    }
}
