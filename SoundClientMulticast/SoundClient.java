import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.*;

public class SoundClient {
	private static boolean running = true;
	private static MulticastSocket socket;
	private static SourceDataLine out;
	private static SocketAddress address;
	private static InetAddress group;

    public static void main(String[] args) throws IOException {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() { 
				try {
					running = false;
					if (socket != null && !socket.isClosed()) {
							socket.leaveGroup(group);
							socket.close();
					}

					if (out != null)
						out.close();
				} catch (IOException exception) {
					// Do nothing. We're already exiting
				}
			}
		});

        // get a datagram socket
        socket = new MulticastSocket(4040);
		DatagramPacket packet;
		group = InetAddress.getByName("239.5.1.95");
		socket.joinGroup(group);
		AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
		byte[] buf = new byte[10000];
		String received;
    
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			System.err.println("out: AudioSystem line is not supported");
			return;
		}
		// Obtain and open the line.
		try {
			out = (SourceDataLine) AudioSystem.getLine(info);
			out.open(format);
		} catch (LineUnavailableException ex) {
			System.err.println("out: Line Unavailable");
			return;
		}

		byte[] data = null;

		out.start();
		/*
		System.out.println("Buffering...");

		System.out.println("Output buffer size: " + out.getBufferSize());
		byte[] startBuffer = new byte[out.getBufferSize()];
		//byte[] startBuffer = new byte[100000];
		int total = 0;
		while (total < startBuffer.length) {
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			data = packet.getData();
			if (total + data.length > startBuffer.length) {
				break;
			} else {
				System.arraycopy(data, 
                	0,
                	startBuffer,
                	total,
                	data.length);
				total += data.length;
				//System.out.println(total + " / " + startBuffer.length);
				System.out.print("\r" + ((total * 1.0 / startBuffer.length)*100) + "%");
			}
		}

		System.out.println("\nWriting buffer...");
		System.out.println("Buffer: " + startBuffer);
		out.write(startBuffer, 0, total);
		out.write(data, 0, data.length);
		*/

		byte[] playback_buffer;

		System.out.print("Playing...");
		try {
			while (running) {
				System.out.print(".");
				// get response
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				data = packet.getData();
				
				playback_buffer = Arrays.copyOfRange(data, 0, packet.getLength());

				//System.out.println("Data length: " + playback_buffer.length);
				out.write(playback_buffer, 0, playback_buffer.length);

				//String received = new String(packet.getData(), 0, packet.getLength());
				//System.out.println("Received: " + received);
			}
		} finally {
			out.close();
			socket.close();
		}
	}
}