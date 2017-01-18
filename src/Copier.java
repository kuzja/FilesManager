import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

public class Copier implements Runnable {

    public static long writtenBytes;
    private final RandomAccessFile in;
    private final RandomAccessFile out;
    private int bufferSize;

    public Copier(RandomAccessFile in, RandomAccessFile out, int bufferSize) {
        this.in = in;
        this.out = out;
        this.bufferSize = bufferSize;
    }

    private void copy() throws IOException, InterruptedException {
        byte[] buffer = new byte[bufferSize];
        int byteCount = -1;

        while (true) {

            synchronized (in) {
                byteCount = in.read(buffer);
                TimeUnit.MILLISECONDS.sleep(3);
            }

            synchronized (out) {
                if (byteCount == -1) {
                    break;
                }
                writtenBytes = writtenBytes + byteCount;
                out.write(buffer, 0, byteCount);
                TimeUnit.MILLISECONDS.sleep(3);
            }

        }
    }


    @Override
    public void run() {
        try {
            copy();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}