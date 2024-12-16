import java.io.*;
import java.net.*;

public class Remcp {

    private static final String PORT = "3000";
    private static final int MAX_ATTEMPTS = 5; // Número máximo de tentativas
    private static final int RETRY_DELAY = 5000; // Intervalo entre tentativas (em ms)

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Remcp <sourcePath> <destinationPath>");
            System.exit(1);
        }

        String sourcePath = args[0];
        String destinationPath = args[1];
        String ip = "";

        // Determina envio ou recepção
        boolean isReceiving;
        if (sourcePath.contains(":")) {
            isReceiving = true; // Receber
            String[] sourceParts = sourcePath.split(":");
            if (sourceParts.length == 2) {
                ip = sourceParts[0];
                sourcePath = sourceParts[1];
            } else {
                System.out.println("Invalid source format. Expected format: ip:/path");
                System.exit(1);
            }
        } else if (destinationPath.contains(":")) {
            isReceiving = false; // Enviar
            String[] destinationParts = destinationPath.split(":");
            if (destinationParts.length == 2) {
                ip = destinationParts[0];
                destinationPath = destinationParts[1];
            } else {
                System.out.println("Invalid destination format. Expected format: ip:/path");
                System.exit(1);
            }
        } else {
            System.out.println("Invalid arguments. Ensure at least one path is remote.");
            System.exit(1);
            return;
        }

        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_ATTEMPTS && !success) {
            attempt++;
            try (Socket connection = new Socket(ip, Integer.parseInt(PORT))) {
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());

                if (isReceiving) {
                    // Receber
                    out.write(1);
                    out.flush();
                    out.write(sourcePath.getBytes());
                    out.flush();

                    success = receiveFile(connection, destinationPath);
                    if (success) {
                        System.out.println("File received successfully!");
                    } else {
                        System.out.println("Failed to receive file. Retrying...");
                    }
                } else {
                    // Enviar
                    out.write(0);
                    out.flush();

                    success = sendFile(connection, sourcePath, destinationPath);
                    if (success) {
                        System.out.println("File sent successfully!");
                    } else {
                        System.out.println("Failed to send file. Retrying...");
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection failed. Attempt " + attempt + " of " + MAX_ATTEMPTS + ".");
            }

            if (!success && attempt < MAX_ATTEMPTS) {
                try {
                    System.out.println("Retrying in " + (RETRY_DELAY / 1000) + " seconds...");
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!success) {
            System.out.println("Operation failed after " + MAX_ATTEMPTS + " attempts.");
        }
    }

    private static boolean sendFile(Socket socket, String sourcePath, String destinationPath) {
        File file = new File(sourcePath);
        File partFile = new File(sourcePath + ".part");

        if (!file.exists()) {
            System.err.println("Source file not found: " + sourcePath);
            return false;
        }

        long offset = 0;
        if (partFile.exists()) {
            offset = partFile.length();
        }

        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             FileInputStream fileIn = new FileInputStream(file)) {

            // Envia o caminho do arquivo e o offset
            out.write(destinationPath.getBytes());
            out.writeLong(offset);
            out.flush();

            // Avança ate o offset
            if (offset > 0) {
                fileIn.skip(offset);
            }

            // Envia o conteudo do arquivo
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }

            System.out.println("File sent successfully to destination: " + destinationPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean receiveFile(Socket socket, String destinationPath) {
        File partFile = new File(destinationPath + ".part");
        long offset = 0;
    
        if (partFile.exists()) {
            offset = partFile.length();
        }
    
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
    
            // Envia o offset atual
            out.writeLong(offset);
            out.flush();
    
            long fileSize = in.readLong();
            System.out.println("File size: " + fileSize + " bytes");
    
            // Recebe o conteudo do arquivo e progresso
            try (FileOutputStream outFile = new FileOutputStream(partFile, true)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesReceived = offset;
    
                while ((bytesRead = in.read(buffer)) != -1) {
                    outFile.write(buffer, 0, bytesRead);
                    totalBytesReceived += bytesRead;
    
                    // Progresso
                    int progress = (int) ((totalBytesReceived * 100) / fileSize);
                    System.out.print("\rProgress: " + progress + "%");
                }
                System.out.println();
            }
    
            // Renomeia o arquivo .part para o nome final
            File finalFile = new File(destinationPath);
            if (partFile.renameTo(finalFile)) {
                System.out.println("File received successfully: " + destinationPath);
                return true;
            } else {
                System.err.println("Failed to rename .part file.");
                return false;
            }
    
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }    
}