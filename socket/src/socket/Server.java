package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
	private static final int PORT = 1111;
    private static final String SAVE_DIR = "uploads/";
    private static Map<String, Socket> clients = new HashMap<>();

    public static void main(String[] args) {
        File directory = new File(SAVE_DIR);
        if (!directory.exists()) directory.mkdir();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur en attente de connexions...");

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientEmail;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                clientEmail = dis.readUTF();
                System.out.println(clientEmail + " s'est connecté.");
                clients.put(clientEmail, socket);

                while (true) {
                    String recipientEmail = dis.readUTF();
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    File file = new File(SAVE_DIR + fileName);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalRead = 0;

                        while ((bytesRead = dis.read(buffer)) > 0) {
                            fos.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                            if (totalRead >= fileSize) break;
                        }
                    }

                    System.out.println("Fichier reçu de " + clientEmail + " : " + fileName);

                    if (clients.containsKey(recipientEmail)) {
                        sendFileToClient(recipientEmail, fileName, file);
                    } else {
                        System.out.println("Destinataire non connecté.");
                    }
                }
            } catch (IOException e) {
                clients.remove(clientEmail);
                System.out.println(clientEmail + " s'est déconnecté.");
            }
        }

        private void sendFileToClient(String recipientEmail, String fileName, File file) {
            try {
                Socket recipientSocket = clients.get(recipientEmail);
                DataOutputStream dos = new DataOutputStream(recipientSocket.getOutputStream());

                dos.writeUTF(fileName);
                dos.writeLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Fichier envoyé à " + recipientEmail + " : " + fileName);
            } catch (IOException e) {
                System.out.println("Erreur d'envoi du fichier à " + recipientEmail);
            }
        }
    }
}
