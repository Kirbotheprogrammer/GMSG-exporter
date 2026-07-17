import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.util.Scanner;

public class GmsgConverter {

    public static void main(String[] args) throws IOException {
        System.out.println("\n-e InputFile OutputFile\nExample: -e eui text\n\nPLEASE DON'T INSERT ANY EXTENSION!");
        Scanner sc = new Scanner(System.in);
        String[] thingToDo = sc.nextLine().split(" ");
        String mode = thingToDo[0];
        String inputFile = thingToDo[1];
        String outputFile = thingToDo[2];

        switch(mode) {
            case "-e": export(inputFile, outputFile);
        }
        sc.close();
    }

    static void export(String inputFile, String outputFile) throws IOException {
        File f = new File(inputFile + ".gmsg");
        File f2 = new File(outputFile+".txt");
        f2.createNewFile();
        BufferedWriter b = new BufferedWriter(new PrintWriter(f2));
        PushbackInputStream bis = new PushbackInputStream(new FileInputStream(f), 4);
        bis.skip(123057);

        StringBuilder sb = new StringBuilder();
        int data;

        while ((data = bis.read()) != -1) {

            if (data == 0x00) {
                int next = bis.read();
                if (next == 0x01) {
                    sb.append(' ');
                    continue;
                }
                if (next != -1) bis.unread(next);
                continue;
            }

            if (data == 0x31) {
                int next = bis.read();
                if (next == 0x27) {
                    sb.append('\n');
                    continue;
                }
                if (next != -1) bis.unread(next);
                sb.append('1');
                continue;
            }

            if (data == 0x7F) {
                int next1 = bis.read();
                int next2 = (next1 != -1) ? bis.read() : -1;
                int next3 = (next2 != -1) ? bis.read() : -1;

                if (next1 == 0x00 && next2 == 0x27 && next3 == 0x00) {
                    String msg = process(sb);
                    if (msg != null) {
                    	System.out.println(msg);
                        b.write(msg);
                        b.write("\n");
                    }
                    sb.setLength(0);
                    continue;
                }
                if (next3 != -1) bis.unread(next3);
                if (next2 != -1) bis.unread(next2);
                if (next1 != -1) bis.unread(next1);
                continue;
            }

            if (data == 0xC3) {
                int next = bis.read();
                if (next != -1) {
                    sb.append(new String(new byte[]{(byte) data, (byte) next}, "UTF-8"));
                }
            } else if (isPrintable(data)) {
                sb.append((char) data);
            }
        }
        String msg = process(sb);
        if (msg != null) {
            b.write(msg);
            b.write("\n");
        }
        bis.close();
        b.close();
    }

    private static boolean isPrintable(int data) {
        if (data == 58 || data == 40 || data == 41) return false;
        return (data >= 32 && data <= 126) || (data >= 128 && data <= 255) || data == 27 || data == 63;
    }

    private static String process(StringBuilder sb) {
        if (sb.length() > 0) {
            String msg = sb.toString().trim();
            if (msg.startsWith("1")) msg = msg.substring(1);
            if (msg.endsWith("1") && msg.length() > 0) msg = msg.substring(0, msg.length() - 1);
            if (!msg.isEmpty()) {
            	return msg;
            }
        }
		return null;
    }
}