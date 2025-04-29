package com.example.kafkachat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel selector de emojis para el chat.
 */
public class EmojiSelector extends JPanel {
    
    private static final String[] EMOJIS = {
        "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", 
        "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗", 
        "😙", "😚", "🙂", "🤗", "🤩", "🤔", "🤨", "😐", 
        "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", 
        "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜", 
        "😝", "🤤", "😒", "😓", "😔", "😕", "🙃", "🤑", 
        "😲", "☹️", "🙁", "😖", "😞", "😟", "😤", "😢", 
        "😭", "😦", "😧", "😨", "😩", "🤯", "😬", "😰"
    };
    
    private ActionListener emojiClickListener;
    
    /**
     * Constructor que inicializa el panel de emojis
     * 
     * @param emojiClickListener Listener para manejar el clic en un emoji
     */
    public EmojiSelector(ActionListener emojiClickListener) {
        this.emojiClickListener = emojiClickListener;
        initComponents();
    }
    
    /**
     * Inicializa los componentes del panel
     */
    private void initComponents() {
        setLayout(new GridLayout(8, 8, 2, 2));
        
        for (String emoji : EMOJIS) {
            JButton emojiButton = new JButton(emoji);
            emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            emojiButton.setFocusPainted(false);
            emojiButton.setContentAreaFilled(false);
            emojiButton.setBorderPainted(false);
            emojiButton.setMargin(new Insets(0, 0, 0, 0));
            emojiButton.setActionCommand(emoji);
            emojiButton.addActionListener(emojiClickListener);
            
            // Tooltip descriptivo
            String description = getEmojiDescription(emoji);
            emojiButton.setToolTipText(description);
            
            add(emojiButton);
        }
    }
    
    /**
     * Obtiene una descripción para el emoji
     * 
     * @param emoji El emoji a describir
     * @return Descripción del emoji
     */
    private String getEmojiDescription(String emoji) {
        // Descripción básica para algunos emojis
        switch (emoji) {
            case "😀": return "Cara sonriente";
            case "😂": return "Cara con lágrimas de risa";
            case "😍": return "Cara con ojos de corazón";
            case "🤔": return "Cara pensando";
            case "😎": return "Cara con gafas de sol";
            case "😭": return "Cara llorando";
            case "🥰": return "Cara sonriente con corazones";
            // Para los demás solo devolvemos "Emoji"
            default: return "Emoji";
        }
    }
}