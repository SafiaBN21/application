package socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;


public class client {
	private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 1111;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Entrez votre  email : ");
            String clientEmail = scanner.nextLine();
            dos.writeUTF(clientEmail);

            new Thread(() -> {
                try {
                    while (true) {
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();

                        File file = new File("downloads/" + fileName);
                        file.getParentFile().mkdirs();

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
                        System.out.println("Fichier reçu : " + fileName);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (true) {
                System.out.print("Entrez l'email du destinataire : ");
                String recipientEmail = scanner.nextLine();
                dos.writeUTF(recipientEmail);

                System.out.print("Entrez le chemin du fichier à envoyer : ");
                String filePath = scanner.nextLine();
                File file = new File(filePath);

                dos.writeUTF(file.getName());
                dos.writeLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Fichier envoyé avec succès !" + recipientEmail);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
