import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// (C) 2013 Kim, Taegyoon
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
                System.out.println("Usage: java parenj [OPTIONS...] [FILES...]");
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
                String code = new String(Files.readAllBytes(Paths.get(fileName)));
                p.eval_string(code);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
