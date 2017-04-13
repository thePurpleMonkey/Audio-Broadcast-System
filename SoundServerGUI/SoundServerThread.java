import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import java.awt.Color;

public class SoundServerThread extends Thread {
    protected DatagramSocket socket;
	protected boolean running;
	protected CopyOnWriteArrayList<InetSocketAddress> clients;
	private NewClientThread clientThread;
	private AudioFormat format;
	private TargetDataLine line;
	private JLabel statusLabel;
	private JLabel clientLabel;

    public SoundServerThread(JLabel clients, JLabel label) throws IOException {
		this("SoundServerThread", clients, label);
    }

    public SoundServerThread(String name, JLabel clientLabel, JLabel label) throws IOException {
        super(name);
		format = new AudioFormat(44100, 16, 2, true, false);
        socket = new DatagramSocket(4041);
		socket.setBroadcast(true);
		statusLabel = label;
		clientLabel = clientLabel;

		clients = new CopyOnWriteArrayList<InetSocketAddress>();
		clientThread = new  NewClientThread();
    }

	private void updateStatus(String status) {
		updateStatus(status, false);
	}

	private void updateStatus(String status, boolean error) {
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				clientLabel.setText("Clients: " + clients.size());
				statusLabel.setText(status);
				System.out.println(status);

				if (error) {
					statusLabel.setForeground(Color.RED);
				} else {
					statusLabel.setForeground(Color.BLACK);
				}
			}
        });
	}

    public void run() {
		running = true;
		clientThread.start();

		// Schedule a task to announce presense every 5 seconds
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					if (socket != null && !socket.isClosed() && running) {
						byte[] msg = String.format("SoundServer:%d", socket.getLocalPort()).getBytes();
						socket.send(new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"), 4040));
					}
				} catch (IOException ex) {
					updateStatus("Unable to announce: " + ex.getMessage(), true);
				}
			}
		}, 0, 5000);
		
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
		if (!AudioSystem.isLineSupported(info)) {
			updateStatus("AudioSystem line is not supported", true);
			running = false;
			return;
		}

		// Obtain and open the line.
		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
		} catch (LineUnavailableException ex) {
			updateStatus("Line Unavailable: " + ex.getMessage());
			running = false;
			return;
		}

		updateStatus("Buffer size: " + line.getBufferSize()/10);

		// Assume that the TargetDataLine, line, has already
		// been obtained and opened.
		int numBytesRead;
		byte[] data = new byte[line.getBufferSize() / 10];

		// Begin audio capture.
		line.start();

		updateStatus("Transmitting...", true);
		//Random r = new Random();

        while (running) {
            try {
                for (InetSocketAddress client : clients) {
					numBytesRead = line.read(data, 0, data.length);
					//if (r.nextInt() % 10 < 8) // Simulate packet loss
					socket.send(new DatagramPacket(data, data.length, client));
					//System.out.print(".");
				}
            } catch (IOException e) {
                e.printStackTrace();
				running = false;
            }
        }

		line.close();
		updateStatus("Closing socket.");
		updateStatus("Running: " + running);
        socket.close();
    }

    public void Stop() {
		running = false;
	}

	private class NewClientThread extends Thread {
		public void run() {
			while (running) {
				byte[] buf = new byte[256];
				byte[] msg;

				try {
					// receive request
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					// display response
					String received = new String(packet.getData(), 0, packet.getLength());
					//System.out.println("Address: " + packet.getAddress().getHostAddress());
					switch (received) {
						case "register":
							updateStatus("Adding " +  packet.getSocketAddress() + " to client list.");
							clients.add((InetSocketAddress) packet.getSocketAddress());
							msg = "OK".getBytes();
							socket.send(new DatagramPacket(msg, msg.length, packet.getAddress(), packet.getPort()));
							break;

						case "unregister":
							updateStatus("Removing " + packet.getSocketAddress() + " from client list.");
							clients.remove((InetSocketAddress) packet.getSocketAddress());
							msg = "BYE".getBytes();
							socket.send(new DatagramPacket(msg, msg.length, packet.getAddress(), packet.getPort()));
							break;

						case "stop":
							updateStatus("Stopping server.");
							running = false;
							clients.add((InetSocketAddress) packet.getSocketAddress());
							buf = "STOPPING".getBytes("UTF-8");
							try {
								for (InetSocketAddress client : clients) {
									socket.send(new DatagramPacket(buf, buf.length, (SocketAddress) client));
								}
							} catch (IOException e) {
								// Do nothing. Already stopping server.
							}
							break;

						case "SoundServer":
							break; // Received own announcement
					
						default:
							updateStatus("Unknown command '" + received + "'");
							break;
					}
				
				} catch (IOException e) {
					e.printStackTrace();
					updateStatus("Error: " + e.getMessage(), true);
					running = false;
				}
			}
		}
	}
}