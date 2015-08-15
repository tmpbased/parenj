package paren;
import java.io.IOException;

// (C) 2013-2015 Kim, Taegyoon
// Parenj: The Paren Programming Language written in Java

public class parenj {
    public static Object testField;
    public static void main(String[] args) {
        if (args.length == 0) {
            paren p = new paren();
            p.print_logo();
            p.repl();
            System.out.println();
            return;
        } else if (args.length == 1) {
            if (args[0].equals("-h")) {
                System.out.println("Usage: java paren.parenj [OPTIONS...] [FILES...]");
                System.out.println();
                System.out.println("OPTIONS:");
                System.out.println("    -h    print this screen.");
                System.out.println("    -v    print version.");
                return;
            } else if (args[0].equals("-v")) {
                System.out.println(paren.VERSION);
                return;
            }
        }        
        
        // execute files, one by one
        for (String fileName : args) {
            paren p = new paren();
            try {
        		p.eval_string(paren.slurp(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
