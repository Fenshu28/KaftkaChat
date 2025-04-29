package com.example.kafkachat;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.gson.Gson;

/**
 * Clase que maneja la producción de mensajes al broker Kafka.
 */
public class ChatProducer {

    private final Producer<String, String> producer;
    private final Gson gson = new Gson();
    private final String username;

    /**
     * Constructor que inicializa el productor Kafka.
     *
     * @param bootstrapServers Dirección de los servidores Kafka
     * @param username Nombre de usuario del cliente
     */
    public ChatProducer(String bootstrapServers, String username) {
        Properties props = KafkaConfig.getProducerProperties(bootstrapServers);
        this.producer = new KafkaProducer<>(props);
        this.username = username;

        // Enviar mensaje de unión al chat
        sendJoinMessage();
    }

    /**
     * Envía un mensaje al canal general de chat.
     *
     * @param content Contenido del mensaje
     */
    public void sendMessage(String content) {
        ChatMessage message = new ChatMessage(ChatMessage.TYPE_MESSAGE, username, content);
        String jsonMessage = gson.toJson(message);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_MESSAGES,
                username, // Usar el nombre de usuario como clave
                jsonMessage
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error al enviar mensaje: " + exception.getMessage());
            }
        });
    }

    /**
     * Envía un mensaje privado a un usuario específico.
     *
     * @param recipient Destinatario del mensaje
     * @param content Contenido del mensaje
     */
    public void sendPrivateMessage(String recipient, String content) {
        // Usamos el nuevo constructor con parámetro booleano adicional
        ChatMessage message = new ChatMessage(username, recipient, content, true);
        String jsonMessage = gson.toJson(message);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_PRIVATE_MESSAGES,
                recipient, // Usar el destinatario como clave para particionamiento
                jsonMessage
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error al enviar mensaje privado: " + exception.getMessage());
            }
        });
    }

    /**
     * Envía un mensaje de unión al chat.
     */
    public void sendJoinMessage() {
        ChatMessage joinMessage = new ChatMessage(ChatMessage.TYPE_JOIN, username, null);
        String jsonMessage = gson.toJson(joinMessage);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_USER_STATUS,
                username,
                jsonMessage
        );

        producer.send(record);
    }

    /**
     * Envía un mensaje de salida antes de cerrar el productor.
     */
    public void sendLeaveMessage() {
        ChatMessage leaveMessage = new ChatMessage(ChatMessage.TYPE_LEAVE, username, null);
        String jsonMessage = gson.toJson(leaveMessage);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_USER_STATUS,
                username,
                jsonMessage
        );

        producer.send(record);
    }

    /**
     * Envía una imagen al canal general.
     *
     * @param imageBase64 Imagen codificada en Base64
     * @param imageFormat Formato de la imagen (png, jpg, etc.)
     */
    public void sendImage(String imageBase64, String imageFormat) {
        ChatMessage message = new ChatMessage(username, imageBase64, imageFormat, "IMAGE");
        String jsonMessage = gson.toJson(message);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_MESSAGES,
                username, // Usar el nombre de usuario como clave
                jsonMessage
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error al enviar imagen: " + exception.getMessage());
            }
        });
    }

    /**
     * Envía una imagen privada a un usuario específico.
     *
     * @param recipient Destinatario del mensaje
     * @param imageBase64 Imagen codificada en Base64
     * @param imageFormat Formato de la imagen (png, jpg, etc.)
     */
    public void sendPrivateImage(String recipient, String imageBase64, String imageFormat) {
        ChatMessage message = new ChatMessage(username, recipient, imageBase64, imageFormat, "IMAGE");
        String jsonMessage = gson.toJson(message);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_PRIVATE_MESSAGES,
                recipient, // Usar el destinatario como clave para particionamiento
                jsonMessage
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error al enviar imagen privada: " + exception.getMessage());
            }
        });
    }

    /**
     * Cierra el productor y libera recursos.
     */
    public void close() {
        try {
            sendLeaveMessage();
            producer.flush();
            producer.close();
        } catch (Exception e) {
            System.err.println("Error al cerrar productor: " + e.getMessage());
        }
    }
}
