import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundServerThread extends Thread {
    protected DatagramSocket socket = null;
	protected boolean running = true;
	private AudioFormat format;

    public SoundServerThread() throws IOException {
		this("SoundServerThread");
    }

    public SoundServerThread(String name) throws IOException {
        super(name);
		format = new AudioFormat(44100, 16, 2, true, false);
        socket = new MulticastSocket();
    }

    public void run() {
		TargetDataLine line;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
		if (!AudioSystem.isLineSupported(info)) {
			System.err.println("AudioSystem line is not supported");
			running = false;
			return;
		}

		//System.out.println("Hello: " + info.getFormats().length);
		//format = info.getFormats()[0];
		//info = new DataLine.Info(TargetDataLine.class, format);

		// Obtain and open the line.
		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
		} catch (LineUnavailableException ex) {
			System.err.println("Line Unavailable: " + ex.getMessage());
			running = false;
			return;
		}

		System.out.println("Buffer size: " + line.getBufferSize()/10);

		// Assume that the TargetDataLine, line, has already
		// been obtained and opened.
		int numBytesRead;
		byte[] data = new byte[line.getBufferSize() / 10];

		// Begin audio capture.
		line.start();

		System.out.print("Transmitting...");
		//Random r = new Random();

		InetAddress group = InetAddress.getByName("239.05.1.95");
		DatagramPacket packet;

        while (running) {
            try {
                //for (InetSocketAddress client : clients) {
					numBytesRead = line.read(data, 0, data.length);
					//if (r.nextInt() % 10 < 8) // Simulate packet loss
					//socket.send(new DatagramPacket(data, data.length, client));
					
					packet = new DatagramPacket(data, numBytesRead, group, 4040);
					socket.send(packet);

					//System.out.print(".");
				//}
            } catch (IOException e) {
                e.printStackTrace();
				running = false;
            }
        }

		line.close();
		socket.leaveGroup(group);
        socket.close();
    }

    public void Stop() {
		running = false;
	}

/*
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
							System.out.println("Adding " +  packet.getSocketAddress() + " to client list.");
							clients.add((InetSocketAddress) packet.getSocketAddress());
							msg = "OK".getBytes();
							socket.send(new DatagramPacket(msg, msg.length, packet.getAddress(), packet.getPort()));
							break;

						case "unregister":
							System.out.println("Removing " + packet.getSocketAddress() + " from client list.");
							clients.remove((InetSocketAddress) packet.getSocketAddress());
							msg = "BYE".getBytes();
							socket.send(new DatagramPacket(msg, msg.length, packet.getAddress(), packet.getPort()));
							break;

						case "stop":
							System.out.println("Stopping server.");
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
							System.err.println("Unknown command '" + received + "'");
							break;
					}
				
				} catch (IOException e) {
					e.printStackTrace();
					running = false;
				}
			}
		}
	}
*/
}