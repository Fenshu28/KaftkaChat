package com.example.kafkachat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import javax.swing.JFileChooser;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Interfaz gráfica para el cliente de chat basado en Kafka.
 */
public class ChatGUI extends JFrame implements ChatConsumer.MessageListener {

    private JTextArea chatArea;
    private JTextPane chatPane; // Reemplazamos JTextArea con JTextPane para mejor soporte de imágenes
    private JTextField messageField;
    private JTextField recipientField;
    private JButton sendButton;
    private JButton emojiButton;
    private JPopupMenu emojiPopup;
    private JButton privateButton;
    private JButton imageButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    private final String username;
    private final ChatProducer producer;
    private final ChatConsumer consumer;
    private final Thread consumerThread;
    private final Gson gson = new Gson();

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

        // Área de chat con JTextPane en lugar de JTextArea
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Configurar para mostrar contenido HTML
        chatPane.setContentType("text/html");

        // CSS inicial para el área de chat
        String initialHTML = "<html><head><style type='text/css'>"
                + "body { font-family: 'Segoe UI', sans-serif; font-size: 14pt; }"
                + "img { max-width: 280px; max-height: 280px; }"
                + ".timestamp { color: #666; }"
                + ".sender { font-weight: bold; color: #0066cc; }"
                + ".private { color: #990000; font-style: italic; }"
                + ".status { color: #006600; font-style: italic; }"
                + "</style></head><body></body></html>";
        chatPane.setText(initialHTML);

