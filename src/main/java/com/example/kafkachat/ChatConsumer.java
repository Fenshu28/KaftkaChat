package com.example.kafkachat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import com.google.gson.Gson;

/**
 * Clase que maneja la recepción de mensajes del broker Kafka.
 */
public class ChatConsumer implements Runnable {

    private final Consumer<String, String> consumer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Gson gson = new Gson();
    private final String username;
    private final MessageListener messageListener;

    /**
     * Constructor que inicializa el consumidor Kafka.
     *
     * @param bootstrapServers Dirección de los servidores Kafka
     * @param username Nombre de usuario del cliente
     * @param messageListener Listener para notificar mensajes recibidos
     */
    public ChatConsumer(String bootstrapServers, String username, MessageListener messageListener) {
        this.username = username;
        this.messageListener = messageListener;

        Properties props = KafkaConfig.getConsumerProperties(bootstrapServers, username);
        this.consumer = new KafkaConsumer<>(props);

        // Suscribirse a los tópicos relevantes
        consumer.subscribe(Arrays.asList(
                KafkaConfig.TOPIC_MESSAGES,
                KafkaConfig.TOPIC_USER_STATUS,
                KafkaConfig.TOPIC_PRIVATE_MESSAGES
        ));
    }

    /**
     * Bucle principal para consumir mensajes.
     */
    @Override
    public void run() {
        try {
            while (!closed.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    processMessage(record);
                }
            }
        } catch (WakeupException e) {
            // Ignorar si estamos cerrando
            if (!closed.get()) {
                throw e;
            }
        } finally {
            consumer.close();
        }
    }

    /**
     * Procesa un mensaje recibido.
     *
     * @param record Registro consumido de Kafka
     */
    private void processMessage(ConsumerRecord<String, String> record) {
        try {
            String value = record.value();
            ChatMessage message = gson.fromJson(value, ChatMessage.class);

            // Cuando recibimos un mensaje JOIN de otro usuario, respondemos con nuestro propio JOIN
            if (ChatMessage.TYPE_JOIN.equals(message.getType()) && !username.equals(message.getSender())) {
                // Notificamos nuestra presencia al nuevo usuario
                if (messageListener instanceof ChatGUI) {
                    ((ChatGUI) messageListener).announcePresence();
                }
            }

            // Resto del código existente
            if (ChatMessage.TYPE_PRIVATE.equals(message.getType())) {
                if (!username.equals(message.getRecipient()) && !username.equals(message.getSender())) {
                    return; // Ignorar mensajes privados para otros usuarios
                }
            }

            // Notificar al listener
            if (messageListener != null) {
                messageListener.onMessageReceived(message);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar mensaje: " + e.getMessage());
        }
    }

    /**
     * Cierra el consumidor de forma segura.
     */
    public void close() {
        closed.set(true);
        consumer.wakeup();
    }

    /**
     * Interfaz para notificar mensajes recibidos.
     */
    public interface MessageListener {

        void onMessageReceived(ChatMessage message);
    }
}
