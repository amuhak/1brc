import org.graalvm.collections.Pair;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String FILE_PATH = "measurements.txt";
    private static final int BUFFER_SIZE = 1 << 23;
    private static final HashMap<String, value> map = new HashMap<>();

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

        ExecutorService service =
                new ThreadPoolExecutor(noThreads, noThreads, 1L, java.util.concurrent.TimeUnit.SECONDS,
                        new java.util.concurrent.LinkedBlockingQueue<>());
        ArrayList<HashMap<String, value>> results = new ArrayList<>();
        for (Pair<Long, Long> pair : toDo) {
            HashMap<String, value> result = new HashMap<>();
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

        for (final HashMap<String, value> result : results) {
            for (final String key : result.keySet()) {
                value v = result.get(key);
                map.computeIfAbsent(key, _ -> new value(v.sum, v.min, v.max, v.n)).update(v.sum, v.min, v.max, v.n);
            }
        }

        ArrayList<Pair<String, value>> list = new ArrayList<>();
        for (String key : map.keySet()) {
            list.add(Pair.create(key, map.get(key)));
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
        HashMap<String, value> map;

        public Worker(long i, long to, HashMap<String, value> map) {
            this.i = i;
            this.to = to;
            this.map = map;
        }

        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE + (BUFFER_SIZE >> 8)];
            int len = 0;
            try {
                var file = new RandomAccessFile(FILE_PATH, "r");
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
                String name = new String(buffer, start, j - start);
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
                int finalNo = no;

                map.computeIfAbsent(name, _ -> new value(finalNo)).update(no);
            }
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
}