        JScrollPane chatScrollPane = new JScrollPane(chatPane);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Panel de entrada de mensajes
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));

        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Panel para botones (emojis, imágenes y enviar)
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));

        // Botón de emoji
        emojiButton = new JButton("😀");
        emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        emojiButton.setToolTipText("Insertar emoji");
        emojiButton.setFocusPainted(false);
        emojiButton.addActionListener(e -> showEmojiPopup());
        buttonPanel.add(emojiButton);

        // Botón de imagen
        imageButton = new JButton("🖼️");
        imageButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        imageButton.setToolTipText("Enviar imagen");
        imageButton.setFocusPainted(false);
        imageButton.addActionListener(e -> selectAndSendImage());
        buttonPanel.add(imageButton);

        // Botón enviar
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

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

        // Crear el popup de emojis
        emojiPopup = new JPopupMenu();
        EmojiSelector emojiSelector = new EmojiSelector(e -> {
            String emoji = e.getActionCommand();
            insertEmoji(emoji);
            emojiPopup.setVisible(false);
        });
        emojiPopup.add(emojiSelector);
    }

    /**
     * Muestra el popup selector de emojis.
     */
    private void showEmojiPopup() {
        emojiPopup.show(emojiButton, 0, -emojiPopup.getPreferredSize().height);
    }

    /**
     * Inserta un emoji en el campo de texto del mensaje.
     *
     * @param emoji El emoji a insertar
     */
    private void insertEmoji(String emoji) {
        messageField.replaceSelection(emoji);
        messageField.requestFocusInWindow();
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
     * Muestra un diálogo para seleccionar una imagen y la envía
     */
    private void selectAndSendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes", "jpg", "jpeg", "png", "gif", "bmp"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                // Convertir imagen a Base64
                ImageUtils.ImageData imageData = ImageUtils.imageToBase64(selectedFile);

                // Verificar si es un mensaje privado o general
                String recipient = recipientField.getText().trim();
                if (!recipient.isEmpty()) {
                    // Mensaje privado con imagen
                    producer.sendPrivateImage(recipient, imageData.getBase64(), imageData.getFormat());

                    // Mostrar en nuestra propia ventana
                    String privateImageMsg = String.format(
                            "<div><span class='timestamp'>[%tT]</span> <span class='private'>[Imagen privada para %s]</span></div>",
                            new Date(), recipient);
                    appendToChat(privateImageMsg);

                    // Mostrar la imagen
                    displayImage(imageData.getBase64(), imageData.getFormat());
                } else {
                    // Mensaje general con imagen
                    producer.sendImage(imageData.getBase64(), imageData.getFormat());
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al procesar la imagen: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Añade contenido HTML al área de chat
     *
     * @param htmlContent Contenido HTML a añadir
     */
    private void appendToChat(String htmlContent) {
        // Para el JTextArea original
        if (chatPane == null && chatArea != null) {
            chatArea.append(htmlContent.replaceAll("<[^>]*>", "") + "\n");
            return;
        }

        // Para el JTextPane con soporte HTML
        try {
            // Obtener el documento actual
            javax.swing.text.Document doc = chatPane.getDocument();

            // Obtener el editor del kit para manipular el documento HTML
            javax.swing.text.html.HTMLEditorKit editorKit = (javax.swing.text.html.HTMLEditorKit) chatPane.getEditorKit();
            javax.swing.text.html.HTMLDocument htmlDoc = (javax.swing.text.html.HTMLDocument) doc;

            // Insertar el HTML al final del documento
            editorKit.insertHTML(htmlDoc, htmlDoc.getLength(), htmlContent, 0, 0, null);

            // Hacer scroll al final
            chatPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            System.err.println("Error al añadir contenido al chat: " + e.getMessage());

            // Fallback a texto plano si falla el HTML
            if (chatPane != null) {
                chatPane.setText(chatPane.getText() + htmlContent.replaceAll("<[^>]*>", "") + "\n");
            }
        }
    }

    /**
     * Muestra la imagen en el área de chat
     *
     * @param base64Image Imagen codificada en Base64
     * @param format Formato de la imagen
     */
    private void displayImage(String base64Image, String format) {
        try {
            // Para chatPane (JTextPane con soporte HTML)
            if (chatPane != null) {
                // Crear el HTML para mostrar la imagen
                String imgHtml = String.format(
                        "<div style='margin: 5px 0'><img src='data:image/%s;base64,%s' style='max-width: 280px; max-height: 280px;'/></div>",
                        format, base64Image);

                // Añadir al chat
                appendToChat(imgHtml);
            } else if (chatArea != null) {
                // Versión alternativa para chatArea (JTextArea sin soporte HTML)
                // Descodificar la imagen y mostrarla como un JLabel bajo el área de texto
                JLabel imageLabel = ImageUtils.base64ToImageLabel(base64Image, format);

                // Añadir la imagen como un componente separado bajo el área de chat
                // (Esto requeriría modificar la estructura del GUI para soportar componentes dinámicos)
                // Esta es una solución simplificada: solo mostrar "[IMAGEN]" en el área de chat
                chatArea.append("[IMAGEN RECIBIDA - Sin soporte de visualización en modo texto]\n");
            }
        } catch (Exception e) {
            System.err.println("Error al mostrar imagen: " + e.getMessage());

            // Notificar el error en el chat
            String errorMsg = "[Error al mostrar imagen: " + e.getMessage() + "]";
            if (chatPane != null) {
                appendToChat("<div style='color:red'>" + errorMsg + "</div>");
            } else if (chatArea != null) {
                chatArea.append(errorMsg + "\n");
            }
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
        try {
            // Usar FlatLaf con tema personalizado
            FlatLightLaf.setup();

            // Personalizar colores y componentes
            UIManager.put("Component.arrowType", "chevron");
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.width", 14);

            // Colores personalizados
            UIManager.put("TextField.foreground", new java.awt.Color(45, 45, 45));
            UIManager.put("TextField.background", new java.awt.Color(252, 252, 252));
            UIManager.put("Button.foreground", new java.awt.Color(245, 245, 245));
            UIManager.put("Button.background", new java.awt.Color(90, 170, 235));
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF: " + ex);
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

    public void announcePresence() {
        // Envia un mensaje JOIN para notificar tu presencia al nuevo usuario
        producer.sendJoinMessage();
    }
}
