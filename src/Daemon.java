import java.io.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class Daemon {

    private static final int MAX_CLIENTS = 2;
    private static final int MAX_TRANSFER_RATE = 10240; // 10 KB/s
    private static int currentClients = 0;
    private static final ReentrantLock clientLock = new ReentrantLock();

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(3000)) {
            System.out.println("Server started on port 3000...");
            while (true) {
                Socket socket = serverSocket.accept();
                clientLock.lock();
                try {
                    if (currentClients >= MAX_CLIENTS) {
                        socket.close();
                    } else {
                        currentClients++;
                        new Thread(() -> handleClient(socket)).start();
                    }
                } finally {
                    clientLock.unlock();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            byte[] flag = new byte[1];
            in.read(flag);

            if (flag[0] == 0) {
                handleReceiveFile(socket);
            } else if (flag[0] == 1) {
                handleSendFile(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            decrementClients();
        }
    }

    private static void handleReceiveFile(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            byte[] filePathBytes = new byte[1024];
            int length = in.read(filePathBytes);

            if (length <= 0) {
                System.err.println("Failed to receive file path.");
                return;
            }

            String destinationPath = new String(filePathBytes, 0, length).trim();
            File partFile = new File(destinationPath + ".part");

            long offset = 0;
            if (partFile.exists()) {
                offset = partFile.length();
            }

            // Envia o offset atual
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeLong(offset);
            out.flush();

            // Recebe os dados do arquivo e continua a partir do offset
            try (FileOutputStream outFile = new FileOutputStream(partFile, true)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    outFile.write(buffer, 0, bytesRead);
                }
            }

            // Renomeia o arquivo .part para o nome final
            File destinationFile = new File(destinationPath);
            if (partFile.renameTo(destinationFile)) {
                System.out.println("File successfully received: " + destinationPath);
            } else {
                System.err.println("Failed to rename .part file to final destination.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleSendFile(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             OutputStream out = socket.getOutputStream()) {
    
            byte[] filePathBytes = new byte[1024];
            int length = in.read(filePathBytes);
    
            if (length <= 0) {
                System.err.println("Failed to receive file path from client.");
                return;
            }
    
            String filePath = new String(filePathBytes, 0, length).trim();
            File file = new File(filePath);
    
            if (!file.exists()) {
                System.err.println("File not found on server: " + filePath);
                return;
            }
    
            // Recebe o offset
            long offset = in.readLong();
    
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeLong(file.length());
            dataOut.flush();
    
            // Divide a taxa de transferencia pela quantidade de clientes ativos
            clientLock.lock();
            int effectiveRate = MAX_TRANSFER_RATE / Math.max(1, currentClients);
            clientLock.unlock();
    
            // Envia o conteudo do arquivo a partir do offset
            try (FileInputStream inFile = new FileInputStream(file)) {
                inFile.skip(offset);
    
                byte[] buffer = new byte[1024];
                int bytesRead;
                long startTime = System.currentTimeMillis();
                long bytesSentThisSecond = 0;
    
                while ((bytesRead = inFile.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                    bytesSentThisSecond += bytesRead;
    
                    // Controle da taxa de transferencia proporcional
                    if (bytesSentThisSecond >= effectiveRate) {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        if (elapsedTime < 1000) {
                            try {
                                Thread.sleep(1000 - elapsedTime);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        startTime = System.currentTimeMillis();
                        bytesSentThisSecond = 0;
                    }
                }
            }
    
            System.out.println("File sent successfully: " + filePath);
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    private static void decrementClients() {
        clientLock.lock();
        try {
            currentClients--;
        } finally {
            clientLock.unlock();
        }
    }
}