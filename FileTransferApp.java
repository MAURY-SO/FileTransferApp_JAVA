import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Enumeration;

public class FileTransferApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("File Transfer App");
                frame.setSize(600, 400);
                frame.setLocation(750, 250);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                JTabbedPane tabbedPane = new JTabbedPane();

                // Add the server and client tabs
                tabbedPane.addTab("Servidor", new ServerPanel());
                tabbedPane.addTab("Cliente", new ClientPanel());

                frame.add(tabbedPane, BorderLayout.CENTER);
                frame.setVisible(true);
            }
        });
    }
}

// Panel del Servidor
class ServerPanel extends JPanel {
    private JTextArea textArea;
    private JButton startButton;
    private JButton stopServerButton;

    public ServerPanel() {
        setLayout(new BorderLayout());
        
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        stopServerButton = new JButton("Detener Servidor");
        stopServerButton.addActionListener(e -> stopServer());
        stopServerButton.setEnabled(false);
        buttonPanel.add(stopServerButton);

        startButton = new JButton("Iniciar Servidor");
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServerButton.setEnabled(true);
                textArea.setText("");
                startServer();
                
            }
        });
    }
    
    private void stopServer() {
    // Detener el servidor
    if (serverRunning) {
        serverRunning = false;
        try {
            serverSocket.close(); 
            textArea.append("Servidor cerrado correctamente.\n");
        } catch (IOException e) {
            e.printStackTrace();
            textArea.append("Error al cerrar el servidor: " + e.getMessage() + "\n");
        }
    }
    stopServerButton.setEnabled(false);
    }
    
    private String ipServer(){
    
         try {
            
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface interfaz = interfaces.nextElement();

      
                if (!interfaz.isUp() || interfaz.isLoopback()) {
                    continue;
                }

                
                Enumeration<InetAddress> direcciones = interfaz.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress direccion = direcciones.nextElement();
                    
                    if (direccion instanceof Inet4Address && !direccion.isLoopbackAddress()) {
                        return String.valueOf(direccion.getHostAddress());
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    
    }
    

private boolean serverRunning = false;
private ServerSocket serverSocket;

    private void startServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String ipS = ipServer();
                serverRunning = true; 

                try {
                    serverSocket = new ServerSocket(); 
                    serverSocket.bind(new InetSocketAddress(ipS, 0));
                    int puertoAsignado = serverSocket.getLocalPort();
                    textArea.append("Servidor escuchando en puerto " + puertoAsignado + "...\n con la ip: " + ipS + "\n");

                    while (serverRunning) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            textArea.append("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress() + "\n");
                            new ClientHandler(clientSocket).start();
                        } catch (SocketException se) {
                            if (!serverRunning) {
                                textArea.append("Servidor detenido.\n");
                                break;
                            }
                            throw se;  
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    textArea.append("Error en el servidor: " + e.getMessage() + "\n");
                }
            }
        }).start();
    }


    private class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream inputStream;
        private FileOutputStream fileOutputStream;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                String fileName = inputStream.readUTF();
                long fileSize = inputStream.readLong();

                textArea.append("Recibiendo archivo: " + fileName + " de tamaño: " + fileSize + " bytes\n");

                File file = new File("received_" + fileName);
                fileOutputStream = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytesRead += bytesRead;
                    fileOutputStream.write(buffer, 0, bytesRead);
                    double aux = (totalBytesRead/fileSize)*100;
                    textArea.append(aux+"% \n");
                    
                    if (totalBytesRead >= fileSize) {
                        break;
                    }
                }

                textArea.append("Archivo recibido exitosamente: " + fileName + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                textArea.append("Error al recibir archivo: " + e.getMessage() + "\n");
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
            }
        }
    }
}

class ClientPanel extends JPanel {
    private JTextArea textArea;
    private JButton selectButton;
    private JButton sendButton;
    private File selectedFile;

    public ClientPanel() {
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        selectButton = new JButton("Seleccionar archivo");
        sendButton = new JButton("Enviar archivo");
        sendButton.setEnabled(false); // Deshabilitar el botón hasta seleccionar un archivo

        JTextField ipTxT = new JTextField();
        ipTxT.setPreferredSize(new Dimension(100, 30));
        buttonPanel.add(ipTxT);
        
        JTextField portTxT = new JTextField();
        portTxT.setPreferredSize(new Dimension(100, 30));
        buttonPanel.add(portTxT);
        
        buttonPanel.add(selectButton);
        buttonPanel.add(sendButton);
        add(buttonPanel, BorderLayout.SOUTH);

        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(ClientPanel.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    textArea.append("Archivo seleccionado: " + selectedFile.getName() + "\n");
                    sendButton.setEnabled(true); // Habilitar el botón de enviar
                }
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    textArea.setText("");
                    try {
                        sendFile(Integer.valueOf(portTxT.getText()),ipTxT.getText(),selectedFile);
                    } catch (IOException ex) {
                        textArea.append("Error al enviar archivo: " + ex.getMessage() + "\n");
                    }
                }
            }
        });
    }

    private void sendFile(Integer port,String ip, File file) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket(ip, port);
                     FileInputStream fileInputStream = new FileInputStream(file);
                     DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())) {

                    // Enviar nombre del archivo y tamaño
                    outputStream.writeUTF(file.getName());
                    outputStream.writeLong(file.length());

                    textArea.append("Enviando archivo: " + file.getName() + "...\n");

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    textArea.append("Archivo enviado exitosamente: " + file.getName() + "\n");

                } catch (IOException e) {
                    textArea.append("Error al enviar archivo: " + e.getMessage() + "\n");
                }
            }
        }).start();
    }
    
    
}

