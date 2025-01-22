package com.cn2.communication;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.util.Scanner;

public class App extends Frame implements WindowListener, ActionListener {

    static TextField inputTextField;
    static JTextArea textArea;
    static JButton sendButton;
    static JButton callButton;
    private DatagramSocket socket; // DatagramSocket για αποστολή και λήψη μηνυμάτων.
    private InetAddress remoteAddress; // Η κλάση InetAddress αντιπροσωπεύει την IP διέυθυνση στην οποία θα στείλουμε δεδομένα.
    private int remotePort = 10; // Το port του απομακρυσμένου χρήστη, στο οποίο στέλνουμε κείμενο και ήχο.
    private int localPort = 10;  // Το δικό μας port, στο οποίο λαμβάνουμε κείμενο και ήχο.
    private String remoteIp = "192.168.100.11"; // Η διεύθυνση IP του απομακρυσμένου χρήστη.
    private Thread receiverThread; // Νήμα για τη λήψη μηνυμάτων.

    private TargetDataLine microphoneLine;
    private SourceDataLine speakerLine;
    private AudioFormat audioFormat;

    public App(String title) throws UnknownHostException {
        super(title);
        setLayout(new FlowLayout());
        addWindowListener(this);

        inputTextField = new TextField(20);
        textArea = new JTextArea(10, 40);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        sendButton = new JButton("Send");
        callButton = new JButton("Call");

        add(scrollPane);
        add(inputTextField);
        add(sendButton);
        add(callButton);

        sendButton.addActionListener(this);
        callButton.addActionListener(this);

        // Ορισμός της απομακρυσμένης διεύθυνσης IP.
        remoteAddress = InetAddress.getByName(remoteIp);

        try {
    		// Δημιουργία DatagramSocket για αποστολή και λήψη μηνυμάτων.
    		// Η λήψη μηνυμάτων γίνεται στο localPort.
            socket = new DatagramSocket(localPort);
            // Νήμα για την λήψη μηνυμάτων (κειμένου + ήχου).
            receiverThread = new Thread(() -> {
                try {
                    while (true) {
                        byte[] buffer = new byte[1024]; // Ο buffer θα αποθηκεύσει τα δεδομένα που θα λάβουμε.
                        								// Μπορούμε να λάβουμε το μέγιστο 1024 bytes.
                        // Για να λάβεις δεδομένα πρέπει να δημιουργήσεις ένα DatagramPacket.
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet); // Για να λάβεις δεδομένα καλείς τη μέθοδο receive().

                        // Ελέγχουμε αν το μήκος του buffer είναι μικρό, οπότε ίσως πρόκειται για κείμενο.
                        if (packet.getLength() < 1024) {
                            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                            textArea.append("Remote: " + receivedMessage + "\n"); // Τυπώνει το κείμενο στην οθόνη του GUI.
                        } else {
                            // Αν το μήκος είναι μεγάλο, τότε πρόκειται για δεδομένα ήχου.
                            speakerLine.write(packet.getData(), 0, packet.getLength());  // Αναπαραγωγή ήχου στο ηχείο.
                        }
                    }
                } catch (IOException ex) {
                    textArea.append("Error in receiving message: " + ex.getMessage() + "\n");
                }
            });
            receiverThread.start(); // Εκκίνηση του νήματος λήψης μηνυμάτων.
        } catch (SocketException ex) {
            textArea.append("Error initializing socket: " + ex.getMessage() + "\n");
        }

        // Ρύθμιση παραμέτρων για την λειτουργία VoIP, σύμφωνα με τις προδιαγραφές της εργασίας.
        // Η AudioFormat καθορίζει τις παραμέτρους του ήχου που θα χρησιμοποιηθούν:
        audioFormat = new AudioFormat(8000, 8, 1, true, false);
        /* 8000: Η δειγματοληψία (sampling rate), δηλαδή 8 kHz (8.000 δείγματα ανά δευτερόλεπτο).
         * 8: Το μέγεθος κάθε δείγματος σε bits (8 bits, δηλαδή 1 byte).
         * 1: Μονοφωνικός ήχος (ένα κανάλι).
         * true: Υποδεικνύει ότι τα δεδομένα ήχου είναι signed (υπογεγραμμένα).
         * false: Υποδεικνύει ότι τα δεδομένα ήχου είναι σε little-endian μορφή (χαμηλότερο byte πρώτα).
         */
        
