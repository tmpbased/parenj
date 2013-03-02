// Paren language core

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;
import java.lang.Math;

public class paren {
    static final String VERSION = "1.2";
    paren() {
        init();
    }
    
    static class node {
        boolean isSymbol;
        Object value;
        node () {}
        node(Object value) {
            this.value = value;
        }
        node(String s, boolean isSymbol) {
            this.value = s;
            this.isSymbol = isSymbol;
        }
        int intValue() {
            return ((Number)value).intValue();
        }
        double doubleValue() {
            return ((Number)value).doubleValue();
        }
        boolean booleanValue() {
            return (Boolean) value;
        }
        String stringValue() {
            if (value == null) return "";
            return value.toString();
        }
        @SuppressWarnings("unchecked")
        Vector<node> vectorValue() {
            return (Vector<node>)value;
        }
        
        String type() {
            if (value == null)
                return "null";
            else
                return value.getClass().getName();          
        }
        
        String str_with_type() {
            return stringValue() + " : " + type();
        }
        public String toString() {
            return stringValue();
        }
    }
    
    Vector<node> parse(String s) {
        s += ' '; // sentinel
        Vector<node> ret = new Vector<node>();
        String acc = ""; // accumulator
        int pos = 0;
        int last = s.length() - 1;
        for (;pos <= last; pos++) {
            char c = s.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ';') {
                if (c == ';') { // end-of-line comment
                    pos++;
                    while (pos <= last && s.charAt(pos) != '\n') pos++;
                }            
                String tok = acc;
                acc = "";
                if (tok.length() == 0) continue;

                if (tok.charAt(0) == '\'') { // left-quoted string
                    ret.add(new node(tok.substring(1)));
                } else if (tok.charAt(0) == '"') { // double-quoted string
                    ret.add(new node(tok.substring(1)));
                } else if (tok.charAt(0) == '(') { // list
                    ret.add(new node(parse(tok.substring(1))));
                } else if (Character.isDigit(tok.charAt(0)) || tok.charAt(0) == '-' && tok.length() >= 2 && Character.isDigit(tok.charAt(1))) { // number
                    if (tok.indexOf('.') != -1 || tok.indexOf('e') != -1) { // double
                        ret.add(new node(Double.parseDouble(tok)));
                    } else {
                        ret.add(new node(Integer.parseInt(tok)));
                    }
                } else { // variable get
                    ret.add(new node(tok, true));
                }
            } else if (acc.length() == 0 && c == '"') { // beginning of string
                acc += '"';
                pos++;
                while (true) {
                    if (s.charAt(pos) == '"') break;
                    if (pos < last && s.charAt(pos) == '\\') { // escape
                        char next = s.charAt(pos+1);
                        if (next == 'r') next = '\r';
                        else if (next == 'n') next = '\n';
                        else if (next == 't') next = '\t';
                        acc += next;
                        pos += 2;
                    }
                    else {
                        acc += s.charAt(pos);
                        pos++;
                    }
                }
            } else if (acc.length() == 0 && c == '(') { // beginning of list
                int numBracket = 1;
                acc += '(';
                pos++;
                while (true) {
                    switch (s.charAt(pos)) {
                    case '(': numBracket++; break;
                    case ')': numBracket--; break;
                    }
                    if (numBracket <= 0) break;
                    acc += s.charAt(pos);
                    pos++;
                }
            } else {
                acc += c;
            }
        }    
        return ret;
    }
    
    enum builtin {
        PLUS, MINUS, MUL, DIV, CARET, PERCENT, SQRT, INC, DEC, PLUSPLUS, MINUSMINUS, FLOOR, CEIL, LN, LOG10, E, PI, RAND,
        EQEQ, NOTEQ, LT, GT, LTE, GTE, ANDAND, OROR, NOT,
        IF, WHEN, FOR, WHILE,
        STRLEN, STRCAT, CHAR_AT, CHR,
        INT, DOUBLE, STRING, READ_STRING, TYPE, SET,
        EVAL, QUOTE, FN, LIST, APPLY, MAP, FILTER, RANGE, NTH, LENGTH, BEGIN, DOT, DOTGET, DOTSET,
        PR, PRN, EXIT
    }
    
    Hashtable<String, builtin> builtin_map = new Hashtable<String, builtin>();
    Hashtable<String, node> global_env = new Hashtable<String, node>(); // variables
    
    void print_symbols() {
        int i = 0;
        for (String key : new TreeSet<String>(global_env.keySet())) {
            System.out.print(" " + key);
            i++;
            if (i % 10 == 0) System.out.println();            
        }
        System.out.println();
    }
    
    void print_functions() {
        int i = 0;
        for (String key : new TreeSet<String>(builtin_map.keySet())) {            
            System.out.print(" " + key);
            i++;
            if (i % 10 == 0) System.out.println();
        }
        System.out.println();
    }
    
    void print_logo() {
        System.out.println(
            "Parenj " + VERSION + " (C) 2013 Kim, Taegyoon\n" +
            "Press Enter key twice to evaluate.");
        System.out.println(
            "Predefined Symbols:");
        print_symbols();
        System.out.println(
            "Functions:");
        print_functions();
        System.out.println(
            "Etc.:\n" +
            " (list) [string] ; end-of-line comment");
    }    

    void init() {
        global_env.put("true", new node(true));
        global_env.put("false", new node(false));
        global_env.put("E", new node(2.71828182845904523536));
        global_env.put("PI", new node(3.14159265358979323846));

        builtin_map.put("+", builtin.PLUS);
        builtin_map.put("-", builtin.MINUS);
        builtin_map.put("*", builtin.MUL);
        builtin_map.put("/", builtin.DIV);
        builtin_map.put("^", builtin.CARET);
        builtin_map.put("%", builtin.PERCENT);
        builtin_map.put("sqrt", builtin.SQRT);
        builtin_map.put("inc", builtin.INC);
        builtin_map.put("dec", builtin.DEC);
        builtin_map.put("++", builtin.PLUSPLUS);
        builtin_map.put("--", builtin.MINUSMINUS);
        builtin_map.put("floor", builtin.FLOOR);
        builtin_map.put("ceil", builtin.CEIL);
        builtin_map.put("ln", builtin.LN);
        builtin_map.put("log10", builtin.LOG10);
        builtin_map.put("rand", builtin.RAND);
        builtin_map.put("==", builtin.EQEQ);
        builtin_map.put("!=", builtin.NOTEQ);
        builtin_map.put("<", builtin.LT);
        builtin_map.put(">", builtin.GT);
        builtin_map.put("<=", builtin.LTE);
        builtin_map.put(">=", builtin.GTE);
        builtin_map.put("&&", builtin.ANDAND);
        builtin_map.put("||", builtin.OROR);
        builtin_map.put("!", builtin.NOT);
        builtin_map.put("if", builtin.IF);
        builtin_map.put("when", builtin.WHEN);
        builtin_map.put("for", builtin.FOR);
        builtin_map.put("while", builtin.WHILE);
        builtin_map.put("strlen", builtin.STRLEN);
        builtin_map.put("strcat", builtin.STRCAT);
        builtin_map.put("char-at", builtin.CHAR_AT);
        builtin_map.put("chr", builtin.CHR);
        builtin_map.put("int", builtin.INT);
        builtin_map.put("double", builtin.DOUBLE);
        builtin_map.put("string", builtin.STRING);
        builtin_map.put("read-string", builtin.READ_STRING);
        builtin_map.put("type", builtin.TYPE);
        builtin_map.put("eval", builtin.EVAL);
        builtin_map.put("quote", builtin.QUOTE);
        builtin_map.put("fn", builtin.FN);
        builtin_map.put("list", builtin.LIST);
        builtin_map.put("apply", builtin.APPLY);
        builtin_map.put("map", builtin.MAP);
        builtin_map.put("filter", builtin.FILTER);
        builtin_map.put("range", builtin.RANGE);
        builtin_map.put("nth", builtin.NTH);
        builtin_map.put("length", builtin.LENGTH);
        builtin_map.put("begin", builtin.BEGIN);
        builtin_map.put(".", builtin.DOT);
        builtin_map.put(".get", builtin.DOTGET);
        builtin_map.put(".set", builtin.DOTSET);
        builtin_map.put("set", builtin.SET);
        builtin_map.put("pr", builtin.PR);
        builtin_map.put("prn", builtin.PRN);
        builtin_map.put("exit", builtin.EXIT);
    }
    
    @SuppressWarnings("unchecked")
    node eval(node n, Hashtable<String, node> env) {        
        if (n.value == null ||
                n.value instanceof Integer ||
                n.value instanceof Double ||
                n.value instanceof Boolean) {
            return n;
        }
        else if (n.value instanceof String) {
            if (!n.isSymbol)
                return n;
            else {
                node found = env.get(n.value);
                if (found == null) {
                    builtin foundBuiltin = builtin_map.get(n.value);
                    if (foundBuiltin == null) {
                        System.err.println("Unknown variable: [" + n.value + "]");
                        return new node();
                    }
                    else { // built-in function
                        return n;
                    }
                }
                else { // variable
                    return found;
                }
            }
        }
        else if (n.value instanceof Vector) { // function (FUNCTION ARGUMENT ..)
            Vector<node> nvalue = n.vectorValue();                
            if (nvalue.size() == 0) return new node();
            node func = eval(nvalue.get(0), env);
            builtin found = builtin_map.get(func.value);
            if (found == null) {
                Vector<node> f = (Vector<node>)func.value;
                if (func.value instanceof Vector && f.size() >= 3 && f.get(0).value.equals("fn")) { // anonymous function application
                    // (fn (ARGUMENT ..) BODY ..)
                    Vector<node> arg_syms = (Vector<node>)f.get(1).value;                    
                    
                    Hashtable<String, node> env2 = (Hashtable<String, node>) env.clone();
                    int len = arg_syms.size();
                    for (int i=0; i<len; i++) { // assign arguments
                        env2.put((String)arg_syms.get(i).value, eval(((Vector<node>)n.value).get(i + 1), env2));
                    }
                    len = f.size();
                    for (int i=2; i<len-1; i++) { // body
                        eval(f.get(i), env2);
                    }
                    return eval(f.get(len-1), env2);
                }
                else {
                    System.err.println("Unknown function: [" + func.value.toString() + "]");
                    return new node();
                }
            }
            else { // built-in function
                switch(found) {
                case PLUS: // (+ X ..)
                    {
                        int len = ((Vector<node>)n.value).size();
                        if (len <= 1) return new node(0);
                        node first = eval(((Vector<node>)n.value).get(1), env);
                        if (first.value instanceof Integer) {
                            int acc = (Integer)first.value;
                            for (int i = 2; i < len; i++) {
                                acc += eval(((Vector<node>)n.value).get(i), env).intValue();
                            }
                            return new node(acc);
                        }
                        else {
                            double acc = (Double)first.value;
                            for (int i = 2; i < len; i++) {
                                acc += eval(((Vector<node>)n.value).get(i), env).doubleValue();
                            }
                            return new node(acc);
                        }
                    }
                case MINUS: // (- X ..)
                    {
                        int len = ((Vector<node>)n.value).size();
                        if (len <= 1) return new node(0);
                        node first = eval(((Vector<node>)n.value).get(1), env);
                        if (first.value instanceof Integer) {
                            int acc = (Integer)first.value;
                            for (int i = 2; i < len; i++) {
                                acc -= eval(((Vector<node>)n.value).get(i), env).intValue();
                            }
                            return new node(acc);
                        }
                        else {
                            double acc = (Double)first.value;
                            for (int i = 2; i < len; i++) {
                                acc -= eval(((Vector<node>)n.value).get(i), env).doubleValue();
                            }
                            return new node(acc);
                        }
                    }
                case MUL: // (* X ..)
                {
                    int len = ((Vector<node>)n.value).size();
                    if (len <= 1) return new node(1);
                    node first = eval(((Vector<node>)n.value).get(1), env);
                    if (first.value instanceof Integer) {
                        int acc = (Integer)first.value;
                        for (int i = 2; i < len; i++) {
                            acc *= eval(((Vector<node>)n.value).get(i), env).intValue();
                        }
                        return new node(acc);
                    }
                    else {
                        double acc = (Double)first.value;
                        for (int i = 2; i < len; i++) {
                            acc *= eval(((Vector<node>)n.value).get(i), env).doubleValue();
                        }
                        return new node(acc);
                    }
                }
                case DIV: // (/ X ..)
                {
                    int len = ((Vector<node>)n.value).size();
                    if (len <= 1) return new node(1);
                    node first = eval(((Vector<node>)n.value).get(1), env);
                    if (first.value instanceof Integer) {
                        int acc = (Integer)first.value;
                        for (int i = 2; i < len; i++) {
                            acc /= eval(((Vector<node>)n.value).get(i), env).intValue();
                        }
                        return new node(acc);
                    }
                    else {
                        double acc = (Double)first.value;
                        for (int i = 2; i < len; i++) {
                            acc /= eval(((Vector<node>)n.value).get(i), env).doubleValue();
                        }
                        return new node(acc);
                    }
                }
                case CARET: { // (^ BASE EXPONENT)
                    return new node(Math.pow(
                            eval(nvalue.get(1), env).doubleValue(),
                            eval(nvalue.get(2), env).doubleValue()));}
                case PERCENT: { // (% DIVIDEND DIVISOR)
                    return new node(eval(nvalue.get(1), env).intValue() % eval(nvalue.get(2), env).intValue());}
                case SQRT: { // (sqrt X)
                    return new node(Math.sqrt(eval(nvalue.get(1), env).doubleValue()));}
                case INC: { // (inc X)
                    int len = nvalue.size();
                    if (len <= 1) return new node(0);
                    node first = eval(nvalue.get(1), env);
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() + 1);
                    }
                    else {
                        return new node(first.doubleValue() + 1.0);
                    }
                }
                case DEC: { // (dec X)
                    int len = nvalue.size();
                    if (len <= 1) return new node(0);
                    node first = eval(nvalue.get(1), env);
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() - 1);
                    }
                    else {
                        return new node(first.doubleValue() - 1.0);
                    }
                }
                case PLUSPLUS: { // (++ X)
                    int len = nvalue.size();
                    if (len <= 1) return new node(0);
                    node first = eval(nvalue.get(1), env);
                    if (first.value instanceof Integer) {
                        String key = nvalue.get(1).stringValue();
                        int value = env.get(key).intValue();
                        env.put(key, new node(value + 1));
                        return new node();
                    }
                    else {
                        String key = nvalue.get(1).stringValue();
                        double value = env.get(key).doubleValue();
                        env.put(key, new node(value + 1));
                        return new node();
                    }
                }
                case MINUSMINUS: { // (-- X)
                    int len = nvalue.size();
                    if (len <= 1) return new node(0);
                    node first = eval(nvalue.get(1), env);
                    if (first.value instanceof Integer) {
                        String key = nvalue.get(1).stringValue();
                        int value = env.get(key).intValue();
                        env.put(key, new node(value - 1));
                        return new node();
                    }
                    else {
                        String key = nvalue.get(1).stringValue();
                        double value = env.get(key).doubleValue();
                        env.put(key, new node(value - 1.0));
                        return new node();
                    }
                    }
                case FLOOR: { // (floor X)
                    return new node(Math.floor(eval(nvalue.get(1), env).doubleValue()));}
                case CEIL: { // (ceil X)
                    return new node(Math.ceil(eval(nvalue.get(1), env).doubleValue()));}
                case LN: { // (ln X)
                    return new node(Math.log(eval(nvalue.get(1), env).doubleValue()));}
                case LOG10: { // (log10 X)
                    return new node(Math.log10(eval(nvalue.get(1), env).doubleValue()));}
                case RAND: { // (rand)
                    return new node(Math.random());}
                case SET: // (set SYMBOL VALUE)
                    {
                        env.put(nvalue.get(1).stringValue(),
                                eval(nvalue.get(2), env));
                        return new node();
                    }
                case EQEQ: { // (== X ..) short-circuit                    
                    node first = eval(nvalue.get(1), env);
                    if (first.value instanceof Integer) {
                        int firstv = first.intValue();                        
                        for (int i = 2; i < nvalue.size(); i++) {
                            if (eval(nvalue.get(i), env).intValue() != firstv) {return new node(false);}
                        }
                    }
                    else {
                        double firstv = first.doubleValue();                        
                        for (int i = 2; i < nvalue.size(); i++) {
                            if (eval(nvalue.get(i), env).doubleValue() != firstv) {return new node(false);}
                        }
                    }
                    return new node(true);}
                case NOTEQ: { // (!= X ..) short-circuit                    
                    node first = eval(nvalue.get(1), env);
                    if (first.value instanceof Integer) {
                        int firstv = first.intValue();                        
                        for (int i = 2; i < nvalue.size(); i++) {
                            if (eval(nvalue.get(i), env).intValue() == firstv) {return new node(false);}
                        }
                    }
                    else {
                        double firstv = first.doubleValue();                        
                        for (int i = 2; i < nvalue.size(); i++) {
                            if (eval(nvalue.get(i), env).doubleValue() == firstv) {return new node(false);}
                        }
                    }
                    return new node(true);}
                case LT: { // (< X Y)
                    node first = eval(nvalue.get(1), env);
                    node second = eval(nvalue.get(2), env);
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() < second.intValue());
                    }
                    else {
                        return new node(first.doubleValue() < second.doubleValue());
                    }}
                case GT: { // (> X Y)
                    node first = eval(nvalue.get(1), env);
                    node second = eval(nvalue.get(2), env);
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() > second.intValue());
                    }
                    else {
                        return new node(first.doubleValue() > second.doubleValue());
                    }}
                case LTE: { // (<= X Y)
                    node first = eval(nvalue.get(1), env);
                    node second = eval(nvalue.get(2), env);
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() <= second.intValue());
                    }
                    else {
                        return new node(first.doubleValue() <= second.doubleValue());
                    }}
                case GTE: { // (>= X Y)
                    node first = eval(nvalue.get(1), env);
                    node second = eval(nvalue.get(2), env);
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() >= second.intValue());
                    }
                    else {
                        return new node(first.doubleValue() >= second.doubleValue());
                    }}
                case ANDAND: { // (&& X ..) short-circuit
                    for (int i = 1; i < nvalue.size(); i++) {
                        if (!eval(nvalue.get(i), env).booleanValue()) {return new node(false);}
                    }
                    return new node(true);}
                case OROR: { // (|| X ..) short-circuit
                    for (int i = 1; i < nvalue.size(); i++) {
                        if (eval(nvalue.get(i), env).booleanValue()) {return new node(true);}
                    }
                    return new node(false);}
                case NOT: { // (! X)
                    return new node(!(eval(nvalue.get(1), env).booleanValue()));}
                case IF: { // (if CONDITION THEN_EXPR ELSE_EXPR)
                    node cond = nvalue.get(1);
                    if (eval(cond, env).booleanValue()) {
                        return eval(nvalue.get(2), env);
                    }
                    else {
                        return eval(nvalue.get(3), env);
                    }}
                case WHEN: { // (when CONDITION EXPR ..)
                    node cond = nvalue.get(1);
                    if (eval(cond, env).booleanValue()) {
                        int len = nvalue.size();
                        for (int i = 2; i < len - 1; i++) {
                            eval(nvalue.get(i), env);
                        }
                        return eval(nvalue.get(len - 1), env); // returns last EXPR
                    }
                    return new node();}
                case FOR: // (for SYMBOL START END STEP EXPR ..)
                    {
                        node start = eval(nvalue.get(2), env);
                        int len = nvalue.size();
                        if (start.value instanceof Integer) {                            
                            int last = eval(nvalue.get(3), env).intValue();
                            int step = eval(nvalue.get(4), env).intValue();
                            env.put(nvalue.get(1).stringValue(), start);
                            String key = nvalue.get(1).stringValue();
                            int a = env.get(key).intValue();
                            if (step >= 0) {
                                for (; a <= last; a += step) {
                                    env.put(key, new node(a));
                                    for (int i = 5; i < len; i++) {
                                        eval(nvalue.get(i), env);
                                    }
                                }
                            }
                            else {
                                for (; a >= last; a += step) {
                                    env.put(key, new node(a));
                                    for (int i = 5; i < len; i++) {
                                        eval(nvalue.get(i), env);
                                    }
                                }
                            }
                        }
                        else {
                            double last = eval(nvalue.get(3), env).intValue();
                            double step = eval(nvalue.get(4), env).intValue();
                            env.put(nvalue.get(1).stringValue(), start);
                            String key = nvalue.get(1).stringValue();
                            double a = env.get(nvalue.get(1).stringValue()).intValue();
                            if (step >= 0) {
                                for (; a <= last; a += step) {
                                    env.put(key, new node(a));
                                    for (int i = 5; i < len; i++) {
                                        eval(nvalue.get(i), env);
                                    }
                                }
                            }
                            else {
                                for (; a >= last; a += step) {
                                    env.put(key, new node(a));
                                    for (int i = 5; i < len; i++) {
                                        eval(nvalue.get(i), env);
                                    }
                                }
                            }
                        }
                        return new node();
                    }
                case WHILE: { // (while CONDITION EXPR ..)
                    node cond = nvalue.get(1);
                    int len = nvalue.size();
                    while (eval(cond, env).booleanValue()) {
                        for (int i = 2; i < len; i++) {
                            eval(nvalue.get(i), env);
                        }
                    }
                    return new node(); }
                case STRLEN: { // (strlen X)
                    return new node(eval(nvalue.get(1), env).stringValue().length());}
                case STRCAT: { // (strcat X ..)
                    int len = nvalue.size();
                    if (len <= 1) return new node("");
                    node first = eval(nvalue.get(1), env);
                    String acc = first.stringValue();
                    for (int i = 2; i < nvalue.size(); i++) {
                        acc += eval(nvalue.get(i), env).stringValue();
                    }
                    return new node(acc);}
                case CHAR_AT: { // (char-at X POSITION)
                    return new node((int) eval(nvalue.get(1), env).stringValue().charAt(eval(nvalue.get(2), env).intValue()));}
                case CHR: { // (chr X)                    
                    char[] temp = {0};
                    temp[0] = (char) eval(nvalue.get(1), env).intValue();
                    return new node(new String(temp));}
                case STRING: { // (string X)
                    return new node(eval(nvalue.get(1), env).stringValue());}
                case DOUBLE: { // (double X)
                    return new node(eval(nvalue.get(1), env).doubleValue());}
                case INT: { // (int X)
                    return new node(eval(nvalue.get(1), env).intValue());}
                case READ_STRING: { // (read-string X)
                    return new node(parse(eval(nvalue.get(1), env).stringValue()).get(0).value);}
                case TYPE: { // (type X)
                    return new node(eval(nvalue.get(1), env).type());}
                case EVAL: { // (eval X)
                    return new node(eval(eval(nvalue.get(1), env), env).value);}
                case QUOTE: { // (quote X)
                    return nvalue.get(1);}
                case FN: { // (fn (ARGUMENT ..) BODY) => evaluates to self
                    return new node(nvalue);}
                case LIST: { // (list X ..)
                    Vector<node> ret = new Vector<node>();
                    for (int i = 1; i < nvalue.size(); i++) {
                        ret.add(eval(nvalue.get(i), env));
                    }
                    return new node(ret);}
                case APPLY: { // (apply FUNC LIST)
                    Vector<node> expr = new Vector<node>();
                    node f = eval(nvalue.get(1), env);
                    expr.add(f);
                    Vector<node> lst = eval(nvalue.get(2), env).vectorValue();
                    for (int i = 0; i < lst.size(); i++) {
                        expr.add(lst.get(i));
                    }                    
                    return eval(new node(expr), env);
                }
                case MAP: { // (map FUNC LIST)
                    node f = eval(nvalue.get(1), env);
                    Vector<node> lst = eval(nvalue.get(2), env).vectorValue();
                    Vector<node> acc = new Vector<node>();
                    Vector<node> expr = new Vector<node>(); // (FUNC ITEM)                       
                    expr.add(f);
                    expr.add(null);
                    for (int i = 0; i < lst.size(); i++) {
                        expr.set(1, lst.get(i));
                        acc.add(eval(new node(expr), env));
                    }                    
                    return new node(acc);
                }
                case FILTER: { // (filter FUNC LIST)
                    node f = eval(nvalue.get(1), env);
                    Vector<node> lst = eval(nvalue.get(2), env).vectorValue();
                    Vector<node> acc = new Vector<node>();
                    Vector<node> expr = new Vector<node>(); // (FUNC ITEM)                    
                    expr.add(f);
                    expr.add(null);
                    for (int i = 0; i < lst.size(); i++) {
                        node item = lst.get(i);
                        expr.set(1, item);
                        node ret = eval(new node(expr), env);
                        if (ret.booleanValue()) acc.add(item);
                    }                    
                    return new node(acc);
                }
                case RANGE: { // (range START END STEP)
                    node start = eval(nvalue.get(1), env);                    
                    Vector<node> ret = new Vector<node>();
                    if (start.value instanceof Integer) {
                        int a = eval(nvalue.get(1), env).intValue();
                        int last = eval(nvalue.get(2), env).intValue();
                        int step = eval(nvalue.get(3), env).intValue();                        
                        if (step >= 0) {
                            for (; a <= last; a += step) {
                                ret.add(new node(a));}}
                        else {
                            for (; a >= last; a += step) {
                                ret.add(new node(a));}}
                    }
                    else {
                        double a = eval(nvalue.get(1), env).doubleValue();
                        double last = eval(nvalue.get(2), env).doubleValue();
                        double step = eval(nvalue.get(3), env).doubleValue();                        
                        if (step >= 0) {
                            for (; a <= last; a += step) {
                                ret.add(new node(a));}}
                        else {
                            for (; a >= last; a += step) {
                                ret.add(new node(a));}}
                    }
                    return new node(ret);
                }
                case NTH: { // (nth INDEX LIST)
                    int i = eval(nvalue.get(1), env).intValue();
                    Vector<node> lst = eval(nvalue.get(2), env).vectorValue();
                    return lst.get(i);}
                case LENGTH: { // (length LIST)                    
                    Vector<node> lst = eval(nvalue.get(1), env).vectorValue();
                    return new node(lst.size());}
                case BEGIN: { // (begin X ..)                    
                    int last = nvalue.size() - 1;
                    if (last <= 0) return new node();
                    for (int i = 1; i < last; i++) {
                        eval(nvalue.get(i), env);
                    }
                    return eval(nvalue.get(last), env);}
                case DOT: {
                    // Java interoperability
                    // (. CLASS METHOD ARGUMENT ..) ; Java method invocation
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nvalue.get(1).stringValue();
                        if (nvalue.get(1).isSymbol && !env.containsKey(className)) { // class's static method e.g. (. java.lang.Math floor 1.5)                            
                            cls = Class.forName(className);
                        } else { // object's method e.g. (. "abc" length)
                            obj = eval(nvalue.get(1), env).value;
                            cls = obj.getClass();
                        }                        
                        Class<?>[] parameterTypes = new Class<?>[nvalue.size() - 3];
                        Vector<Object> parameters = new Vector<Object>();                    
                        int last = nvalue.size() - 1;                        
                        for (int i = 3; i <= last; i++) {
                            Object param = eval(nvalue.get(i), env).value;
                            parameters.add(param);
                            Class<?> paramClass;
                            if (param instanceof Integer) paramClass = Integer.TYPE;
                            else if (param instanceof Double) paramClass = Double.TYPE;
                            else if (param instanceof Boolean) paramClass = Boolean.TYPE;
                            else paramClass = param.getClass();                            
                            parameterTypes[i - 3] = paramClass;
                        }
                        String methodName = nvalue.get(2).stringValue();
                        Method method = cls.getMethod(methodName, parameterTypes);
                        return new node(method.invoke(obj, parameters.toArray()));
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return new node();
                    }}
                case DOTGET: {
                    // Java interoperability
                    // (.get CLASS FIELD) ; get Java field
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nvalue.get(1).stringValue();
                        if (nvalue.get(1).isSymbol && !env.containsKey(className)) { // class's static field e.g. (.get java.lang.Math PI)                            
                            cls = Class.forName(className);
                        } else { // object's method
                            obj = eval(nvalue.get(1), env).value;
                            cls = obj.getClass();
                        }                        
                        String fieldName = nvalue.get(2).stringValue();
                        java.lang.reflect.Field field = cls.getField(fieldName);                        
                        return new node(field.get(cls));
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return new node();
                    }}
                case DOTSET: {
                    // Java interoperability
                    // (.set CLASS FIELD VALUE) ; set Java field
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nvalue.get(1).stringValue();
                        if (nvalue.get(1).isSymbol && !env.containsKey(className)) { // class's static field e.g. (.get java.lang.Math PI)                            
                            cls = Class.forName(className);
                        } else { // object's method
                            obj = eval(nvalue.get(1), env).value;
                            cls = obj.getClass();
                        }                        
                        String fieldName = nvalue.get(2).stringValue();
                        java.lang.reflect.Field field = cls.getField(fieldName);
                        Object value = eval(nvalue.get(3), env).value;
                        field.set(cls, value);
                        return new node();                        
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return new node();
                    }}
                case PR: // (pr X ..)
                    {                        
                        for (int i = 1; i < nvalue.size(); i++) {
                            if (i != 1) System.out.print(" ");
                            System.out.print(eval(nvalue.get(i), env).stringValue());
                        }
                        return new node();
                    }
                case PRN: // (prn X ..)
                    {
                        for (int i = 1; i < nvalue.size(); i++) {
                            if (i != 1) System.out.print(" ");
                            System.out.print(eval(nvalue.get(i), env).stringValue());
                        }
                        System.out.println();
                        return new node();
                    }
                case EXIT: {
                    System.out.println();
                    System.exit(0);
                    return new node(); }
                default: {
                    System.err.println("Not implemented function: [" + func.value.toString() + "]");
                    return new node();}
                } // end switch(found)
            } // end else
        }
        else {
            System.err.println("Unknown type");
            return new node();
        }
    }

    node eval_all(Vector<node> lst, Hashtable<String, node> env) {        
        int last = lst.size() - 1;
        if (last < 0) return new node();
        for (int i = 0; i < last; i++) {
            eval(lst.get(i), env);
        }
        return eval(lst.get(last), env);
    }
        
    node eval_string(String s) {
        return eval_all(parse(s), global_env);
    }
    
    void eval_print(String s) {
        System.out.println(eval_string(s).str_with_type());
    }
    
    static void prompt() {
        System.out.print("> ");
    }
    
    static void prompt2() {
        System.out.print("  ");
    }    
    
    // read-eval-print loop
    void repl() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String code = "";
        while (true) {
            try {
                if (code.length() == 0) prompt(); else prompt2();
                String line = br.readLine();
                if (line == null) { // EOF
                    eval_print(code);
                    break;
                }
                code += "\n" + line;
                if (line.length() == 0) {
                    eval_print(code);
                    code = "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
