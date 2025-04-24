package com.example.kafkachat;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Configuración centralizada para los productores y consumidores de Kafka.
 */
public class KafkaConfig {
    
    // Configuración por defecto para el servidor Kafka
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    
    // Tópicos de Kafka para el chat
    public static final String TOPIC_MESSAGES = "chat-messages";
    public static final String TOPIC_USER_STATUS = "user-status";
    public static final String TOPIC_PRIVATE_MESSAGES = "private-messages";
    
    // Nombre de grupo para los consumidores
    private static final String CONSUMER_GROUP_ID = "chat-client-group";
    
    /**
     * Obtiene las propiedades de configuración para un productor Kafka.
     * 
     * @param bootstrapServers Dirección de servidores Kafka (host:puerto)
     * @return Properties con la configuración
     */
    public static Properties getProducerProperties(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
        }
        
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // Configuraciones adicionales para mejorar la confiabilidad
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Esperar confirmación de todos los replicas
        props.put(ProducerConfig.RETRIES_CONFIG, 3);  // Número de reintentos
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1); // Pequeño retraso para batching
        
        return props;
    }
    
    /**
     * Obtiene las propiedades de configuración para un consumidor Kafka.
     * 
     * @param bootstrapServers Dirección de servidores Kafka (host:puerto)
     * @param clientId ID único de este cliente
     * @return Properties con la configuración
     */
    public static Properties getConsumerProperties(String bootstrapServers, String clientId) {
        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
        }
        
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        
        // Usar el nombre de usuario como parte del ID de grupo para garantizar mensajes únicos
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID + "-" + clientId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        
        // Para nuevos clientes, comenzar desde los mensajes más recientes
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        // Commit automático de offsets
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        return props;
    }
}