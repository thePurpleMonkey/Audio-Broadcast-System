import java.io.IOException;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

public class SoundServer {
    private static SoundServerThread thread;

    public static void main(String[] args) throws IOException {        
        //Create and set up the window.
        JFrame frame = new JFrame("Sound Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(3000, 3000);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1, 10, 10));
        panel.setPreferredSize(new Dimension(500, 100));

        JButton button = new JButton("Start");
        JLabel clients = new JLabel("Clients: 0");
        JLabel label = new JLabel("Inactive.");

        thread = new SoundServerThread(clients, label);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                thread.start();
                button.setEnabled(false);
                label.setText("Running.");
            }          
        });

        panel.add(button);
        panel.add(clients);
        panel.add(label);

        frame.getContentPane().add(panel);
        frame.setSize(3000, 3000);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}