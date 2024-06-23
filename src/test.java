import org.graalvm.collections.Pair;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class test {
    private static final String FILE_PATH = "measurements.txt";
    private static final int BUFFER_SIZE = 1 << 17;
    private static final ConcurrentHashMap<String, value> map = new ConcurrentHashMap<>();

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

        System.out.println("Here");

        toDo.stream().parallel().forEach(pair -> {
            long i = pair.getLeft();
            long too = pair.getRight();
            try {
                new test().execute(i, too);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        ArrayList<Pair<String, value>> list = new ArrayList<>();
        for (String key : map.keySet()) {
            list.add(Pair.create(key, map.get(key)));
        }

        list.sort(Comparator.comparing(Pair::getLeft));

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Pair<String, value> pair : list) {
            value v = pair.getRight();
            sb.append(pair.getLeft()).append("=").append(v.min / 10.0).append("/").append(
                    Math.round(v.sum / (double) v.n) / 10.0).append("/").append(v.max / 10.0).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("}");
        System.out.println(sb);

        System.out.println();
        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start) + "ms");
    }

    public void execute(long i, long to) throws Exception {
        var file = new RandomAccessFile(FILE_PATH, "r");
        byte[] buffer = new byte[BUFFER_SIZE * 2];
        file.seek(i);
        int delta = (int) (to - i);
        int len = file.read(buffer, 0, delta);
        buffer[len] = '\n';
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
            if (map.containsKey(name)) {
                map.get(name).update(no);
            } else {
                map.put(name, new value(no));
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

        public synchronized void update(int val) {
            sum += val;
            min = Math.min(min, val);
            max = Math.max(max, val);
            n++;
        }
    }


}
