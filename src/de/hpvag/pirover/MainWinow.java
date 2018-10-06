package de.hpvag.pirover;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.AWTEvent;
import java.io.IOException;
import java.math.BigInteger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

//import org.freedesktop.gstreamer.Bin;
//import org.freedesktop.gstreamer.Pipeline;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Timer;

import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.elements.AppSink;

public class MainWinow extends JFrame implements AWTEventListener {

	private JPanel contentPane;
	private Socket s;
	PrintWriter s_out;
	BufferedReader s_in;
	Timer keep_alive_timer;
	private static Pipeline video_pipe;
	private static Pipeline audio_pipe;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Gst.init("PiRover", args);
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWinow frame = new MainWinow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		//ConnectionWindow cw = new ConnectionWindow();
		//cw.connect();
	}

	/**
	 * Create the frame.
	 */
	public MainWinow() {
		setResizable(false);
		setTitle("PiRover Command Center");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 320, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JLabel lblUseArrowKeys = new JLabel("<html>Use arrow keys to control the PiRover. To disconnect just close the window.</html>");
		contentPane.add(lblUseArrowKeys, BorderLayout.NORTH);
		
		//JLabel lblArrowKetsIcon = new JLabel(new ImageIcon("img/kb.png"));
		//contentPane.add(lblArrowKetsIcon, BorderLayout.CENTER);
		
		// Gstreamer-Pipelines zusammen bauen, um Video und Audio zu empfangen.
		// Getrennte Pipelines für beides, da sonst sehr hohe Latenz bei Audio. 
		
		// JPEG-Pipeline.
		//Bin bin = Bin.launch("udpsrc uri=udp://0.0.0.0:5000 ! application/x-rtp,encoding-name=JPEG,payload=26 ! rtpjpegdepay ! jpegdec ! videoconvert", true);
		
		// H264-Pipeline
		//Bin bin = Bin.launch("udpsrc uri=udp://0.0.0.0:5000 ! application/x-rtp,encoding-name=H264,payload=96 ! rtph264depay ! h264parse ! avdec_h264 ! videoconvert", true);
		
		Pipeline video_pipe = Pipeline.launch("udpsrc uri=udp://0.0.0.0:5000 ! application/x-rtp,encoding-name=H264,payload=96 ! rtph264depay ! h264parse ! avdec_h264 ! videoconvert ! appsink name=as");
		//Pipeline audio_pipe = Pipeline.launch("udpsrc address=0.0.0.0 port=5001 caps=application/x-rtp ! rtppcmudepay ! mulawdec ! audioconvert ! autoaudiosink");
		Pipeline audio_pipe = Pipeline.launch("udpsrc uri=udp://0.0.0.0:5001 ! vorbisdec ! audioconvert ! autoaudiosink");
        
		// Die Video-Komponente zum Anzeigen des Videos im Fenster erzeugen.
        SimpleVideoComponent vc = new SimpleVideoComponent((AppSink) video_pipe.getElementByName("as"));
        contentPane.add(vc);
        vc.setPreferredSize(new Dimension(800, 600));
        pack();
		
		// Verbindung zum Server aufbauen.
		ConnectionDialog cd = new ConnectionDialog();
		
		while (true) {
			String[] str = cd.showDialog(); // Dialog zum Abfragen der Verbindungsdaten anzeigen.
			System.out.println("Die Adresse lautet: " + str[0] + " Der Port lautet: " + str[1] /*+ " Password: " + str[2]*/);
			
			// Verbindung herstellen.
			try {
				s = new Socket(str[0], Integer.parseInt(str[1]));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				showErrorMsg("Host " + str[0] + " not found!");
				continue;
			} catch (ConnectException e) {
				e.printStackTrace();
				showErrorMsg("Failed to connect to host " + str[0] + ":" + str[1] + "!");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				showErrorMsg("An unknown error occurred while trying to connect!");
				System.exit(0);
			}
			
			// Eingabe/Ausgabe vorbereiten und noch eniges andres.
			try {
				s_out = new PrintWriter(s.getOutputStream(), true);
				s_in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				
				// Server begruessen und testen, ob sich ein PiRover-Server meldet.
				s_out.println("Hello PiRover!");

				String answere = s_in.readLine();
				System.out.println("Server says: " + answere);
				
				Pattern p = Pattern.compile("PiRover 1.0 here! (.*)");
				Matcher m = null;
				if (answere != null) m = p.matcher(answere);
				
				if (answere == null || !m.find()) {
					showErrorMsg("No proper answere! No PiRover server?");
					continue;
				}
				
				// Salz fuer den Authentifizierungs-Hash aus der Begruessung extrahieren, Authentifizierungs-Hash berechnen und an Server senden.
				String authhash = null;
				
				try {
					authhash = new BigInteger(1, MessageDigest.getInstance("MD5").digest((str[2] + m.group(1)).getBytes())).toString(16);
					
				} catch (NoSuchAlgorithmException e) {
				}
				
				System.out.println("Salt for auth hash is: " + m.group(1) + " The following auth hash will be transmited: " + authhash);
				
				s_out.println(authhash);
				
				// Antwort des Servers auswerten (Hat die Authentifizierung geklappt?).
				answere = s_in.readLine();
				System.out.println("Server says: " + answere);
				
				if (answere != null && answere.equals("Client already connected!")) {
					showErrorMsg("An client is already connected!");
					s.close();
					continue;
				}
				
				if (answere == null || !answere.equals("OK")) {
					showErrorMsg("Authentication failed!");
					System.exit(0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			break;
		}
		
		// Dafuer sorgen, dass Eingaben mit der Tastatur abgefangen und verarbeitet werden können.
		Toolkit tk = Toolkit.getDefaultToolkit();
        tk.addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        
        // Ein Timer einrichten, der zyklisch ein sog. Keep-Alive-Datenpaket an den Server sendet.
        keep_alive_timer = new javax.swing.Timer(1000, new ActionListener() {
        	public void actionPerformed( ActionEvent e ) {
        			s_out.println("Keep alive");
				}
        	} );
        keep_alive_timer.start();
        
		video_pipe.play();
		audio_pipe.play();
	}
	
	/**
	 * Zeigt einen Dialog mit einer Fehlermeldung an.
	 * @param msg: Wortlaut der Fehlermeldung.
	 */
	private void showErrorMsg(String msg) {
		JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	@Override public void eventDispatched(AWTEvent e) {
		if (e instanceof KeyEvent) {
            KeyEvent key = (KeyEvent) e;
            /*** Wurde eine Taste gedrueckt? ***/
            if (key.getID() == KeyEvent.KEY_PRESSED) {
            	switch (key.getKeyCode()) {
                case KeyEvent.VK_UP:
                	s_out.println("UP pressed");
                	break;
                case KeyEvent.VK_DOWN:
                	s_out.println("DOWN pressed");
                	break;
                case KeyEvent.VK_LEFT:
                	s_out.println("LEFT pressed");
                	break;
                case KeyEvent.VK_RIGHT:
                	s_out.println("RIGHT pressed");
                }
            }
            /*** Wurde eine Taste los gelassen? ***/
            else if (key.getID() == KeyEvent.KEY_RELEASED) {
            	switch (key.getKeyCode()) {
                case KeyEvent.VK_UP:
                	s_out.println("UP released");
                	break;
                case KeyEvent.VK_DOWN:
                	s_out.println("DOWN released");
                	break;
                case KeyEvent.VK_LEFT:
                	s_out.println("LEFT released");
                	break;
                case KeyEvent.VK_RIGHT:
                	s_out.println("RIGHT released");
                }
            }
            
            //key.consume(); // Wenn diese Zeile einkommentiert wird, dann bekommt das Textfeld keine Eingaben mehr mit.
        }
	}

}
