package com.example.kafkachat;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Interfaz gráfica para el cliente de chat basado en Kafka.
 */
public class ChatGUI extends JFrame implements ChatConsumer.MessageListener {
    
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField recipientField;
    private JButton sendButton;
    private JButton privateButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    
    private final String username;
    private final ChatProducer producer;
    private final ChatConsumer consumer;
    private final Thread consumerThread;
    
    /**
     * Constructor que inicializa la GUI y los componentes de Kafka.
     * 
     * @param bootstrapServers Dirección de los servidores Kafka
     * @param username Nombre de usuario del cliente
     */
    public ChatGUI(String bootstrapServers, String username) {
        this.username = username;
        
        // Inicializar componentes de Kafka
        this.producer = new ChatProducer(bootstrapServers, username);
        this.consumer = new ChatConsumer(bootstrapServers, username, this);
        this.consumerThread = new Thread(consumer);
        
        // Configurar la ventana
        setTitle("Kafka Chat - " + username);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Manejar cierre de ventana para limpiar recursos
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeChat();
            }
        });
        
        // Inicializar componentes de la GUI
        initComponents();
        
        // Iniciar el consumidor en un hilo separado
        consumerThread.start();
    }
    
    /**
     * Inicializa los componentes de la interfaz.
     */
    private void initComponents() {
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Área de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setWrapStyleWord(true);
        chatArea.setLineWrap(true);
        
        // Auto-scroll del área de chat
        DefaultCaret caret = (DefaultCaret)chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Panel de entrada de mensajes
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Panel de mensajes privados
        JPanel privatePanel = new JPanel(new BorderLayout(5, 0));
        
        recipientField = new JTextField();
        recipientField.setToolTipText("Destinatario para mensaje privado");
        
        privateButton = new JButton("Privado");
        privateButton.addActionListener(e -> sendPrivateMessage());
        
        privatePanel.add(new JLabel("Para: "), BorderLayout.WEST);
        privatePanel.add(recipientField, BorderLayout.CENTER);
        privatePanel.add(privateButton, BorderLayout.EAST);
        
        // Panel combinado para entrada de mensajes
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 5));
        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(privatePanel, BorderLayout.SOUTH);
        
        // Lista de usuarios
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userList.getSelectedValue() != null) {
                recipientField.setText(userList.getSelectedValue());
            }
        });
        
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Online"));
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        
        // Ensamblar panel principal
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(userScrollPane, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * Envía un mensaje al canal general.
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            producer.sendMessage(message);
            messageField.setText("");
            messageField.requestFocus();
        }
    }
    
    /**
     * Envía un mensaje privado a un usuario específico.
     */
    private void sendPrivateMessage() {
        String recipient = recipientField.getText().trim();
        String message = messageField.getText().trim();
        
        if (!recipient.isEmpty() && !message.isEmpty()) {
            producer.sendPrivateMessage(recipient, message);
            
            // También mostrar el mensaje en nuestra propia ventana
            String privateMsg = "[Privado para " + recipient + "]: " + message;
            chatArea.append(privateMsg + "\n");
            
            messageField.setText("");
            messageField.requestFocus();
        }
    }
    
    /**
     * Callback cuando se recibe un mensaje.
     */
    @Override
    public void onMessageReceived(ChatMessage message) {
        // Actualizar interfaz en el hilo de Swing
        SwingUtilities.invokeLater(() -> {
            // Agregar mensaje al área de chat
            chatArea.append(message.getFormattedMessage() + "\n");
            
            // Actualizar lista de usuarios si es un mensaje de estado
            if (ChatMessage.TYPE_JOIN.equals(message.getType())) {
                if (!userListModel.contains(message.getSender())) {
                    userListModel.addElement(message.getSender());
                }
            } else if (ChatMessage.TYPE_LEAVE.equals(message.getType())) {
                userListModel.removeElement(message.getSender());
            }
        });
    }
    
    /**
     * Cierra el chat y libera recursos.
     */
    private void closeChat() {
        // Detener primero el consumidor para evitar excepciones
        try {
            consumer.close();
            consumerThread.join(1000); // Esperar max 1 segundo a que termine
        } catch (Exception e) {
            System.err.println("Error al cerrar consumidor: " + e.getMessage());
        }
        
        // Cerrar productor
        producer.close();
        
        // Cerrar ventana
        dispose();
        System.exit(0);
    }
    
    /**
     * Método principal para iniciar la aplicación.
     */
    public static void main(String[] args) {
        // Usar Look and Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Mostrar diálogo de configuración
        SwingUtilities.invokeLater(() -> {
            showLoginDialog();
        });
    }
    
    /**
     * Muestra un diálogo para ingresar el nombre de usuario y servidor Kafka.
     */
    private static void showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        
        JTextField serverField = new JTextField("localhost:9092");
        JTextField usernameField = new JTextField(System.getProperty("user.name"));
        
        panel.add(new JLabel("Servidor Kafka (host:puerto):"));
        panel.add(serverField);
        panel.add(new JLabel("Nombre de usuario:"));
        panel.add(usernameField);
        
        int result = JOptionPane.showConfirmDialog(null, panel, 
                "Configuración del Chat", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String bootstrapServers = serverField.getText().trim();
            String username = usernameField.getText().trim();
            
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(null, "El nombre de usuario no puede estar vacío", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                showLoginDialog();
                return;
            }
            
            // Crear y mostrar la ventana principal
            ChatGUI chatGUI = new ChatGUI(bootstrapServers, username);
            chatGUI.setLocationRelativeTo(null);
            chatGUI.setVisible(true);
        } else {
            System.exit(0);
        }
    }
}