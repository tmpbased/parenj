// (C) 2013 Kim, Taegyoon
// Paren language core
//
// # Changelog
// Version 1.5.1: Fixed recursion
//
// Version 1.5
//  Compile-time allocation of variables (No run-time symbol table lookup)
//  Much faster (10x).
//  (eval): Evaluate in global environment
// Version 1.5.5: Full support for long data type
// Version 1.6: Added macro
// Version 1.6.1: Faster execution (Hashtable -> HashMap, Vector -> ArrayList)
// Version 1.6.3: added defmacro ...
// Version 1.7: package paren
package paren;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.lang.Math;

public class paren {
	public static final String VERSION = "1.7";
    public paren() {
        init();
    }
    
    public static class node implements Cloneable {
        Object value;
        boolean isData;
        Class<?> clazz = null; // type hint
        
        node() {}
        node(Object value) {
            this.value = value;
        }
        protected node clone() {
        	node r = new node(this.value);
        	r.isData = this.isData;
        	r.clazz = this.clazz;
        	return r;
        }
        int intValue() {
            return ((Number)value).intValue();
        }
        double doubleValue() {
            return ((Number)value).doubleValue();
        }
        long longValue() {
            return ((Number)value).longValue();
        }
        boolean booleanValue() {
            return (Boolean) value;
        }
        String stringValue() {
            if (value == null) return "";
            return value.toString();
        }
        @SuppressWarnings("unchecked")
        ArrayList<node> arrayListValue() {
            return (ArrayList<node>)value;
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
    
    // frequently used constants
    static final node node_true = new node(true);
    static final node node_false = new node(false);
    static final node node_0 = new node(0);
    static final node node_1 = new node(1);

    static class symbol {
        String name;
        symbol(String name) {this.name = name;}
        public String toString() {
            return name;
        }
    }
    
    static class fn { // anonymous function
        ArrayList<node> def; // definition
        environment env;        
        fn(ArrayList<node> def, environment env) {
            this.def = def;
            this.env = env;
        }
        public String toString() {
            return def.toString();
        }
    }
    
    static class environment {
        HashMap<String, node> env = new HashMap<String, node>();
        environment outer;
        environment() {this.outer = null;}
        environment(environment outer) {this.outer = outer;}
        node get(String name) {
            node found = env.get(name);            
            if (found != null) {
                return found;
            }
            else {
                if (outer != null) {
                    return outer.get(name);
                }
                else {
                    return null;
                }
            }            
        }
    }
    
    private static class tokenizer {
        private ArrayList<String> ret = new ArrayList<String>();
        private String acc = ""; // accumulator
        private String s;
        public int unclosed = 0;
        
        public tokenizer(String s) {
            this.s = s;
        }
        
        private void emit() {
            if (acc.length() > 0) {ret.add(acc); acc = "";}
        }
        
        public ArrayList<String> tokenize() {
            int last = s.length() - 1;
            for (int pos=0; pos <= last; pos++) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    emit();
                }
                else if (c == ';') { // end-of-line comment
                    emit();
                    do pos++; while (pos <= last && s.charAt(pos) != '\n');
                }
                else if (c == '"') { // beginning of string
                	unclosed++;
                    emit();
                    acc += '"';
                    pos++;
                    while (pos <= last) {
                        if (s.charAt(pos) == '"') {unclosed--; break;}
                        if (s.charAt(pos) == '\\') { // escape
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
                    emit();
                }
                else if (c == '(') {
                	unclosed++;
                    emit();
                    acc += c;
                    emit();
                }
                else if (c == ')') {
                	unclosed--;
                    emit();
                    acc += c;
                    emit();
                }
                else {
                    acc += c;
                }
            }
            emit();
            return ret;            
        }
    }
    
    public static ArrayList<String> tokenize(String s) {
        return new tokenizer(s).tokenize();
    }
    
    private static class parser {        
        private int pos = 0;
        private ArrayList<String> tokens;
        public parser(ArrayList<String> tokens) {
            this.tokens = tokens;
        }
        public ArrayList<node> parse() {
            ArrayList<node> ret = new ArrayList<node>();
            int last = tokens.size() - 1;
            for (;pos <= last; pos++) {
                String tok = tokens.get(pos);
                if (tok.charAt(0) == '"') { // double-quoted string
                    ret.add(new node(tok.substring(1)));
                }
                else if (tok.equals("(")) { // list
                    pos++;
                    ret.add(new node(parse()));
                }
                else if (tok.equals(")")) { // end of list
                    break;
                }
                else if (Character.isDigit(tok.charAt(0)) || tok.charAt(0) == '-' && tok.length() >= 2 && Character.isDigit(tok.charAt(1))) { // number
                    if (tok.indexOf('.') != -1 || tok.indexOf('e') != -1) { // double
                        ret.add(new node(Double.parseDouble(tok)));
                    }
                    else if (tok.endsWith("L") || tok.endsWith("l")) { // long
                    	ret.add(new node(Long.parseLong(tok.substring(0, tok.length() - 1))));
                    }
                    else {
                        ret.add(new node(Integer.parseInt(tok)));
                    }
                }
                else { // symbol
                    ret.add(new node(new symbol(tok)));
                }
            }
            return ret;
        }        
    }
    
    ArrayList<node> parse(String s) {
        return new parser(tokenize(s)).parse();
    }
    
    enum builtin {
        PLUS, MINUS, MUL, DIV, CARET, PERCENT, SQRT, INC, DEC, PLUSPLUS, MINUSMINUS, FLOOR, CEIL, LN, LOG10, E, PI, RAND,
        EQ, EQEQ, NOTEQ, LT, GT, LTE, GTE, ANDAND, OROR, NOT,
        IF, WHEN, FOR, WHILE,
        STRLEN, STRCAT, CHAR_AT, CHR,
        INT, DOUBLE, STRING, READ_STRING, TYPE, SET,
        EVAL, QUOTE, FN, LIST, APPLY, FOLD, MAP, FILTER, RANGE, NTH, LENGTH, BEGIN, DOT, DOTGET, DOTSET, NEW,
        PR, PRN, EXIT, SYSTEM, CONS, LONG, NULLP, CAST, DEFMACRO
    }
    
    environment global_env = new environment(); // variables. compile-time
    
    void print_collection(Collection<String> coll) {
        int i = 0;
        for (String key : new TreeSet<String>(coll)) {
            System.out.print(" " + key);
            i++;
            if (i % 10 == 0) System.out.println();            
        }
        System.out.println();
    }

    public void print_logo() {
        System.out.println(
            "Parenj " + VERSION + " (C) 2013 Kim, Taegyoon");
        System.out.println(
            "Predefined Symbols:");
        print_collection(global_env.env.keySet());
        System.out.println(
                "Macros:");
        print_collection(macros.keySet());

        System.out.println(
            "Etc.:\n" +
            " (list) \"string\" ; end-of-line comment");
    }    

    void init() {
        global_env.env.put("true", node_true);
        global_env.env.put("false", node_false);
        global_env.env.put("E", new node(2.71828182845904523536));
        global_env.env.put("PI", new node(3.14159265358979323846));
        global_env.env.put("null", new node());

        global_env.env.put("+", new node(builtin.PLUS));
        global_env.env.put("-", new node(builtin.MINUS));
        global_env.env.put("*", new node(builtin.MUL));
        global_env.env.put("/", new node(builtin.DIV));
        global_env.env.put("^", new node(builtin.CARET));
        global_env.env.put("%", new node(builtin.PERCENT));
        global_env.env.put("sqrt", new node(builtin.SQRT));
        global_env.env.put("inc", new node(builtin.INC));
        global_env.env.put("dec", new node(builtin.DEC));
        global_env.env.put("++", new node(builtin.PLUSPLUS));
        global_env.env.put("--", new node(builtin.MINUSMINUS));
        global_env.env.put("floor", new node(builtin.FLOOR));
        global_env.env.put("ceil", new node(builtin.CEIL));
        global_env.env.put("ln", new node(builtin.LN));
        global_env.env.put("log10", new node(builtin.LOG10));
        global_env.env.put("rand", new node(builtin.RAND));
        global_env.env.put("=", new node(builtin.EQ));
        global_env.env.put("==", new node(builtin.EQEQ));
        global_env.env.put("!=", new node(builtin.NOTEQ));
        global_env.env.put("<", new node(builtin.LT));
        global_env.env.put(">", new node(builtin.GT));
        global_env.env.put("<=", new node(builtin.LTE));
        global_env.env.put(">=", new node(builtin.GTE));
        global_env.env.put("&&", new node(builtin.ANDAND));
        global_env.env.put("||", new node(builtin.OROR));
        global_env.env.put("!", new node(builtin.NOT));
        global_env.env.put("if", new node(builtin.IF));
        global_env.env.put("when", new node(builtin.WHEN));
        global_env.env.put("for", new node(builtin.FOR));
        global_env.env.put("while", new node(builtin.WHILE));
        global_env.env.put("strlen", new node(builtin.STRLEN));
        global_env.env.put("strcat", new node(builtin.STRCAT));
        global_env.env.put("char-at", new node(builtin.CHAR_AT));
        global_env.env.put("chr", new node(builtin.CHR));
        global_env.env.put("int", new node(builtin.INT));
        global_env.env.put("double", new node(builtin.DOUBLE));
        global_env.env.put("string", new node(builtin.STRING));
        global_env.env.put("read-string", new node(builtin.READ_STRING));
        global_env.env.put("type", new node(builtin.TYPE));
        global_env.env.put("eval", new node(builtin.EVAL));
        global_env.env.put("quote", new node(builtin.QUOTE));
        global_env.env.put("fn", new node(builtin.FN));
        global_env.env.put("list", new node(builtin.LIST));
        global_env.env.put("apply", new node(builtin.APPLY));
        global_env.env.put("fold", new node(builtin.FOLD));
        global_env.env.put("map", new node(builtin.MAP));
        global_env.env.put("filter", new node(builtin.FILTER));
        global_env.env.put("range", new node(builtin.RANGE));
        global_env.env.put("nth", new node(builtin.NTH));
        global_env.env.put("length", new node(builtin.LENGTH));
        global_env.env.put("begin", new node(builtin.BEGIN));
        global_env.env.put(".", new node(builtin.DOT));
        global_env.env.put(".get", new node(builtin.DOTGET));
        global_env.env.put(".set", new node(builtin.DOTSET));
        global_env.env.put("new", new node(builtin.NEW));
        global_env.env.put("set", new node(builtin.SET));
        global_env.env.put("pr", new node(builtin.PR));
        global_env.env.put("prn", new node(builtin.PRN));
        global_env.env.put("exit", new node(builtin.EXIT));
        global_env.env.put("system", new node(builtin.SYSTEM));
        global_env.env.put("cons", new node(builtin.CONS));
        global_env.env.put("long", new node(builtin.LONG));
        global_env.env.put("null?", new node(builtin.NULLP));
        global_env.env.put("cast", new node(builtin.CAST));
        global_env.env.put("defmacro", new node(builtin.DEFMACRO));

        eval_string("(defmacro setfn (name ...) (set name (fn ...)))");
		eval_string("(defmacro defn (...) (setfn ...))");        
    }
    
    HashMap<String, node[]> macros = new HashMap<>();
    
	node apply_macro(node body, HashMap<String, node> vars) {
		if (body.value instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<node> bvec = (ArrayList<node>) body.value;
			ArrayList<node> ret = new ArrayList<>();
			for (int i = 0; i < bvec.size(); i++) {
				node b = bvec.get(i);
				if (b.stringValue().equals("...")) {
					ret.addAll(vars.get(b.stringValue()).arrayListValue());
				} else ret.add(apply_macro(bvec.get(i), vars));
			}
			return new node(ret);
		} else {
			String bstr = body.stringValue();
			if (vars.containsKey(bstr)) return vars.get(bstr); else return body;
		}
	}

	node macroexpand(node n) {
		ArrayList<node> nArrayList = n.arrayListValue();
		if (macros.containsKey(nArrayList.get(0).stringValue())) {
			node[] macro = macros.get(nArrayList.get(0).stringValue());
			HashMap<String, node> macrovars = new HashMap<>();
			ArrayList<node> argsyms = macro[0].arrayListValue();
			for (int i = 0; i < argsyms.size(); i++) {
				String argsym = argsyms.get(i).stringValue();
				if (argsym.equals("...")) {
					node n2 = new node(new ArrayList<node>());
					macrovars.put(argsym, n2);
					ArrayList<node> ellipsis = n2.arrayListValue();
					for (int i2 = i + 1; i2 < nArrayList.size(); i2++)
						ellipsis.add(nArrayList.get(i2));
					break;
				} else {
					macrovars.put(argsyms.get(i).stringValue(), nArrayList.get(i+1));
				}
			}
			return apply_macro(macro[1], macrovars);
		} else
			return n;
	}

    node compile(node n, environment env) {    	
        if (n.value instanceof symbol) {
            symbol sym = (symbol) n.value;
            node found = env.get(sym.name);
            if (found != null) { // variable
            	found.isData = true;
                return found;
            }
            else {
                return n;
//                System.err.println("Unknown variable: [" + sym.name + "]");
//                return new node();
            }
        }
        else if (n.value instanceof ArrayList) { // function (FUNCTION ARGUMENT ..)
        	if (n.isData) return n;
            ArrayList<node> nArrayList = n.arrayListValue();                
            if (nArrayList.size() == 0) return new node();
            node func = compile(nArrayList.get(0), env);
            builtin foundBuiltin;
            if (func.value instanceof builtin) { // special forms regarding symbol
                foundBuiltin = (builtin) func.value;
                switch(foundBuiltin) {
                case SET: { // (set SYMBOL VALUE)
                    node symbol;
                    String name = ((symbol) nArrayList.get(1).value).name;
                    node found = env.get(name);
                    if (found != null) { // variable
                        symbol = found;
                    }
                    else {
                        symbol = new node();
                        env.env.put(name, symbol);
                    }                    
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);
                    r.add(symbol);
                    r.add(compile(nArrayList.get(2), env));
                    return new node(r);}
                case FOR: // (for SYMBOL START END STEP EXPR ..)
                    {
                        node symbol;
                        String name = ((symbol) nArrayList.get(1).value).name;
                        node found = env.get(name);
                        if (found != null) { // variable
                            symbol = found;
                        }
                        else {
                            symbol = new node();
                            env.env.put(name, symbol);
                        }
                        ArrayList<node> r = new ArrayList<node>();
                        r.add(func);
                        r.add(symbol);
                        int len = nArrayList.size();
                        for (int i = 2; i < len; i++) {
                            r.add(compile(nArrayList.get(i), env));
                        }
                        return new node(r);
                    }
                case FN: {
                    // anonymous function. lexical scoping
                    // (fn (ARGUMENT ..) BODY ..)                    
                    environment local_env = new environment(env);
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);
                    
                    for (int i=1; i<nArrayList.size(); i++) {
                        r.add(nArrayList.get(i));
                    }
                    return new node(new fn(r, local_env));                
                }
                case QUOTE: {
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);                    
                    int len = nArrayList.size();
                    for (int i = 1; i < len; i++) {
                        r.add(nArrayList.get(i));
                    }
                    return new node(r);                    
                }
                case DOT: {
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);                    
                    int len = nArrayList.size();
                    for (int i = 1; i < len; i++) {
                        if (i == 2) {
                            r.add(nArrayList.get(i));
                        } else {
                            r.add(compile(nArrayList.get(i), env));
                        }
                    }
                    return new node(r);                     
                }
                case DOTGET:
                case DOTSET: {
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);                    
                    int len = nArrayList.size();
                    for (int i = 1; i < len; i++) {
                        if (i <= 2) {
                            r.add(nArrayList.get(i));
                        } else {
                            r.add(compile(nArrayList.get(i), env));
                        }
                    }
                    return new node(r);                    
                }
                case NEW: {
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);                    
                    int len = nArrayList.size();
                    for (int i = 1; i < len; i++) {
                        if (i == 1) {
                            r.add(nArrayList.get(i));
                        } else {
                            r.add(compile(nArrayList.get(i), env));
                        }
                    }
                    return new node(r);
                }
                case DEFMACRO: { // (defmacro add (a b) (+ a b)) ; define macro
                	macros.put(nArrayList.get(1).stringValue(), new node[]{nArrayList.get(2), nArrayList.get(3)});
                    return new node();                	
                }
                default: {
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);                    
                    int len = nArrayList.size();
                    for (int i = 1; i < len; i++) {
                        r.add(compile(nArrayList.get(i), env));
                    }
                    return new node(r);}
                } // end switch(found)                
            }
            else {
            	if (macros.containsKey(nArrayList.get(0).stringValue())) { // compile macro
                    return compile(macroexpand(n), env);
            	} else {            	
	                ArrayList<node> r = new ArrayList<node>();
	                for (node n2: nArrayList) {
	                    r.add(compile(n2, env));
	                }
	                return new node(r);
            	}
            }
        }
        else {
            return n;
        }
    }    
    
    node eval(node n) {    	
        if (n.value instanceof ArrayList) { // function (FUNCTION ARGUMENT ..)
        	if (n.isData) return n.clone();;
            ArrayList<node> nArrayList = n.arrayListValue();                
            if (nArrayList.size() == 0) return new node();
            node func = eval(nArrayList.get(0));
            builtin foundBuiltin;
            if (func.value instanceof builtin) {
                foundBuiltin = (builtin) func.value;
                switch(foundBuiltin) {
                case PLUS: // (+ X ..)
                    {
                        int len = nArrayList.size();
                        if (len <= 1) return node_0;
                        node first = eval(nArrayList.get(1));
                        if (first.value instanceof Integer) {
                            int acc = first.intValue();
                            for (int i = 2; i < len; i++) {
                                acc += eval(nArrayList.get(i)).intValue();
                            }
                            return new node(acc);
                        }
                        else if (first.value instanceof Long) {
                        	long acc = first.longValue();
                            for (int i = 2; i < len; i++) {
                                acc += eval(nArrayList.get(i)).longValue();
                            }
                            return new node(acc);
                        }
                        else {
                            double acc = first.doubleValue();
                            for (int i = 2; i < len; i++) {
                                acc += eval(nArrayList.get(i)).doubleValue();
                            }
                            return new node(acc);
                        }
                    }
                case MINUS: // (- X ..)
                    {
                        int len = nArrayList.size();
                        if (len <= 1) return node_0;
                        node first = eval(nArrayList.get(1));
                        if (first.value instanceof Integer) {
                            int acc = first.intValue();
                            for (int i = 2; i < len; i++) {
                                acc -= eval(nArrayList.get(i)).intValue();
                            }
                            return new node(acc);
                        }
                        else if (first.value instanceof Long) {
                        	long acc = first.longValue();
                            for (int i = 2; i < len; i++) {
                                acc -= eval(nArrayList.get(i)).longValue();
                            }
                            return new node(acc);
                        }
                        else {
                            double acc = first.doubleValue();
                            for (int i = 2; i < len; i++) {
                                acc -= eval(nArrayList.get(i)).doubleValue();
                            }
                            return new node(acc);
                        }
                    }
                case MUL: // (* X ..)
                {
                    int len = nArrayList.size();
                    if (len <= 1) return node_1;
                    node first = eval(nArrayList.get(1));
                    if (first.value instanceof Integer) {
                        int acc = first.intValue();
                        for (int i = 2; i < len; i++) {
                            acc *= eval(nArrayList.get(i)).intValue();
                        }
                        return new node(acc);
                    }
                    else if (first.value instanceof Long) {
                    	long acc = first.longValue();
                        for (int i = 2; i < len; i++) {
                            acc *= eval(nArrayList.get(i)).longValue();
                        }
                        return new node(acc);
                    }
                    else {
                        double acc = first.doubleValue();
                        for (int i = 2; i < len; i++) {
                            acc *= eval(nArrayList.get(i)).doubleValue();
                        }
                        return new node(acc);
                    }
                }
                case DIV: // (/ X ..)
                {
                    int len = nArrayList.size();
                    if (len <= 1) return node_1;
                    node first = eval(nArrayList.get(1));
                    if (first.value instanceof Integer) {
                        int acc = first.intValue();
                        for (int i = 2; i < len; i++) {
                            acc /= eval(nArrayList.get(i)).intValue();
                        }
                        return new node(acc);
                    }
                    else if (first.value instanceof Long) {
                    	long acc = first.longValue();
                        for (int i = 2; i < len; i++) {
                            acc /= eval(nArrayList.get(i)).longValue();
                        }
                        return new node(acc);
                    }
                    else {
                        double acc = first.doubleValue();
                        for (int i = 2; i < len; i++) {
                            acc /= eval(nArrayList.get(i)).doubleValue();
                        }
                        return new node(acc);
                    }
                }
                case CARET: { // (^ BASE EXPONENT)
                    return new node(Math.pow(
                            eval(nArrayList.get(1)).doubleValue(),
                            eval(nArrayList.get(2)).doubleValue()));}
                case PERCENT: { // (% DIVIDEND DIVISOR)
                    return new node(eval(nArrayList.get(1)).intValue() % eval(nArrayList.get(2)).intValue());}
                case SQRT: { // (sqrt X)
                    return new node(Math.sqrt(eval(nArrayList.get(1)).doubleValue()));}
                case INC: { // (inc X)
                    int len = nArrayList.size();
                    if (len <= 1) return node_0;
                    node first = eval(nArrayList.get(1));
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() + 1);
                    }
                    else if (first.value instanceof Long) {
                    	return new node(first.longValue() + 1);
                    }
                    else {
                        return new node(first.doubleValue() + 1.0);
                    }
                }
                case DEC: { // (dec X)
                    int len = nArrayList.size();
                    if (len <= 1) return node_0;
                    node first = eval(nArrayList.get(1));
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() - 1);
                    }
                    else if (first.value instanceof Long) {
                    	return new node(first.longValue() - 1);
                    }
                    else {
                        return new node(first.doubleValue() - 1.0);
                    }
                }
                case PLUSPLUS: { // (++ X)
                    int len = nArrayList.size();
                    if (len <= 1) return node_0;
                    node n2 = nArrayList.get(1);
                    if (n2.value instanceof Integer) {
                        n2.value = n2.intValue() + 1;
                    }
                    else if (n2.value instanceof Long) {
                    	n2.value = n2.longValue() + 1;
                    }
                    else {
                        n2.value = n2.doubleValue() + 1.0;
                    }
                    return n2;
                }
                case MINUSMINUS: { // (-- X)
                    int len = nArrayList.size();
                    if (len <= 1) return node_0;                    
                    node n2 = nArrayList.get(1);
                    if (n2.value instanceof Integer) {
                        n2.value = n2.intValue() - 1;
                    }
                    else if (n2.value instanceof Long) {
                    	n2.value = n2.longValue() - 1;
                    }
                    else {
                        n2.value = (n2.doubleValue() - 1.0);
                    }
                    return n2;
                }
                case FLOOR: { // (floor X)
                    return new node(Math.floor(eval(nArrayList.get(1)).doubleValue()));}
                case CEIL: { // (ceil X)
                    return new node(Math.ceil(eval(nArrayList.get(1)).doubleValue()));}
                case LN: { // (ln X)
                    return new node(Math.log(eval(nArrayList.get(1)).doubleValue()));}
                case LOG10: { // (log10 X)
                    return new node(Math.log10(eval(nArrayList.get(1)).doubleValue()));}
                case RAND: { // (rand)
                    return new node(Math.random());}
                case SET: { // (set SYMBOL VALUE)
                    node n2 = nArrayList.get(1); 
                    n2.value = eval(nArrayList.get(2)).value; 
                    return n2;}
                case EQ: { // (= X ..) short-circuit, Object.equals()
                    node first = eval(nArrayList.get(1));
                    Object firstv = first.value;
                    for (int i = 2; i < nArrayList.size(); i++) {
                        if (!eval(nArrayList.get(i)).value.equals(firstv)) {return node_false;}
                    }
                    return node_true;}
                case EQEQ: { // (== X ..) short-circuit                    
                    node first = eval(nArrayList.get(1));
                    if (first.value instanceof Integer) {
                        int firstv = first.intValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i)).intValue() != firstv) {return node_false;}
                        }
                    }
                    else if (first.value instanceof Long) {
                	   long firstv = first.longValue();                        
                       for (int i = 2; i < nArrayList.size(); i++) {
                           if (eval(nArrayList.get(i)).longValue() != firstv) {return node_false;}
                       }
                    }
                    else {
                        double firstv = first.doubleValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i)).doubleValue() != firstv) {return node_false;}
                        }
                    }
                    return node_true;}
                case NOTEQ: { // (!= X ..) short-circuit                    
                    node first = eval(nArrayList.get(1));
                    if (first.value instanceof Integer) {
                        int firstv = first.intValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i)).intValue() == firstv) {return node_false;}
                        }
                    }
                    else if (first.value instanceof Long) {
                	   long firstv = first.longValue();                        
                       for (int i = 2; i < nArrayList.size(); i++) {
                           if (eval(nArrayList.get(i)).longValue() == firstv) {return node_false;}
                       }
                    }
                    else {
                        double firstv = first.doubleValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i)).doubleValue() == firstv) {return node_false;}
                        }
                    }
                    return node_true;}
                case LT: { // (< X Y)
                    node first = eval(nArrayList.get(1));
                    node second = eval(nArrayList.get(2));
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() < second.intValue());
                    }
                    else if (first.value instanceof Long) {
                    	return new node(first.longValue() < second.longValue());
                    }
                    else {
                        return new node(first.doubleValue() < second.doubleValue());
                    }}
                case GT: { // (> X Y)
                    node first = eval(nArrayList.get(1));
                    node second = eval(nArrayList.get(2));
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() > second.intValue());
                    }
                    else if (first.value instanceof Long) {
                    	return new node(first.longValue() > second.longValue());
                    }
                    else {
                        return new node(first.doubleValue() > second.doubleValue());
                    }}
                case LTE: { // (<= X Y)
                    node first = eval(nArrayList.get(1));
                    node second = eval(nArrayList.get(2));
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() <= second.intValue());
                    }
                    else if (first.value instanceof Long) {
                    	return new node(first.longValue() <= second.longValue());
                    }
                    else {
                        return new node(first.doubleValue() <= second.doubleValue());
                    }}
                case GTE: { // (>= X Y)
                    node first = eval(nArrayList.get(1));
                    node second = eval(nArrayList.get(2));
                    if (first.value instanceof Integer) {
                        return new node(first.intValue() >= second.intValue());
                    }
                    else if (first.value instanceof Long) {
                    	return new node(first.longValue() >= second.longValue());
                    }
                    else {
                        return new node(first.doubleValue() >= second.doubleValue());
                    }}
                case ANDAND: { // (&& X ..) short-circuit
                    for (int i = 1; i < nArrayList.size(); i++) {
                        if (!eval(nArrayList.get(i)).booleanValue()) {return node_false;}
                    }
                    return node_true;}
                case OROR: { // (|| X ..) short-circuit
                    for (int i = 1; i < nArrayList.size(); i++) {
                        if (eval(nArrayList.get(i)).booleanValue()) {return node_true;}
                    }
                    return node_false;}
                case NOT: { // (! X)
                    return new node(!(eval(nArrayList.get(1)).booleanValue()));}
                case IF: { // (if CONDITION THEN_EXPR ELSE_EXPR)
                    node cond = nArrayList.get(1);
                    if (eval(cond).booleanValue()) {
                        return eval(nArrayList.get(2));
                    }
                    else {
                        return eval(nArrayList.get(3));
                    }}
                case WHEN: { // (when CONDITION EXPR ..)
                    node cond = nArrayList.get(1);
                    if (eval(cond).booleanValue()) {
                        int len = nArrayList.size();
                        for (int i = 2; i < len - 1; i++) {
                            eval(nArrayList.get(i));
                        }
                        return eval(nArrayList.get(len - 1)); // returns last EXPR
                    }
                    return new node();}
                case FOR: // (for SYMBOL START END STEP EXPR ..)
                    {
                        node start = eval(nArrayList.get(2));
                        int len = nArrayList.size();
                        if (start.value instanceof Integer) {                            
                            int last = eval(nArrayList.get(3)).intValue();
                            int step = eval(nArrayList.get(4)).intValue();                            
                            int a = start.intValue();
                            node na = nArrayList.get(1);
                            if (step >= 0) {
                                for (; a <= last; a += step) {
                                    na.value = a;
                                    for (int i = 5; i < len; i++) {
                                        eval(nArrayList.get(i));
                                    }
                                }
                            }
                            else {
                                for (; a >= last; a += step) {
                                    na.value = a;
                                    for (int i = 5; i < len; i++) {
                                        eval(nArrayList.get(i));
                                    }
                                }
                            }
                        }
                        else if (start.value instanceof Long) {
                            long last = eval(nArrayList.get(3)).longValue();
                            long step = eval(nArrayList.get(4)).longValue();                            
                            long a = start.longValue();
                            node na = nArrayList.get(1);
                            if (step >= 0) {
                                for (; a <= last; a += step) {
                                    na.value = a;
                                    for (int i = 5; i < len; i++) {
                                        eval(nArrayList.get(i));
                                    }
                                }
                            }
                            else {
                                for (; a >= last; a += step) {
                                    na.value = a;
                                    for (int i = 5; i < len; i++) {
                                        eval(nArrayList.get(i));
                                    }
                                }
                            }
                        }
                        else {
                            double last = eval(nArrayList.get(3)).doubleValue();
                            double step = eval(nArrayList.get(4)).doubleValue();                            
                            double a = start.doubleValue();                            
                            node na = nArrayList.get(1);
                            if (step >= 0) {
                                for (; a <= last; a += step) {
                                    na.value = a;
                                    for (int i = 5; i < len; i++) {
                                        eval(nArrayList.get(i));
                                    }
                                }
                            }
                            else {
                                for (; a >= last; a += step) {
                                    na.value = a;
                                    for (int i = 5; i < len; i++) {
                                        eval(nArrayList.get(i));
                                    }
                                }
                            }
                        }
                        return new node();
                    }
                case WHILE: { // (while CONDITION EXPR ..)
                    node cond = nArrayList.get(1);
                    int len = nArrayList.size();
                    while (eval(cond).booleanValue()) {
                        for (int i = 2; i < len; i++) {
                            eval(nArrayList.get(i));
                        }
                    }
                    return new node(); }
                case STRLEN: { // (strlen X)
                    return new node(eval(nArrayList.get(1)).stringValue().length());}
                case STRCAT: { // (strcat X ..)
                    int len = nArrayList.size();
                    if (len <= 1) return new node("");
                    node first = eval(nArrayList.get(1));
                    String acc = first.stringValue();
                    for (int i = 2; i < nArrayList.size(); i++) {
                        acc += eval(nArrayList.get(i)).stringValue();
                    }
                    return new node(acc);}
                case CHAR_AT: { // (char-at X POSITION)
                    return new node((int) eval(nArrayList.get(1)).stringValue().charAt(eval(nArrayList.get(2)).intValue()));}
                case CHR: { // (chr X)                    
                    char[] temp = {0};
                    temp[0] = (char) eval(nArrayList.get(1)).intValue();
                    return new node(new String(temp));}
                case STRING: { // (string X)
                    return new node(eval(nArrayList.get(1)).stringValue());}
                case DOUBLE: { // (double X)
                    return new node(eval(nArrayList.get(1)).doubleValue());}
                case INT: { // (int X)
                    return new node(eval(nArrayList.get(1)).intValue());}
                case LONG: { // (long X)
                    return new node(eval(nArrayList.get(1)).longValue());}                
                case READ_STRING: { // (read-string X)
                    return new node(parse(eval(nArrayList.get(1)).stringValue()).get(0).value);}
                case TYPE: { // (type X)
                    return new node(eval(nArrayList.get(1)).type());}
                case EVAL: { // (eval X)
                    return new node(eval(compile(eval(nArrayList.get(1)), global_env)).value);}
                case QUOTE: { // (quote X)
                    return nArrayList.get(1);}
                case FN: { // (fn (ARGUMENT ..) BODY) => lexical closure
                    return n;}                    
                case LIST: { // (list X ..)
                    ArrayList<node> ret = new ArrayList<node>();
                    for (int i = 1; i < nArrayList.size(); i++) {
                        ret.add(eval(nArrayList.get(i)));
                    }
                    return new node(ret);}
                case APPLY: { // (apply FUNC LIST)
                    ArrayList<node> expr = new ArrayList<node>();
                    node f = eval(nArrayList.get(1));
                    expr.add(f);
                    ArrayList<node> lst = eval(nArrayList.get(2)).arrayListValue();
                    for (int i = 0; i < lst.size(); i++) {
                        expr.add(lst.get(i));
                    }                    
                    return eval(new node(expr));
                }
                case FOLD: { // (fold FUNC LIST)
                    node f = eval(nArrayList.get(1));
                    ArrayList<node> lst = eval(nArrayList.get(2)).arrayListValue();
                    node acc = eval(lst.get(0));
                    ArrayList<node> expr = new ArrayList<node>(); // (FUNC ITEM)
                    expr.add(f);
                    expr.add(null); // first argument
                    expr.add(null); // second argument
                    for (int i = 1; i < lst.size(); i++) {
                        expr.set(1, acc);
                        expr.set(2, lst.get(i));
                        acc = eval(new node(expr));
                    }
                    return acc;
                }
                case MAP: { // (map FUNC LIST)
                    node f = eval(nArrayList.get(1));
                    ArrayList<node> lst = eval(nArrayList.get(2)).arrayListValue();
                    ArrayList<node> acc = new ArrayList<node>();
                    ArrayList<node> expr = new ArrayList<node>(); // (FUNC ITEM)                       
                    expr.add(f);
                    expr.add(null);
                    for (int i = 0; i < lst.size(); i++) {
                        expr.set(1, lst.get(i));
                        acc.add(eval(new node(expr)));
                    }                    
                    return new node(acc);
                }
                case FILTER: { // (filter FUNC LIST)
                    node f = eval(nArrayList.get(1));
                    ArrayList<node> lst = eval(nArrayList.get(2)).arrayListValue();
                    ArrayList<node> acc = new ArrayList<node>();
                    ArrayList<node> expr = new ArrayList<node>(); // (FUNC ITEM)                    
                    expr.add(f);
                    expr.add(null);
                    for (int i = 0; i < lst.size(); i++) {
                        node item = lst.get(i);
                        expr.set(1, item);
                        node ret = eval(new node(expr));
                        if (ret.booleanValue()) acc.add(item);
                    }                    
                    return new node(acc);
                }
                case RANGE: { // (range START END STEP)
                    node start = eval(nArrayList.get(1));                    
                    ArrayList<node> ret = new ArrayList<node>();
                    if (start.value instanceof Integer) {
                        int a = eval(nArrayList.get(1)).intValue();
                        int last = eval(nArrayList.get(2)).intValue();
                        int step = eval(nArrayList.get(3)).intValue();                        
                        if (step >= 0) {
                            for (; a <= last; a += step) {
                                ret.add(new node(a));}}
                        else {
                            for (; a >= last; a += step) {
                                ret.add(new node(a));}}
                    }
                    else if (start.value instanceof Long) {
                        long a = eval(nArrayList.get(1)).longValue();
                        long last = eval(nArrayList.get(2)).longValue();
                        long step = eval(nArrayList.get(3)).longValue();                        
                        if (step >= 0) {
                            for (; a <= last; a += step) {
                                ret.add(new node(a));}}
                        else {
                            for (; a >= last; a += step) {
                                ret.add(new node(a));}}
                    }
                    else {
                        double a = eval(nArrayList.get(1)).doubleValue();
                        double last = eval(nArrayList.get(2)).doubleValue();
                        double step = eval(nArrayList.get(3)).doubleValue();                        
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
                    int i = eval(nArrayList.get(1)).intValue();
                    ArrayList<node> lst = eval(nArrayList.get(2)).arrayListValue();
                    return lst.get(i);}
                case LENGTH: { // (length LIST)                    
                    ArrayList<node> lst = eval(nArrayList.get(1)).arrayListValue();
                    return new node(lst.size());}
                case BEGIN: { // (begin X ..)                    
                    int last = nArrayList.size() - 1;
                    if (last <= 0) return new node();
                    for (int i = 1; i < last; i++) {
                        eval(nArrayList.get(i));
                    }
                    return eval(nArrayList.get(last));}
                case DOT: {
                    // Java interoperability
                    // (. CLASS METHOD ARGUMENT ..) ; Java method invocation
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nArrayList.get(1).stringValue();
                        if (nArrayList.get(1).value instanceof symbol) { // class's static method e.g. (. java.lang.Math floor 1.5)                            
                            cls = Class.forName(className);
                        } else { // object's method e.g. (. "abc" length)
                            obj = eval(nArrayList.get(1)).value;
                            cls = obj.getClass();
                        }                        
                        Class<?>[] parameterTypes = new Class<?>[nArrayList.size() - 3];
                        ArrayList<Object> parameters = new ArrayList<Object>();                    
                        int last = nArrayList.size() - 1;                        
                        for (int i = 3; i <= last; i++) {
                        	node a = eval(nArrayList.get(i));
                            Object param = a.value;
                            parameters.add(param);
                            Class<?> paramClass;
                            if (a.clazz == null) {
	                            if (param instanceof Integer) paramClass = Integer.TYPE;
	                            else if (param instanceof Double) paramClass = Double.TYPE;
	                            else if (param instanceof Long) paramClass = Long.TYPE;
	                            else if (param instanceof Boolean) paramClass = Boolean.TYPE;
	                            else paramClass = param.getClass();
                            } else {
                            	paramClass = a.clazz; // use hint
                            }
                            parameterTypes[i - 3] = paramClass;
                        }
                        String methodName = nArrayList.get(2).stringValue();
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
                        String className = nArrayList.get(1).stringValue();
                        if (nArrayList.get(1).value instanceof symbol) { // class's static field e.g. (.get java.lang.Math PI)                            
                            cls = Class.forName(className);
                        } else { // object's method
                            obj = eval(nArrayList.get(1)).value;
                            cls = obj.getClass();
                        }                        
                        String fieldName = nArrayList.get(2).stringValue();
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
                        String className = nArrayList.get(1).stringValue();
                        if (nArrayList.get(1).value instanceof symbol) { // class's static field e.g. (.get java.lang.Math PI)                            
                            cls = Class.forName(className);
                        } else { // object's method
                            obj = eval(nArrayList.get(1)).value;
                            cls = obj.getClass();
                        }                        
                        String fieldName = nArrayList.get(2).stringValue();
                        java.lang.reflect.Field field = cls.getField(fieldName);
                        Object value = eval(nArrayList.get(3)).value;
                        field.set(cls, value);
                        return new node();                        
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return new node();
                    }}
                case NEW: {
                    // Java interoperability
                    // (new CLASS ARG ..) ; create new Java object
                    try {
                        String className = nArrayList.get(1).stringValue();
                        Class<?> cls = Class.forName(className);                      
                        Class<?>[] parameterTypes = new Class<?>[nArrayList.size() - 2];
                        ArrayList<Object> parameters = new ArrayList<Object>();                    
                        int last = nArrayList.size() - 1;                        
                        for (int i = 2; i <= last; i++) {
                        	node a = eval(nArrayList.get(i));
                            Object param = a.value;
                            parameters.add(param);
                            Class<?> paramClass;
                            if (a.clazz == null) {
	                            if (param instanceof Integer) paramClass = Integer.TYPE;
	                            else if (param instanceof Double) paramClass = Double.TYPE;
	                            else if (param instanceof Long) paramClass = Long.TYPE;
	                            else if (param instanceof Boolean) paramClass = Boolean.TYPE;
	                            else paramClass = param.getClass();
                            } else {
                            	paramClass = a.clazz; // use hint
                            }
                            parameterTypes[i - 2] = paramClass;	                            
                        }
                        Constructor<?> ctor = cls.getConstructor(parameterTypes);
                        return new node(ctor.newInstance(parameters.toArray()));
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return new node();
                    }}
                case PR: // (pr X ..)
                    {                        
                        for (int i = 1; i < nArrayList.size(); i++) {
                            if (i != 1) System.out.print(" ");
                            System.out.print(eval(nArrayList.get(i)).stringValue());
                        }
                        return new node();
                    }
                case PRN: // (prn X ..)
                    {
                        for (int i = 1; i < nArrayList.size(); i++) {
                            if (i != 1) System.out.print(" ");
                            System.out.print(eval(nArrayList.get(i)).stringValue());
                        }
                        System.out.println();
                        return new node();
                    }
                case EXIT: { // (exit X)
                    System.out.println();
                    System.exit(eval(nArrayList.get(1)).intValue());
                    return new node(); }
                case SYSTEM: { // (system "notepad" "a.txt") ; run external program
                    ArrayList<String> args = new ArrayList<String>();
                    for (int i = 1; i < nArrayList.size(); i++) {
                        args.add(eval(nArrayList.get(i)).stringValue());
                    }
                    ProcessBuilder pb = new ProcessBuilder(args);
                    pb.inheritIO();
                    Process ps = null;
                    try {
                        ps = pb.start();
                        ps.waitFor();
                        return new node(ps.exitValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new node(); 
                    }
                case CONS: { // (cons X LST): Returns a new list where x is the first element and lst is the rest.
                    //node x = new node(eval(nArrayList.get(1)).value);
                    node x = eval(nArrayList.get(1));
                    ArrayList<node> lst = eval(nArrayList.get(2)).arrayListValue();
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(x);
                    for (node n2 : lst) {
                    	r.add(n2);
                    }
                    return new node(r);
                }
                case NULLP: { // (null? X): Returns true if X is null.
                	return new node(eval(nArrayList.get(1)).value == null);
                }
                case CAST: { // (cast CLASS X): Returns type-hinted object.
                	node x = eval(nArrayList.get(2));
                	try {
						x.clazz = Class.forName(nArrayList.get(1).stringValue());
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	return x;
                }
                default: {
                    System.err.println("Not implemented function: [" + func.value.toString() + "]");
                    return new node();}
                } // end switch(found)                
            }
            else {                
                if (func.value instanceof fn) {
                    // anonymous function application. lexical scoping
                    // (fn (ARGUMENT ..) BODY ..)
                    fn f = (fn) func.value;
                    ArrayList<node> arg_syms = f.def.get(1).arrayListValue();                    
                                        
                    int len = arg_syms.size();
                    for (int i=0; i<len; i++) { // assign arguments
                        String k = arg_syms.get(i).stringValue();
                        node cn = compile(nArrayList.get(i + 1), f.env);                        
                        node n2 = eval(cn);
                        f.env.env.put(k, n2);
                    }
                    len = f.def.size();
                    for (int i=2; i<len-1; i++) { // body
                        eval(compile(f.def.get(i), f.env));
                    }
                    node ret = eval(compile(f.def.get(len-1), f.env));
                    return ret;
                }
                else {
                    System.err.println("Unknown function: [" + func.value.toString() + "]");
                    return new node();
                }
            }
        }
        else {
        	return n.clone();
        }
    }
    
    ArrayList<node> compile_all(ArrayList<node> lst) {
        ArrayList<node> compiled = new ArrayList<node>();
        int last = lst.size() - 1;        
        for (int i = 0; i <= last; i++) {
            compiled.add(compile(lst.get(i), global_env));
        }
        return compiled;
    }    

    node eval_all(ArrayList<node> lst) {        
        int last = lst.size() - 1;
        if (last < 0) return new node();
        for (int i = 0; i < last; i++) {
            eval(lst.get(i));
        }
        return eval(lst.get(last));
    }
        
    public node eval_string(String s) {
        ArrayList<node> compiled = compile_all(parse(s));
        return eval_all(compiled);
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
    public void repl() {
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
                tokenizer t = new tokenizer(code);
                t.tokenize();
                if (t.unclosed <= 0) { // no unmatched parenthesis nor quotation
                    eval_print(code);
                    code = "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
