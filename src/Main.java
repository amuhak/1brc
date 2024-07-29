import org.graalvm.collections.Pair;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String FILE_PATH = "measurements.txt";
    private static final int BUFFER_SIZE = 1 << 23;
    private static final HashMap<bArr, value> map = new HashMap<>();
    private static final ConcurrentLinkedQueue<byte[]> bufferQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<RandomAccessFile> fileQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        RandomAccessFile file = new RandomAccessFile(FILE_PATH, "r");
        final long length = file.length();

        final List<Pair<Long, Long>> toDo = Collections.synchronizedList(new ArrayList<>());
        long to;

        for (long i = 0; i < length - 1; i = to) {
            to = Math.min(i + BUFFER_SIZE, length - 1);
            file.seek(to);
            while (file.read() != '\n' && to < length) {
                to++;
            }
            toDo.add(Pair.create(i, to));
        }

        fileQueue.add(file);

        final int noThreads = Runtime.getRuntime().availableProcessors();

        // Headroom is given to help smooth transitions between threads
        final byte[][] buffer = new byte[noThreads + Math.max(1, noThreads / 8)][BUFFER_SIZE + (BUFFER_SIZE >> 8)];
        bufferQueue.addAll(Arrays.asList(buffer));

        for (int i = 0; i < noThreads + Math.max(2, noThreads / 8) - 1; i++) {
            fileQueue.add(new RandomAccessFile(FILE_PATH, "r"));
        }

        ExecutorService service =
                new ThreadPoolExecutor(noThreads, noThreads, 1L, java.util.concurrent.TimeUnit.SECONDS,
                        new java.util.concurrent.LinkedBlockingQueue<>()); // Thread pool

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
                map.merge(key, new value(v.sum, v.min, v.max, v.n), (existingValue, newValue) -> {
                    existingValue.update(newValue.sum, newValue.min, newValue.max, newValue.n);
                    return existingValue;
                });
            }
        }


        final ArrayList<Pair<String, value>> list = new ArrayList<>();
        for (final bArr key : map.keySet()) {
            list.add(Pair.create(new String(key.arr), map.get(key)));
        }

        list.sort(Comparator.comparing(Pair::getLeft));

        final StringBuilder sb = new StringBuilder();
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

                byte[] temp = new byte[j - start];
                System.arraycopy(buffer, start, temp, 0, j - start);
                bArr name = new bArr(temp);

                j++;

                final int no;

                // Magic numbers come from subtracting various '0' values

                if (buffer[j] == '-') {
                    if (buffer[j + 2] == '.') {
                        no = (buffer[j + 1] * 10 + buffer[j + 3] - 0x210) * -1;
                        j += 3;
                    } else {
                        no = (buffer[j + 1] * 100 + buffer[j + 2] * 10 + buffer[j + 4] - 0x14d0) * -1;
                        j += 4;
                    }
                } else {
                    if (buffer[j + 1] == '.') {
                        no = buffer[j] * 10 + buffer[j + 2] - 0x210;
                        j += 2;
                    } else {
                        no = buffer[j] * 100 + buffer[j + 1] * 10 + buffer[j + 3] - 0x14d0;
                        j += 3;
                    }
                }

                map.merge(name, new value(no), (existingValue, _) -> {
                    existingValue.update(no);
                    return existingValue;
                });
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
