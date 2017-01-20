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

    private void read() throws IOException, InterruptedException {
        byte[] buffer = new byte[bufferSize];
        int byteCount = 0;

        while (byteCount > -1) {

            synchronized (in) {
                byteCount = in.read(buffer);
                write(buffer, byteCount);
                TimeUnit.MILLISECONDS.sleep(1);
            }

            TimeUnit.MILLISECONDS.sleep(1);

        }
    }

    private void write(byte[] buffer, int byteCount) throws IOException, InterruptedException {

        synchronized (out) {
            if (byteCount > -1) {
                out.write(buffer, 0, byteCount);
                writtenBytes = writtenBytes + byteCount;
            }
        }

    }


    @Override
    public void run() {
        try {
            read();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}