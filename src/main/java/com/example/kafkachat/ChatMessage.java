package com.example.kafkachat;

import java.util.Date;

/**
 * Clase que representa un mensaje en el sistema de chat basado en Kafka. Esta
 * clase debe ser serializable para ser enviada a través de Kafka.
 */
public class ChatMessage {

    // Tipos de mensajes
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_PRIVATE = "PRIVATE";
    public static final String TYPE_IMAGE = "IMAGE"; // Nuevo tipo para imágenes

    private String type;        // Tipo de mensaje
    private String sender;      // Nombre del remitente
    private String content;     // Contenido del mensaje o imagen codificada en Base64
    private String recipient;   // Destinatario (para mensajes privados)
    private long timestamp;     // Marca de tiempo
    private String imageFormat; // Formato de la imagen (para mensajes de tipo IMAGE)

    // Constructor por defecto (necesario para la deserialización)
    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructor para un nuevo mensaje de chat.
     *
     * @param type Tipo de mensaje
     * @param sender Nombre del remitente
     * @param content Contenido del mensaje
     */
    public ChatMessage(String type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructor para mensaje privado.
     *
     * @param sender Nombre del remitente
     * @param recipient Nombre del destinatario
     * @param content Contenido del mensaje
     * @param isPrivate Parámetro diferenciador (siempre debe ser true)
     */
    public ChatMessage(String sender, String recipient, String content, boolean isPrivate) {
        if (isPrivate) { // Este parámetro es solo para diferenciar constructores
            this.type = TYPE_PRIVATE;
            this.sender = sender;
            this.recipient = recipient;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        } else {
            throw new IllegalArgumentException("Este constructor es solo para mensajes privados");
        }
    }

    /**
     * Constructor para mensaje con imagen
     *
     * @param sender Nombre del remitente
     * @param imageBase64 Imagen codificada en Base64
     * @param imageFormat Formato de la imagen (ej: "png", "jpg")
     * @param imageFlag Flag para distinguir del constructor de mensaje normal
     */
    public ChatMessage(String sender, String imageBase64, String imageFormat, String imageFlag) {
        if ("IMAGE".equals(imageFlag)) {
            this.type = TYPE_IMAGE;
            this.sender = sender;
            this.content = imageBase64;
            this.imageFormat = imageFormat;
            this.timestamp = System.currentTimeMillis();
        } else {
            throw new IllegalArgumentException("Este constructor es solo para mensajes con imagen");
        }
    }

    /**
     * Constructor para mensaje con imagen privado
     *
     * @param sender Nombre del remitente
     * @param recipient Nombre del destinatario
     * @param imageBase64 Imagen codificada en Base64
     * @param imageFormat Formato de la imagen (ej: "png", "jpg")
     * @param imageFlag Flag para distinguir del constructor de mensaje privado
     */
    public ChatMessage(String sender, String recipient, String imageBase64, String imageFormat, String imageFlag) {
        if ("IMAGE".equals(imageFlag)) {
            this.type = TYPE_IMAGE;
            this.sender = sender;
            this.recipient = recipient;
            this.content = imageBase64;
            this.imageFormat = imageFormat;
            this.timestamp = System.currentTimeMillis();
        } else {
            throw new IllegalArgumentException("Este constructor es solo para mensajes con imagen privada");
        }
    }

    // Getters y setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    /**
     * Comprueba si el mensaje contiene una imagen
     *
     * @return true si es un mensaje de tipo imagen
     */
    public boolean isImage() {
        return TYPE_IMAGE.equals(type);
    }

    /**
     * Formatea el mensaje para mostrarlo en la interfaz de chat.
     */
    public String getFormattedMessage() {
        Date date = new Date(timestamp);

        if (TYPE_JOIN.equals(type)) {
            return String.format("[%tT] %s se ha unido al chat", date, sender);
        } else if (TYPE_LEAVE.equals(type)) {
            return String.format("[%tT] %s ha abandonado el chat", date, sender);
        } else if (TYPE_PRIVATE.equals(type)) {
            return String.format("[%tT] [Privado de %s]: %s", date, sender, content);
        } else if (TYPE_IMAGE.equals(type)) {
            if (recipient != null) {
                return String.format("[%tT] [Imagen privada de %s]", date, sender);
            } else {
                return String.format("[%tT] [Imagen de %s]", date, sender);
            }
        } else {
            return String.format("[%tT] %s: %s", date, sender, content);
        }
    }
}