        // Ρύθμιση του μικροφώνου:
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        // Το αντικείμενο DataLine.Info περιγράφει την επιθυμητή γραμμή δεδομένων (data line).
        // Ορίζεται ότι η γραμμή είναι τύπου TargetDataLine (γραμμή εισόδου ήχου: μικρόφωνο).
        // Το info συνδέεται με το αντικείμενο audioFormat ώστε να αντιστοιχεί στις παραμέτρους που ορίσαμε.
        try {
            microphoneLine = (TargetDataLine) AudioSystem.getLine(info);
            microphoneLine.open(audioFormat);
            microphoneLine.start(); // Ξεκινά την εγγραφή ήχου από το μικρόφωνο.
            
            // Ρύθμιση των ηχείων:
            info = new DataLine.Info(SourceDataLine.class, audioFormat); 
            // Εδώ, αντί για TargetDataLine, χρησιμοποιείται SourceDataLine, που είναι τύπος γραμμής εξόδου (output line): ηχεία.
            // Το info συνδέεται ξανά με το ίδιο audioFormat.
            speakerLine = (SourceDataLine) AudioSystem.getLine(info);
            speakerLine.open(audioFormat);
            speakerLine.start(); // Ξεκινά την αναπαραγωγή ήχου από τα ηχεία.
        } catch (LineUnavailableException ex) {
            textArea.append("Error setting up audio line: " + ex.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        try {
            App app = new App("CN2 - AUTH");
            app.setSize(500, 300);
            app.setVisible(true);
        } catch (UnknownHostException ex) {
            System.out.println("Invalid IP address: " + ex.getMessage());
            System.exit(0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) { // Αν πατηθεί το κουμπί 'Send', κάνε αυτό:
            String message = inputTextField.getText(); // Λαμβάνει το κείμενο που θα στείλουμε από το πλαίσιο 'inputTextField'.
            if (message.isEmpty()) {
                return;
            }
            try {
                // Αποστολή μηνύματος μέσω UDP.
                byte[] buffer = message.getBytes(); // Αντιγραφή του κειμένου στον buffer, σε μορφή byte.
                // Για να στείλεις δεδομένα πρέπει να δημιουργήσεις ένα DatagramPacket.
                // Το μήνυμα στέλνεται στην διεύθυνση remoteAddress και στο remotePort.
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, remoteAddress, remotePort);
                socket.send(packet); // Στέλνω το μήνυμα με τη μέθοδο send().
                textArea.append("Local: " + message + "\n");  // Εμφάνιση κειμένου.
                inputTextField.setText("");
            } catch (IOException ex) {
                textArea.append("Error sending message: " + ex.getMessage() + "\n");
            }
        } else if (e.getSource() == callButton) { // Αν πατηθεί το κουμπί 'Call', κάνε αυτό.
            // Λειτουργία VoIP. Καλείται η κάτωθι μέθοδος.
            startVoiceCommunication();
        }
    }

    private void startVoiceCommunication() {
        // Νήμα αποστολής ήχου.
        Thread voiceSendThread = new Thread(() -> {
            byte[] buffer = new byte[1024]; // Ο buffer που θα αποθηκεύσει τον ήχο.

            try {
                while (true) {
                	// Διαβάζω τον ήχο από το μικρόφωνο.
                    int bytesRead = microphoneLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) { // Αν ο buffer έχει περιεχόμενο, κάνε αυτό:
                    	// Δημιουργώ πακέτο για να στείλω τον ήχο.
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, remoteAddress, remotePort);
                        socket.send(packet); // Στέλνω το πακέτο.
                    }
                }
            } catch (IOException ex) {
                textArea.append("Error in audio transmission: " + ex.getMessage() + "\n");
            }
        });
        voiceSendThread.start(); // Εκκίνηση του νήματος αποστολής ήχου.
    }

    @Override
    public void windowActivated(WindowEvent e) {}
    @Override
    public void windowClosed(WindowEvent e) {}
    @Override
    public void windowClosing(WindowEvent e) {
        dispose();
        System.exit(0);
    }
    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowDeiconified(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowOpened(WindowEvent e) {}
}