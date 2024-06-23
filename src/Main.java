import org.graalvm.collections.Pair;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        FastIO sc = new FastIO("measurements.txt");
        HashMap<String, value> map = new HashMap<>();
        String val;
        while ((val = sc.readLine()) != null) {
            String[] arr = val.split(";");
            String name = arr[0];
            int change = (int) (Double.parseDouble(arr[1]) * 10);
            if (!map.containsKey(name)) {
                map.put(name, new value(0, 0, 0));
            }
            value v = map.get(name);
            v.update(change);
        }
        ArrayList<Pair<String, value>> list = new ArrayList<>();
        for (String key : map.keySet()) {
            list.add(Pair.create(key, map.get(key)));
        }
        // Sort the list by string
        list.sort(Comparator.comparing(Pair::getLeft));
        // Print like Abha=5.0/18.0/27.4
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Pair<String, value> pair : list) {
            value v = pair.getRight();
            sb.append(pair.getLeft()).append("=")
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
        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start) + "ms");
    }

    public static class value {
        long sum;
        int min;
        int max;
        int n;

        public value(long sum, int min, int max) {
            this.sum = sum;
            this.min = min;
            this.max = max;
            this.n = 1;
        }

        public void update(int val) {
            sum += val;
            min = Math.min(min, val);
            max = Math.max(max, val);
            n++;
        }
    }

    public static class FastIO {
        public int BUFFER_SIZE = 1 << 18;
        private final DataInputStream din;
        private final byte[] buffer;
        private int bufferPointer, bytesRead;

        public FastIO() {
            din = new DataInputStream(System.in);
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public FastIO(String file_name) throws IOException {
            din = new DataInputStream(new FileInputStream(file_name));
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public String readLine() throws IOException {
            byte[] buf = new byte[64]; // line length
            int cnt = 0, c;
            while ((c = read()) != -1) {
                if (c == '\n') {
                    if (cnt != 0) {
                        break;
                    } else {
                        continue;
                    }
                }
                buf[cnt++] = (byte) c;
            }
            if (cnt == 0) {
                return null;
            }
            return new String(buf, 0, cnt);
        }

        public int nextInt() throws IOException {
            int ret = 0;
            byte c = read();
            while (c <= ' ') {
                c = read();
            }
            boolean neg = (c == '-');
            if (neg) {
                c = read();
            }
            do {
                ret = ret * 10 + c - '0';
            } while ((c = read()) >= '0' && c <= '9');

            return neg ? -ret : ret;
        }

        public long nextLong() throws IOException {
            long ret = 0;
            byte c = read();
            while (c <= ' ') {
                c = read();
            }
            boolean neg = (c == '-');
            if (neg) {
                c = read();
            }
            do {
                ret = ret * 10 + c - '0';
            } while ((c = read()) >= '0' && c <= '9');
            if (neg) {
                return -ret;
            }
            return ret;
        }

        public double nextDouble() throws IOException {
            double ret = 0, div = 1;
            byte c = read();
            while (c <= ' ') {
                c = read();
            }
            boolean neg = (c == '-');
            if (neg) {
                c = read();
            }

            do {
                ret = ret * 10 + c - '0';
            } while ((c = read()) >= '0' && c <= '9');

            if (c == '.') {
                while ((c = read()) >= '0' && c <= '9') {
                    ret += (c - '0') / (div *= 10);
                }
            }

            if (neg) {
                return -ret;
            }
            return ret;
        }

        private void fillBuffer() throws IOException {
            bytesRead = din.read(buffer, bufferPointer = 0, BUFFER_SIZE);
            if (bytesRead == -1) {
                buffer[0] = -1;
            }
        }

        private byte read() throws IOException {
            if (bufferPointer == bytesRead) {
                fillBuffer();
            }
            return buffer[bufferPointer++];
        }

        public void close() throws IOException {
            din.close();
        }
    }
}
