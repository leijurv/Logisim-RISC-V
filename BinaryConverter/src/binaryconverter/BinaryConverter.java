/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package binaryconverter;

import java.util.Scanner;

/**
 *
 * @author leijurv
 */
public class BinaryConverter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        //Scanner scan = new Scanner(new ProcessBuilder("/opt/riscv/bin/riscv32-unknown-elf-objdump", "-s", "/Users/leijurv/Downloads/test").start().getInputStream());
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
        scan.nextLine();
        scan.nextLine();
        System.out.println("v2.0 raw");
        int pos = 0;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.startsWith("Contents of section")) {
                if (line.equals("Contents of section .comment:")) {
                    break;
                }
                continue;
            }
            int linePos = Integer.parseInt(line.substring(0, 7).trim(), 16);
            if (linePos - pos != 0) {
                System.out.print((linePos - pos) / 4 + "*0 ");
                pos = linePos;
            }
            String restOfLine = line.substring(7, 44).trim();
            String[] parts = restOfLine.split(" ");
            for (String part : parts) {
                for (int i = part.length() - 2; i >= 0; i -= 2) {
                    System.out.print(part.substring(i, i + 2));
                }
                System.out.print(" ");
            }
            pos += restOfLine.replace(" ", "").length() / 2;
        }
    }

}
