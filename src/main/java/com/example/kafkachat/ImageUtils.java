package com.example.kafkachat;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

/**
 * Clase de utilidad para manejar imágenes en el chat.
 */
public class ImageUtils {

    private static final int MAX_IMAGE_WIDTH = 300;
    private static final int MAX_IMAGE_HEIGHT = 300;

    /**
     * Convierte una imagen en un string Base64
     * 
     * @param file Archivo de imagen
     * @return Tupla con el string Base64 y el formato de la imagen
     * @throws IOException Si hay un error al leer la imagen
     */
    public static ImageData imageToBase64(File file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file);
        
        // Redimensionar la imagen si es demasiado grande
        BufferedImage resizedImage = resizeImageIfNeeded(originalImage);
        
        // Detectar formato de la imagen
        String formatName = getImageFormat(file.getName());
        
        // Convertir a Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, formatName, baos);
        byte[] imageBytes = baos.toByteArray();
        
        return new ImageData(Base64.getEncoder().encodeToString(imageBytes), formatName);
    }
    
    /**
     * Convierte un string Base64 en un componente JLabel con la imagen
     * 
     * @param base64Image String Base64 de la imagen
     * @param format Formato de la imagen
     * @return JLabel con la imagen
     * @throws IOException Si hay un error al decodificar la imagen
     */
    public static JLabel base64ToImageLabel(String base64Image, String format) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bais);
        
        // Crear un icono escalado para mostrar en el chat
        Image scaledImage = image.getScaledInstance(-1, 200, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImage);
        
        return new JLabel(icon);
    }
    
    /**
     * Redimensiona la imagen si excede los límites máximos
     * 
     * @param original Imagen original
     * @return Imagen redimensionada o la original si no excede los límites
     */
    private static BufferedImage resizeImageIfNeeded(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Comprobar si la imagen necesita ser redimensionada
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) {
            return original;
        }
        
        // Calcular nuevas dimensiones manteniendo la relación de aspecto
        double ratio = Math.min(
                (double) MAX_IMAGE_WIDTH / width,
                (double) MAX_IMAGE_HEIGHT / height
        );
        
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);
        
        // Crear imagen redimensionada
        BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resized;
    }
    
    /**
     * Obtiene el formato de la imagen a partir del nombre del archivo
     * 
     * @param fileName Nombre del archivo
     * @return Formato de la imagen (por defecto "png" si no se puede determinar)
     */
    private static String getImageFormat(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "png"; // Formato por defecto
    }
    
    /**
     * Clase interna para almacenar datos de imagen
     */
    public static class ImageData {
        private final String base64;
        private final String format;
        
        public ImageData(String base64, String format) {
            this.base64 = base64;
            this.format = format;
        }
        
        public String getBase64() {
            return base64;
        }
        
        public String getFormat() {
            return format;
        }
    }
}