import org.graalvm.collections.Pair;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String FILE_PATH = "measurements.txt";
    private static final int BUFFER_SIZE = 1 << 23;
    private static final HashMap<bArr, value> map = new HashMap<>();
    public static byte[][] buffer;
    public static ConcurrentLinkedQueue<byte[]> bufferQueue = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<RandomAccessFile> fileQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        final long length;
        RandomAccessFile file = new RandomAccessFile(FILE_PATH, "r");

        length = file.length();

        ArrayList<Pair<Long, Long>> toDo = new ArrayList<>();
        long to;

        for (long i = 0; i < length - 1; i = to) {
            to = Math.min(i + BUFFER_SIZE, length - 1);
            file.seek(to);
            while (file.read() != '\n' && to < length) {
                to++;
            }
            toDo.add(Pair.create(i, to));
        }

        int noThreads = Runtime.getRuntime().availableProcessors();

        // 1.25 times the buffer size for headroom
        buffer = new byte[noThreads + (noThreads / 4)][BUFFER_SIZE + (BUFFER_SIZE >> 8)];
        bufferQueue.addAll(Arrays.asList(buffer));

        for (int i = 0; i < noThreads + (noThreads / 4); i++) {
            fileQueue.add(new RandomAccessFile(FILE_PATH, "r"));
        }

        ExecutorService service =
                new ThreadPoolExecutor(noThreads, noThreads, 1L, java.util.concurrent.TimeUnit.SECONDS,
                        new java.util.concurrent.LinkedBlockingQueue<>());
        ArrayList<HashMap<bArr, value>> results = new ArrayList<>();
        for (Pair<Long, Long> pair : toDo) {
            HashMap<bArr, value> result = new HashMap<>();
            results.add(result);
            service.submit(new Worker(pair.getLeft(), pair.getRight(), result));
        }

        service.shutdown();

        if (!service.awaitTermination(5, TimeUnit.MINUTES)) {
            System.err.println("""
                    Threads didn't finish in 5 minutes!\s
                     Something is seriously wrong!\s
                     Shutting down!""");
            service.shutdownNow();
        }

        for (final HashMap<bArr, value> result : results) {
            for (final Map.Entry<bArr, value> in : result.entrySet()) {
                final bArr key = in.getKey();
                final value v = in.getValue();
                map.computeIfAbsent(key, _ -> new value(v.sum, v.min, v.max, v.n)).update(v.sum, v.min, v.max, v.n);
            }
        }

        ArrayList<Pair<String, value>> list = new ArrayList<>();
        for (bArr key : map.keySet()) {
            list.add(Pair.create(new String(key.arr), map.get(key)));
        }

        list.sort(Comparator.comparing(Pair::getLeft));

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Pair<String, value> pair : list) {
            value v = pair.getRight();
            sb.append(pair.getLeft())
                    .append("=")
                    .append(v.min / 10.0)
                    .append("/")
                    .append(Math.round(v.sum / (double) v.n) / 10.0)
                    .append("/")
                    .append(v.max / 10.0)
                    .append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("}");
        System.out.println(sb);

        System.out.println();
        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start) + "ms");
    }

    static class Worker implements Runnable {
        long i;
        long to;
        HashMap<bArr, value> map;

        public Worker(long i, long to, HashMap<bArr, value> map) {
            this.i = i;
            this.to = to;
            this.map = map;
        }

        public void run() {
            byte[] buffer = bufferQueue.poll();
            var file = fileQueue.poll();
            int len = 0;
            try {
                file.seek(i);
                int delta = (int) (to - i);
                len = file.read(buffer, 0, delta);
                buffer[len] = '\n';
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int j = 0; j < len; j++) {
                if (buffer[j] == '\n') {
                    continue;
                }
                int start = j;

                while (buffer[j] != ';') {
                    j++;
                }

                // String name = new String(buffer, start, j - start);
                bArr name = new bArr(Arrays.copyOfRange(buffer, start, j));

                j++;
                int no = 0;
                boolean neg = false;

                if (buffer[j] == '-') {
                    neg = true;
                    j++;
                }

                while (buffer[j] != '\n') {
                    if (buffer[j] == '.') {
                        j++;
                        continue;
                    }
                    no *= 10;
                    no += buffer[j] - '0';
                    j++;
                }

                if (neg) {
                    no = -no;
                }

                final int finalNo = no;

                map.computeIfAbsent(name, _ -> new value(finalNo)).update(no);
            }

            bufferQueue.add(buffer);
            fileQueue.add(file);
        }
    }

    public static class value {
        long sum;
        int min;
        int max;
        int n;

        public value(int n) {
            this.sum = n;
            this.min = n;
            this.max = n;
            this.n = 1;
        }

        public value(long sum, int min, int max, int n) {
            this.sum = sum;
            this.min = min;
            this.max = max;
            this.n = n;
        }

        public void update(int val) {
            sum += val;
            min = Math.min(min, val);
            max = Math.max(max, val);
            n++;
        }

        private void update(long sum, int min, int max, int n) {
            this.sum += sum;
            this.min = Math.min(this.min, min);
            this.max = Math.max(this.max, max);
            this.n += n;
        }
    }

    public static class bArr {
        byte[] arr;

        public bArr(byte[] arr) {
            this.arr = arr;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arr);
        }

        @Override
        public boolean equals(Object obj) {
            return Arrays.equals(arr, ((bArr) obj).arr);
        }
    }
}
