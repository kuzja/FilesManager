import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileManager {

    private Scanner in;
    private String pathFrom;
    private String pathTo;
    private int threadCount;
    private int bufferSize;
    private long copyFilesLength;
    private Operation op;
    private ArrayList<File> fileList = new ArrayList<>();

    public FileManager() {
        this.in = new Scanner(System.in);
    }

    public static void main(String[] args) {

        FileManager fm = new FileManager();

        System.out.println("Specify copy from:");
        fm.setPathFrom(fm.getUserDataString());
        fm.checkSourcePath(fm.getPathFrom());

        System.out.println("Specify copy to:");
        fm.setPathTo(fm.getUserDataString());

        System.out.println("Specify threads count (minimum 2):");
        fm.setThreadCount(Integer.parseInt(fm.getUserDataString()));

        if (fm.getThreadCount() < 2) {
            fm.setThreadCount(2);
            System.out.println("Threads count is set 2");
        }

        System.out.println("Specify buffer size (Kb):");
        fm.setBufferSize(Integer.parseInt(fm.getUserDataString()));

        fm.checkSourcePath(fm.getPathFrom());

        fm.op = fm.getOperationType();

        fm.scanDirs(fm.getPathFrom());

        String sourceDir = "";

        if (fm.op == Operation.DirToDest) {
            sourceDir = new File(fm.getPathFrom()).getName();

            File directory = new File(fm.getPathTo() + "\\" + sourceDir);
            if (!directory.exists()) {
                directory.mkdir();
            }
        }

        for (File file : fm.fileList) {

            if (file.isDirectory()) {
                String path = fm.getPathTo() + "\\" + sourceDir + "\\" + file.getAbsolutePath().replace(fm.getPathFrom(), "");
                File directory = new File(path);
                if (!directory.exists()) {
                    directory.mkdir();
                }
            }
        }

        Thread countDown = new Thread(fm.new CountDown());


        fm.copyFilesLength = getFilesLength(fm.fileList);

        countDown.start();

        boolean success = true;

        for (File file : fm.fileList) {

            if (file.isDirectory()) {
                continue;
            }

            String source = "";
            String dest = "";

            if (fm.op == Operation.FileToDir) {
                source = fm.getPathFrom();
                dest = fm.getPathTo() + "\\" + file.getName();
            } else if (fm.op == Operation.DirToDest) {
                dest = fm.getPathTo() + "\\" + sourceDir + file.getAbsolutePath().replace(fm.getPathFrom(), "");
                source = file.getAbsolutePath();
            } else {
                source = fm.getPathFrom();
                dest = fm.getPathTo();
            }

            try (RandomAccessFile in = new RandomAccessFile(source, "r");
                 RandomAccessFile out = new RandomAccessFile(dest, "rw");) {

                ExecutorService executor = Executors.newFixedThreadPool(fm.getThreadCount());

                for (int i = 0; i < fm.getThreadCount(); i++) {
                    executor.execute(new Copier(in, out, fm.getBufferSize()));
                }

                executor.shutdown();

                while (!executor.isTerminated()) {/*NOP*/}

            } catch (IOException ex) {
                success = false;
                countDown.interrupt();
                System.out.println("An error occurred while copying files. Operation terminated");
                break;
            }
        }

        if (success) {
            countDown.interrupt();
            System.out.println("File(s) has been successfully copied.");
        }

    }

    private static long getFilesLength(ArrayList<File> fileList) {
        long fileLength = 0;

        for (File file : fileList) {
            fileLength = fileLength + file.length();
        }
        return fileLength;
    }

    private Operation getOperationType() {
        if (new File(pathFrom).isDirectory() & new File(getPathTo()).isDirectory()) {
            return Operation.DirToDest;
        } else if ((!(new File(pathFrom).isDirectory()) & !(new File(getPathTo()).isDirectory()))) {
            return Operation.FileToFile;
        } else {
            return Operation.FileToDir;
        }
    }

    public int getThreadCount() {
        return threadCount;
    }

    private void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    private void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize * 1024;
    }

    public String getPathFrom() {
        return pathFrom;
    }

    private void setPathFrom(String pathFrom) {
        this.pathFrom = pathFrom;
    }

    public String getPathTo() {
        return pathTo;
    }

    private void setPathTo(String pathTo) {
        this.pathTo = pathTo;
    }

    private void checkSourcePath(String path) {

        while (true) {

            File file = new File(path);

            if (!file.exists()) {
                System.out.println("Specified path does not exist");
                System.out.println("If you want other path write (y) or write (n) to exit");

                if (getUserDataString().equals("y")) {
                    System.out.println("Specify copy from:");
                    path = getUserDataString();

                    continue;
                } else {
                    System.exit(0);
                }
            }
            setPathFrom(path);
            break;
        }
    }

    private String getUserDataString() {
        return in.nextLine();
    }

    private void scanDirs(String path) {

        File f = new File(path);
        File[] listFiles = f.listFiles();

        if (op == Operation.FileToDir | op == Operation.FileToFile) {
            fileList.add(f);
            return;
        }

        if (listFiles != null) {
            for (File file : listFiles) {
                fileList.add(file);
                if (!file.isDirectory()) {
                    fileList.add(file);
                } else {
                    fileList.add(file);
                    scanDirs(file.getPath());
                }

            }
        }
    }

    class CountDown implements Runnable {
        @Override
        public void run() {
            try {
                long copied = 0;
                while (copyFilesLength != Copier.writtenBytes) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    TimeUnit.SECONDS.sleep(1);

                    double changeSpeed = Copier.writtenBytes - copied;

                    int remainingTime = (int) ((copyFilesLength - Copier.writtenBytes) / changeSpeed);

                    copied = Copier.writtenBytes;

                    System.out.printf("remaining time ......... %4ds %10.2f MB/s%n", remainingTime, changeSpeed / 1024 / 1024);
                }
            } catch (InterruptedException e) {/*NOP*/}
        }
    }
}
