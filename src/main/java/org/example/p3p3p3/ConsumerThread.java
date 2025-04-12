package org.example.p3p3p3;

// Source code is decompiled from a .class file using FernFlower decompiler.
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class ConsumerThread implements Runnable {
    private Socket socket;

    public ConsumerThread(Socket var1) {
        this.socket = var1;
    }

    public void run() {
        try {
            DataInputStream var1 = new DataInputStream(this.socket.getInputStream());

            try {
                String var2 = var1.readUTF();
                long var3 = var1.readLong();
                System.out.println("Receiving file: " + var2 + " (" + var3 + " bytes)");
                File var5 = new File("uploaded_videos");
                if (!var5.exists()) {
                    var5.mkdirs();
                }

                File var6 = new File(var5, var2);
                FileOutputStream var7 = new FileOutputStream(var6);

                try {
                    byte[] var8 = new byte[4096];

                    int var9;
                    for(long var10 = var3; var10 > 0L && (var9 = var1.read(var8, 0, (int)Math.min((long)var8.length, var10))) != -1; var10 -= (long)var9) {
                        var7.write(var8, 0, var9);
                    }

                    System.out.println("File received and saved: " + var2);
                } catch (Throwable var26) {
                    try {
                        var7.close();
                    } catch (Throwable var25) {
                        var26.addSuppressed(var25);
                    }

                    throw var26;
                }

                var7.close();
                this.generatePreview(var6);
            } catch (Throwable var27) {
                try {
                    var1.close();
                } catch (Throwable var24) {
                    var27.addSuppressed(var24);
                }

                throw var27;
            }

            var1.close();
        } catch (IOException var28) {
            var28.printStackTrace();
        } finally {
            try {
                this.socket.close();
            } catch (IOException var23) {
                var23.printStackTrace();
            }

        }

    }

    private void generatePreview(File var1) {
        File var2 = new File("previews");
        if (!var2.exists()) {
            var2.mkdirs();
        }

        String var3 = "previews/" + var1.getName();
        ProcessBuilder var4 = new ProcessBuilder(new String[]{"ffmpeg", "-i", var1.getAbsolutePath(), "-ss", "00:00:00", "-t", "10", "-c", "copy", var3});

        try {
            Process var5 = var4.start();
            int var6 = var5.waitFor();
            if (var6 == 0) {
                System.out.println("Preview generated: " + var3);
            } else {
                System.out.println("FFmpeg failed to generate preview for " + var1.getName());
            }
        } catch (Exception var7) {
            PrintStream var10000 = System.err;
            String var10001 = var1.getName();
            var10000.println("Error generating preview for " + var10001 + ": " + var7.getMessage());
        }

    }
}
