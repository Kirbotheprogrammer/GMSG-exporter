import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

public class GmsgConverter {

    public static void main(String[] args) throws IOException {
        System.out.println("\n-e InputFile OutputFile\n-i InputFile OutputFile\nExample: -e eui text\n\nPLEASE DON'T INSERT ANY EXTENSION!");
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
        FileInputStream bis = new FileInputStream(f);
        bis.skip(123057);

        StringBuilder sb = new StringBuilder();
        int data;

        while ((data = bis.read()) != -1) {

            // CHECK 00 01 (Space)
            if (data == 0x00) {
                int next = bis.read();
                if (next == 0x01) {
                    sb.append(' ');
                    continue;
                }
                // If it wasn't 00 01, handle the two bytes normally
                if (isPrintable(data)) sb.append((char)data);
                if (next != -1 && isPrintable(next)) sb.append((char)next);
                continue;
            }

            // CHECK 1' (Line break within the message)
            if (data == 0x31) {
                int next = bis.read();
                if (next == 0x27) {
                    sb.append('\n');
                    continue;
                }
                // If it wasn't 1', re-add the bytes
                sb.append('1');
                if (next != -1 && isPrintable(next)) sb.append((char)next);
                continue;
            }

            // CHECK 7F 00 27 00 (End of message)
            if (data == 0x7F) {
                int next1 = bis.read();
                int next2 = bis.read();
                int next3 = bis.read();

                if (next1 == 0x00 && next2 == 0x27 && next3 == 0x00) {
                    processAndDisplay(sb);
                    sb.setLength(0);
                    continue;
                }
                // If it wasn't the delimiter, re-add everything
                if (isPrintable(data)) sb.append((char)data);
                if (isPrintable(next1)) sb.append((char)next1);
                if (isPrintable(next2)) sb.append((char)next2);
                if (isPrintable(next3)) sb.append((char)next3);
                continue;
            }

            // Handle UTF-8 (C3 xx)
            if (data == 0xC3) {
                int next = bis.read();
                sb.append(new String(new byte[]{(byte)data, (byte)next}, "UTF-8"));
            } else if (isPrintable(data)) {
                sb.append((char) data);
            }
        }
        processAndDisplay(sb);
        bis.close();
    }

    private static boolean isPrintable(int data) {
        if (data == 58 || data == 40 || data == 41) return false;
        return (data >= 32 && data <= 126) || (data >= 128 && data <= 255) || data == 27 || data == 63;
    }

    private static void processAndDisplay(StringBuilder sb) {
        if (sb.length() > 0) {
            String msg = sb.toString().trim();
            if (msg.startsWith("1")) msg = msg.substring(1);
            if (msg.endsWith("1") && msg.length() > 0) msg = msg.substring(0, msg.length() - 1);
            if (!msg.isEmpty()) System.out.println(msg);
        }
    }
}