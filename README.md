

# File Transfer Project - Berkeley Sockets

This project implements a file transfer system using **Berkeley Sockets** in Java. It consists of a server (`Daemon`) and a client (`Remcp`) that enable reliable file sending and receiving, with support for transfer resumption.

## Requirements

- **Java 8 or higher**

## How to Compile
```bash
javac -d bin src/*.java
```

## How to Run

### Start the Server
```bash
java -cp bin Daemon
```

### Use the Client
- **Send a file to the server**:
  ```bash
  java -cp bin Remcp <local_file_path> <server_ip>:<server_file_path>
  ```
  Example:
  ```bash
  java -cp bin Remcp test/photo.jpeg localhost:/tmp/photo.jpeg
  ```

- **Receive a file from the server**:
  ```bash
  java -cp bin Remcp <server_ip>:<server_file_path> <local_file_path>
  ```
  Example:
  ```bash
  java -cp bin Remcp localhost:/tmp/photo.jpeg test/received_photo.jpeg
  ```

## Features

- **File Sending and Receiving**: Transfers files between the client and server.
- **Transfer Resumption**: Resumes interrupted transfers from where they left off.
- **Transfer Progress**: Displays progress while receiving files on the client.
