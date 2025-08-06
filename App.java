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
    private DatagramSocket socket; // DatagramSocket for sending and receiving messages.
    private InetAddress remoteAddress; // InetAddress represents the IP address to which we will send data.
    private int remotePort = 20; // The port of the remote user to which we send text and audio.
    private int localPort = 10;  // Our own port, where we receive text and audio.
    private String remoteIp = "160.40.66.152"; // The IP address of the remote user.
    private Thread receiverThread; // Thread for receiving messages.

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

        // Set the remote IP address.
        remoteAddress = InetAddress.getByName(remoteIp);

        try {
            // Create DatagramSocket for sending and receiving messages.
            // Message reception happens on the localPort.
            socket = new DatagramSocket(localPort);
            // Thread for receiving messages (text + audio).
            receiverThread = new Thread(() -> {
                try {
                    while (true) {
                        byte[] buffer = new byte[1024]; // Buffer will store the received data.
                                                       // Max size of received data: 1024 bytes.
                        // To receive data you need to create a DatagramPacket.
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet); // Use the receive() method to get data.

                        // If the packet length is small, it may be text.
                        if (packet.getLength() < 1024) {
                            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                            textArea.append("Remote: " + receivedMessage + "\n"); // Display text in the GUI.
                        } else {
                            // If the length is large, it is likely audio data.
                            speakerLine.write(packet.getData(), 0, packet.getLength()); // Play audio to speaker.
                        }
                    }
                } catch (IOException ex) {
                    textArea.append("Error in receiving message: " + ex.getMessage() + "\n");
                }
            });
            receiverThread.start(); // Start the receiver thread.
        } catch (SocketException ex) {
            textArea.append("Error initializing socket: " + ex.getMessage() + "\n");
        }

        // Configure VoIP settings according to project specifications.
        // AudioFormat defines the audio parameters to be used:
        audioFormat = new AudioFormat(8000, 8, 1, true, false);
        /* 8000: Sampling rate, i.e., 8 kHz (8,000 samples per second).
         * 8: Sample size in bits (8 bits = 1 byte).
         * 1: Mono audio (1 channel).
         * true: Audio data is signed.
         * false: Audio data is in little-endian format (least significant byte first).
         */

        // Microphone configuration:
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        // DataLine.Info describes the desired data line type.
        // Specifies TargetDataLine (audio input line: microphone).
        // The info is associated with the audioFormat object to match the defined parameters.
        try {
            microphoneLine = (TargetDataLine) AudioSystem.getLine(info);
            microphoneLine.open(audioFormat);
            microphoneLine.start(); // Start recording audio from microphone.

            // Speaker configuration:
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
            // Now use SourceDataLine, which is an output line type: speakers.
            // Info is again associated with the same audioFormat.
            speakerLine = (SourceDataLine) AudioSystem.getLine(info);
            speakerLine.open(audioFormat);
            speakerLine.start(); // Start audio playback.
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
        if (e.getSource() == sendButton) { // If 'Send' button is clicked:
            String message = inputTextField.getText(); // Get the text from the inputTextField.
            if (message.isEmpty()) {
                return;
            }
            try {
                // Send message via UDP.
                byte[] buffer = message.getBytes(); // Convert the message to byte array.
                // Create DatagramPacket to send the message.
                // Message is sent to remoteAddress at remotePort.
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, remoteAddress, remotePort);
                socket.send(packet); // Send the packet using send().
                textArea.append("Local: " + message + "\n");  // Display sent message.
                inputTextField.setText("");
            } catch (IOException ex) {
                textArea.append("Error sending message: " + ex.getMessage() + "\n");
            }
        } else if (e.getSource() == callButton) { // If 'Call' button is clicked:
            // VoIP operation. Start voice communication.
            startVoiceCommunication();
        }
    }

    private void startVoiceCommunication() {
        // Thread for sending audio.
        Thread voiceSendThread = new Thread(() -> {
            byte[] buffer = new byte[1024]; // Buffer to store audio.

            try {
                while (true) {
                    // Read audio from microphone.
                    int bytesRead = microphoneLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) { // If buffer has data:
                        // Create packet to send audio.
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, remoteAddress, remotePort);
                        socket.send(packet); // Send packet.
                    }
                }
            } catch (IOException ex) {
                textArea.append("Error in audio transmission: " + ex.getMessage() + "\n");
            }
        });
        voiceSendThread.start(); // Start audio send thread.
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
