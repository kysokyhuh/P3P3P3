// Updated ProducerThread.java to support both files and folders and read backchannel responses
package org.example.p3p3p3;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ProducerThread implements Runnable {
    private String path;
    private String serverIP;
    private int serverPort;

    public ProducerThread(String path, String serverIP, int serverPort) {
        this.path = path;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void run() {
        File target = new File(this.path);

        if (!target.exists()) {
            System.out.println("Path does not exist: " + this.path);
            return;
        }

        if (target.isFile()) {
            sendFile(target);
            return;
        }

        if (!target.isDirectory()) {
            System.out.println("Path is not a directory: " + this.path);
            return;
        }

        File[] files = target.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files found in folder: " + this.path);
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                sendFile(file);
            }
        }
    }

    private void sendFile(File file) {
        try (Socket socket = new Socket(this.serverIP, this.serverPort);
             FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             OutputStream os = socket.getOutputStream();
             DataOutputStream dos = new DataOutputStream(os)) {

            // Send file name and size to Consumer
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            dos.flush();

            // Read back the response from Consumer
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String response = dis.readUTF();
            System.out.println("Response from Consumer: " + response);

        } catch (IOException e) {
            System.err.println("Error sending file " + file.getName() + ": " + e.getMessage());
        }
    }
}
