# Sistema de Chat Distribuido con Apache Kafka

Este proyecto implementa un sistema de chat distribuido utilizando Apache Kafka como broker de eventos. Es un excelente ejemplo práctico de cómo funcionan los eventos distribuidos en un sistema real.

## Características

- Mensajería en tiempo real entre múltiples clientes
- Mensajes privados entre usuarios
- Notificaciones de conexión/desconexión de usuarios
- Lista de usuarios conectados
- Interfaz gráfica intuitiva

## Requisitos previos

1. Java JDK 11 o superior
2. Apache Maven
3. Apache Kafka 4.0 (con modo KRaft, sin necesidad de ZooKeeper)

## Instalación y Configuración

### 1. Instalar Java (JDK)

```bash
# Para sistemas basados en Debian/Ubuntu
sudo apt update
sudo apt install default-jdk

# Para sistemas basados en Red Hat/CentOS
sudo yum install java-11-openjdk

# Verifica la instalación
java -version
```

### 2. Instalar Kafka 4.0

```bash
# Descargar la distribución binaria de Kafka
wget https://downloads.apache.org/kafka/4.0.0/kafka_2.13-4.0.0.tgz

# Extraer el archivo
tar -xzf kafka_2.13-4.0.0.tgz

# Mover a tu directorio home para mejor manejo de permisos
mkdir -p ~/kafka
cp -r kafka_2.13-4.0.0/* ~/kafka/
cd ~/kafka
```

### 3. Configurar Kafka en modo KRaft (sin ZooKeeper)

Kafka 4.0 puede ejecutarse sin ZooKeeper usando el modo KRaft. Sigue estos pasos para configurarlo:

#### 3.1 Crear archivo de configuración para KRaft

```bash
mkdir -p config/kraft
cat > config/kraft/server.properties << EOL
# ID y rol del nodo
node.id=1
process.roles=broker,controller

# Configuración del controlador
controller.quorum.voters=1@localhost:9093
controller.listener.names=CONTROLLER

# Listeners y configuración de red
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
inter.browser.listener.name=PLAINTEXT
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT

# Directorio de logs
log.dirs=~/kafka/logs

# Configuraciones básicas
num.partitions=1
default.replication.factor=1
min.insync.replicas=1
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
EOL
```

#### 3.2 Crear directorio para logs

```bash
mkdir -p ~/kafka/logs
```

#### 3.3 Generar un ID de clúster y formatear el almacenamiento

```bash
# Generar un ID de clúster
bin/kafka-storage.sh random-uuid
# Ejemplo de salida: Df-MLVT1QQqpguSUUW9q-g

# Formatear el almacenamiento usando el ID generado
bin/kafka-storage.sh format -t TU_UUID_GENERADO -c config/kraft/server.properties
# Reemplaza TU_UUID_GENERADO con el valor obtenido en el paso anterior
```

### 4. Iniciar el servidor Kafka

```bash
bin/kafka-server-start.sh config/kraft/server.properties
```

### 5. Crear los tópicos necesarios

En una nueva terminal, crea los tópicos necesarios para el chat:

```bash
cd ~/kafka
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3 --topic chat-messages
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3 --topic user-status
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3 --topic private-messages
```

## Compilación

Para compilar el proyecto, ejecuta:

```bash
mvn clean package
```

Esto generará un archivo JAR ejecutable con todas las dependencias incluidas en el directorio `target/`.

## Ejecución

Puedes ejecutar la aplicación con:

```bash
java -jar target/kafka-chat-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Al iniciar, la aplicación mostrará un diálogo para ingresar:
- La dirección del servidor Kafka (por defecto: localhost:9092)
- Tu nombre de usuario

## Uso en diferentes máquinas

Para usar el chat en diferentes máquinas:

1. Asegúrate de que todas las máquinas puedan acceder al servidor Kafka
   - Si estás utilizando Kafka localmente, configura tu servidor para aceptar conexiones externas:
     - En tu archivo `config/kraft/server.properties`, asegúrate de tener:
     - `listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://:9093`
     - `advertised.listeners=PLAINTEXT://tu_ip_pública:9092`
     - Reinicia el servidor Kafka

2. Compila el JAR y distribúyelo a las diferentes máquinas

3. En cada máquina, ejecuta el JAR con la dirección correcta del servidor Kafka:
   - Cuando la aplicación solicite la dirección del servidor, introduce `ip_del_servidor_kafka:9092`

## Arquitectura del Sistema

### Componentes principales

1. **ChatMessage**: Clase que representa los mensajes del chat
2. **KafkaConfig**: Configuración centralizada para Kafka
3. **ChatProducer**: Maneja el envío de mensajes a Kafka
4. **ChatConsumer**: Recibe mensajes de Kafka
5. **ChatGUI**: Interfaz gráfica de usuario

### Tópicos de Kafka

- **chat-messages**: Para mensajes generales del chat
- **user-status**: Para eventos de conexión/desconexión
- **private-messages**: Para mensajes privados entre usuarios

## Relación con la Teoría de Eventos Distribuidos

Este sistema implementa varios conceptos clave de eventos distribuidos:

1. **Desacoplamiento**: Los clientes de chat no se comunican directamente entre sí, sino a través del broker Kafka.

2. **Comunicación asíncrona**: Los mensajes se envían sin esperar respuesta inmediata.

3. **Publicación/Suscripción**: Los clientes publican mensajes en tópicos y reciben los que les interesan.

4. **Escalabilidad**: El sistema puede manejar un gran número de usuarios y mensajes.

5. **Tolerancia a fallos**: Si un cliente se desconecta, los demás siguen funcionando normalmente.

6. **Persistencia de eventos**: Kafka almacena los mensajes, permitiendo que los clientes que se conecten más tarde puedan recibir mensajes anteriores.

## Extensiones posibles

Algunas ideas para extender este proyecto:

1. Historial de mensajes persistente
2. Grupos/canales de chat
3. Envío de archivos
4. Cifrado de mensajes
5. Autenticación de usuarios
6. Indicador de "escribiendo..."
7. Confirmaciones de lectura

## Solución de problemas

**Error de conexión a Kafka**:
- Verifica que Kafka esté ejecutándose
- Comprueba que la dirección y puerto sean correctos
- Asegúrate de que no haya firewalls bloqueando la conexión

**No se reciben mensajes**:
- Verifica las suscripciones a los tópicos
- Comprueba la configuración del grupo de consumidores

**Problemas con permisos al iniciar Kafka**:
- Si intentas ejecutar Kafka en `/opt` u otro directorio del sistema, puedes tener problemas de permisos
- Usa `sudo` o ejecuta Kafka desde tu directorio home como se indica en esta guía

**Error con el archivo server.properties**:
- Verifica la configuración del archivo `config/kraft/server.properties`
- Asegúrate de tener la propiedad `controller.listener.names=CONTROLLER` presente

## Licencia

Este proyecto está licenciado bajo la Licencia MIT.
