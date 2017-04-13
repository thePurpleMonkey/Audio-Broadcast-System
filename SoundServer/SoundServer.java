import java.io.IOException;

public class SoundServer {
    private static SoundServerThread thread;

    public static void main(String[] args) throws IOException {        
        thread = new SoundServerThread();
        thread.start();
    }
}