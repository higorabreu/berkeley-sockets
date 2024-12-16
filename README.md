# Projeto de Transferência de Arquivos - Berkeley Sockets

Este projeto implementa um sistema de transferência de arquivos utilizando **Berkeley Sockets** em Java. Ele é composto por um servidor (`Daemon`) e um cliente (`Remcp`) que permitem envio e recebimento de arquivos de forma confiável, com suporte a retomada de transferências.

## Requisitos

- **Java 8 ou superior**

## Como Compilar
```bash
javac -d bin src/*.java
```

## Como Executar

### Iniciar o Servidor
```bash
java -cp bin Daemon
```

### Usar o Cliente
- **Enviar um arquivo para o servidor**:
  ```bash
  java -cp bin Remcp <local_file_path> <server_ip>:<server_file_path>
  ```
  Exemplo:
  ```bash
  java -cp bin Remcp test/foto.jpeg localhost:/tmp/foto.jpeg
  ```

- **Receber um arquivo do servidor**:
  ```bash
  java -cp bin Remcp <server_ip>:<server_file_path> <local_file_path>
  ```
  Exemplo:
  ```bash
  java -cp bin Remcp localhost:/tmp/foto.jpeg test/received_foto.jpeg
  ```

## Funcionalidades

- **Envio e Recebimento de Arquivos**: Envia e recebe arquivos entre cliente e servidor.
- **Retomada de Transferências**: Continua transferências interrompidas do ponto em que pararam.
- **Progresso da Transferência**: Mostra o progresso durante o recebimento de arquivos no cliente.
