// (C) 2013-2015 Kim, Taegyoon
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
// Version 1.7.1: improved function call
// Version 1.8: added thread
package paren;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.lang.Math;

public class paren {
	public static final String VERSION = "1.12";
    public paren() {
        init();
    }
    
    public static class node implements Cloneable {
        Object value;
        Class<?> clazz = null; // type hint
        
        node() {}
        node(Object value) {
    		this.value = value;
        }
        protected node clone() {
        	node r = new node(this.value);
        	r.clazz = this.clazz;
        	return r;
        }
        int intValue() {
        	if (value instanceof Number) {
        		return ((Number)value).intValue();
        	} else {
        		return Integer.parseInt(stringValue());
        	}
        }
        double doubleValue() {
        	if (value instanceof Number) {
        		return ((Number)value).doubleValue();
        	} else {
        		return Double.parseDouble(stringValue());
        	}
        }
        long longValue() {
        	if (value instanceof Number) {
        		return ((Number)value).longValue();
        	} else {
        		return Long.parseLong(stringValue());
        	}
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
    static final node node_null = new node();

    static class symbol {
		public static HashMap<String, Integer> symcode = new HashMap<String, Integer>();
		public static ArrayList<String> symname = new ArrayList<String>();
		public int code;
		public static int ToCode(String name) {
			Integer r = symcode.get(name);
			if (r == null) {
				r = symcode.size();
				symcode.put(name, r);
				symname.add(name);
			}
			return r;
		}

		public symbol(String name) {
			code = ToCode(name);
		}

		public String toString() {
			return symname.get(code);
		}
    }
    
    static class fn { // anonymous function
        ArrayList<node> def; // definition
        environment outer_env;        
        fn(ArrayList<node> def, environment outer_env) {
            this.def = def;
            this.outer_env = outer_env;
        }
        public String toString() {
            return def.toString();
        }
    }
    
    static class environment {
        //HashMap<String, node> env = new HashMap<String, node>();
    	HashMap<Integer, node> env = new HashMap<Integer, node>();
        environment outer;
        environment() {this.outer = null;}
        environment(environment outer) {this.outer = outer;}
        node get(int code) {
            node found = env.get(code);            
            if (found != null) {
                return found;
            }
            else {
                if (outer != null) {
                    return outer.get(code);
                }
                else {
                    return null;
                }
            }            
        }
        
        node set(int code, node v) {
        	node v2 = v.clone();
        	env.put(code, v2);
        	return v2;
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
                else if (c == ';' || c == '#') { // end-of-line comment
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
        PLUS, MINUS, MUL, DIV, CARET, PERCENT, SQRT, INC, DEC, PLUSPLUS, MINUSMINUS, FLOOR, CEIL, LN, LOG10, RAND,
        EQ, EQEQ, NOTEQ, LT, GT, LTE, GTE, ANDAND, OROR, NOT,
        IF, WHEN, FOR, WHILE,
        STRLEN, STRCAT, CHAR_AT, CHR,
        INT, DOUBLE, STRING, READ_STRING, TYPE, SET,
        EVAL, QUOTE, FN, LIST, APPLY, FOLD, MAP, FILTER, RANGE, NTH, LENGTH, BEGIN, DOT, DOTGET, DOTSET, NEW,
        PR, PRN, EXIT, SYSTEM, CONS, LONG, NULLP, CAST, DEFMACRO, READ_LINE, SLURP, SPIT, THREAD, DEF, BREAK
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
        System.out.println("Parenj " + VERSION + " (C) 2013 Kim, Taegyoon");
        System.out.println("Predefined Symbols:");
        ArrayList<String> r = new ArrayList<String>(global_env.env.keySet().size());
        for (int x : global_env.env.keySet()) {
        	r.add(symbol.symname.get(x));        	        	
        }
        for (String x : new TreeSet<String>(r)) {
        	System.out.print(" " + x);
        }
        System.out.println();
        System.out.println("Macros:");
        print_collection(macros.keySet());
    }    

    void init() throws Exception {
        global_env.env.put(symbol.ToCode("true"), node_true);
        global_env.env.put(symbol.ToCode("false"), node_false);
        global_env.env.put(symbol.ToCode("E"), new node(2.71828182845904523536));
        global_env.env.put(symbol.ToCode("PI"), new node(3.14159265358979323846));
        global_env.env.put(symbol.ToCode("null"), new node());

        global_env.env.put(symbol.ToCode("+"), new node(builtin.PLUS));
        global_env.env.put(symbol.ToCode("-"), new node(builtin.MINUS));
        global_env.env.put(symbol.ToCode("*"), new node(builtin.MUL));
        global_env.env.put(symbol.ToCode("/"), new node(builtin.DIV));
        global_env.env.put(symbol.ToCode("^"), new node(builtin.CARET));
        global_env.env.put(symbol.ToCode("%"), new node(builtin.PERCENT));
        global_env.env.put(symbol.ToCode("sqrt"), new node(builtin.SQRT));
        global_env.env.put(symbol.ToCode("inc"), new node(builtin.INC));
        global_env.env.put(symbol.ToCode("dec"), new node(builtin.DEC));
        global_env.env.put(symbol.ToCode("++"), new node(builtin.PLUSPLUS));
        global_env.env.put(symbol.ToCode("--"), new node(builtin.MINUSMINUS));
        global_env.env.put(symbol.ToCode("floor"), new node(builtin.FLOOR));
        global_env.env.put(symbol.ToCode("ceil"), new node(builtin.CEIL));
        global_env.env.put(symbol.ToCode("ln"), new node(builtin.LN));
        global_env.env.put(symbol.ToCode("log10"), new node(builtin.LOG10));
        global_env.env.put(symbol.ToCode("rand"), new node(builtin.RAND));
        global_env.env.put(symbol.ToCode("="), new node(builtin.EQ));
        global_env.env.put(symbol.ToCode("=="), new node(builtin.EQEQ));
        global_env.env.put(symbol.ToCode("!="), new node(builtin.NOTEQ));
        global_env.env.put(symbol.ToCode("<"), new node(builtin.LT));
        global_env.env.put(symbol.ToCode(">"), new node(builtin.GT));
        global_env.env.put(symbol.ToCode("<="), new node(builtin.LTE));
        global_env.env.put(symbol.ToCode(">="), new node(builtin.GTE));
        global_env.env.put(symbol.ToCode("&&"), new node(builtin.ANDAND));
        global_env.env.put(symbol.ToCode("||"), new node(builtin.OROR));
        global_env.env.put(symbol.ToCode("!"), new node(builtin.NOT));
        global_env.env.put(symbol.ToCode("if"), new node(builtin.IF));
        global_env.env.put(symbol.ToCode("when"), new node(builtin.WHEN));
        global_env.env.put(symbol.ToCode("for"), new node(builtin.FOR));
        global_env.env.put(symbol.ToCode("while"), new node(builtin.WHILE));
        global_env.env.put(symbol.ToCode("strlen"), new node(builtin.STRLEN));
        global_env.env.put(symbol.ToCode("strcat"), new node(builtin.STRCAT));
        global_env.env.put(symbol.ToCode("char-at"), new node(builtin.CHAR_AT));
        global_env.env.put(symbol.ToCode("chr"), new node(builtin.CHR));
        global_env.env.put(symbol.ToCode("int"), new node(builtin.INT));
        global_env.env.put(symbol.ToCode("double"), new node(builtin.DOUBLE));
        global_env.env.put(symbol.ToCode("string"), new node(builtin.STRING));
        global_env.env.put(symbol.ToCode("read-string"), new node(builtin.READ_STRING));
        global_env.env.put(symbol.ToCode("type"), new node(builtin.TYPE));
        global_env.env.put(symbol.ToCode("eval"), new node(builtin.EVAL));
        global_env.env.put(symbol.ToCode("quote"), new node(builtin.QUOTE));
        global_env.env.put(symbol.ToCode("fn"), new node(builtin.FN));
        global_env.env.put(symbol.ToCode("list"), new node(builtin.LIST));
        global_env.env.put(symbol.ToCode("apply"), new node(builtin.APPLY));
        global_env.env.put(symbol.ToCode("fold"), new node(builtin.FOLD));
        global_env.env.put(symbol.ToCode("map"), new node(builtin.MAP));
        global_env.env.put(symbol.ToCode("filter"), new node(builtin.FILTER));
        global_env.env.put(symbol.ToCode("range"), new node(builtin.RANGE));
        global_env.env.put(symbol.ToCode("nth"), new node(builtin.NTH));
        global_env.env.put(symbol.ToCode("length"), new node(builtin.LENGTH));
        global_env.env.put(symbol.ToCode("begin"), new node(builtin.BEGIN));
        global_env.env.put(symbol.ToCode("."), new node(builtin.DOT));
        global_env.env.put(symbol.ToCode(".get"), new node(builtin.DOTGET));
        global_env.env.put(symbol.ToCode(".set"), new node(builtin.DOTSET));
        global_env.env.put(symbol.ToCode("new"), new node(builtin.NEW));
        global_env.env.put(symbol.ToCode("set"), new node(builtin.SET));
        global_env.env.put(symbol.ToCode("pr"), new node(builtin.PR));
        global_env.env.put(symbol.ToCode("prn"), new node(builtin.PRN));
        global_env.env.put(symbol.ToCode("exit"), new node(builtin.EXIT));
        global_env.env.put(symbol.ToCode("system"), new node(builtin.SYSTEM));
        global_env.env.put(symbol.ToCode("cons"), new node(builtin.CONS));
        global_env.env.put(symbol.ToCode("long"), new node(builtin.LONG));
        global_env.env.put(symbol.ToCode("null?"), new node(builtin.NULLP));
        global_env.env.put(symbol.ToCode("cast"), new node(builtin.CAST));
        global_env.env.put(symbol.ToCode("defmacro"), new node(builtin.DEFMACRO));
        global_env.env.put(symbol.ToCode("read-line"), new node(builtin.READ_LINE));
        global_env.env.put(symbol.ToCode("slurp"), new node(builtin.SLURP));
        global_env.env.put(symbol.ToCode("spit"), new node(builtin.SPIT));
        global_env.env.put(symbol.ToCode("thread"), new node(builtin.THREAD));
        global_env.env.put(symbol.ToCode("def"), new node(builtin.DEF));
        global_env.env.put(symbol.ToCode("break"), new node(builtin.BREAK));
        eval_string("(defmacro setfn (name ...) (set name (fn ...)))");
        eval_string("(defmacro defn (name ...) (def name (fn ...)))");
		eval_string("(defmacro join (t) (. t join))");
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

    node compile(node n) {    	
        if (n.value instanceof ArrayList) { // function (FUNCTION ARGUMENT ..)
            ArrayList<node> nArrayList = n.arrayListValue();                
            if (nArrayList.size() == 0) return n;
            node func = compile(nArrayList.get(0));
            if (func.value instanceof symbol && func.toString().equals(("defmacro"))) {
                // (defmacro add (a b) (+ a b)) ; define macro
            	macros.put(nArrayList.get(1).stringValue(), new node[]{nArrayList.get(2), nArrayList.get(3)});
                return node_null;                	
            } else {
            	if (macros.containsKey(nArrayList.get(0).stringValue())) { // compile macro
                    return compile(macroexpand(n));
            	} else {            	
	                ArrayList<node> r = new ArrayList<node>();
	                for (node n2: nArrayList) {
	                    r.add(compile(n2));
	                }
	                return new node(r);
            	}
            }
        } else {
            return n;
        }
    }    
        
    node eval(node n, environment env) throws Exception {
    	if (n.value instanceof symbol) {
    		//node r = env.get(n.toString());
    		node r = env.get(((symbol)n.value).code);
//    		if (r == null) {
//    			System.err.println("Unknown variable: " + n.toString());
//    			return node_null;
//    		}
    		return r;
    	}
    	else if (n.value instanceof ArrayList) { // function (FUNCTION ARGUMENT ..)
            ArrayList<node> nArrayList = n.arrayListValue();                
            if (nArrayList.size() == 0) return node_null;
            node func = eval(nArrayList.get(0), env);
            builtin foundBuiltin;
            if (func.value instanceof builtin) {
                foundBuiltin = (builtin) func.value;
                switch(foundBuiltin) {
                case PLUS: // (+ X ..)
                    {
                        int len = nArrayList.size();
                        if (len <= 1) return node_0;
                        node first = eval(nArrayList.get(1), env);
                        if (first.value instanceof Integer) {
                            int acc = first.intValue();
                            for (int i = 2; i < len; i++) {
                                acc += eval(nArrayList.get(i), env).intValue();
                            }
                            return new node(acc);
                        }
                        else if (first.value instanceof Long) {
                        	long acc = first.longValue();
                            for (int i = 2; i < len; i++) {
                                acc += eval(nArrayList.get(i), env).longValue();
                            }
                            return new node(acc);
                        }
                        else {
                            double acc = first.doubleValue();
                            for (int i = 2; i < len; i++) {
                                acc += eval(nArrayList.get(i), env).doubleValue();
                            }
                            return new node(acc);
                        }
                    }
                case MINUS: // (- X ..)
                    {
                        int len = nArrayList.size();
                        if (len <= 1) return node_0;
                        node first = eval(nArrayList.get(1), env);
                        if (first.value instanceof Integer) {
                            int acc = first.intValue();
                            for (int i = 2; i < len; i++) {
                                acc -= eval(nArrayList.get(i), env).intValue();
                            }
                            return new node(acc);
                        }
                        else if (first.value instanceof Long) {
                        	long acc = first.longValue();
                            for (int i = 2; i < len; i++) {
                                acc -= eval(nArrayList.get(i), env).longValue();
                            }
                            return new node(acc);
                        }
                        else {
                            double acc = first.doubleValue();
                            for (int i = 2; i < len; i++) {
                                acc -= eval(nArrayList.get(i), env).doubleValue();
                            }
                            return new node(acc);
                        }
                    }
                case MUL: // (* X ..)
                {
                    int len = nArrayList.size();
                    if (len <= 1) return node_1;
                    node first = eval(nArrayList.get(1), env);
                    if (first.value instanceof Integer) {
                        int acc = first.intValue();
                        for (int i = 2; i < len; i++) {
                            acc *= eval(nArrayList.get(i), env).intValue();
                        }
                        return new node(acc);
                    }
                    else if (first.value instanceof Long) {
                    	long acc = first.longValue();
                        for (int i = 2; i < len; i++) {
                            acc *= eval(nArrayList.get(i), env).longValue();
                        }
                        return new node(acc);
                    }
                    else {
                        double acc = first.doubleValue();
                        for (int i = 2; i < len; i++) {
                            acc *= eval(nArrayList.get(i), env).doubleValue();
                        }
                        return new node(acc);
                    }
                }
                case DIV: // (/ X ..)
                {
                    int len = nArrayList.size();
                    if (len <= 1) return node_1;
                    node first = eval(nArrayList.get(1), env);
                    if (first.value instanceof Integer) {
                        int acc = first.intValue();
                        for (int i = 2; i < len; i++) {
                            acc /= eval(nArrayList.get(i), env).intValue();
                        }
                        return new node(acc);
                    }
                    else if (first.value instanceof Long) {
                    	long acc = first.longValue();
                        for (int i = 2; i < len; i++) {
                            acc /= eval(nArrayList.get(i), env).longValue();
                        }
                        return new node(acc);
                    }
                    else {
                        double acc = first.doubleValue();
                        for (int i = 2; i < len; i++) {
                            acc /= eval(nArrayList.get(i), env).doubleValue();
                        }
                        return new node(acc);
                    }
                }
                case CARET: { // (^ BASE EXPONENT)
                    return new node(Math.pow(
                            eval(nArrayList.get(1), env).doubleValue(),
                            eval(nArrayList.get(2), env).doubleValue()));}
                case PERCENT: { // (% DIVIDEND DIVISOR)
                    return new node(eval(nArrayList.get(1), env).intValue() % eval(nArrayList.get(2), env).intValue());}
                case SQRT: { // (sqrt X)
                    return new node(Math.sqrt(eval(nArrayList.get(1), env).doubleValue()));}
                case INC: { // (inc X)
                    int len = nArrayList.size();
                    if (len <= 1) return node_0;
                    node first = eval(nArrayList.get(1), env);
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
                    node first = eval(nArrayList.get(1), env);
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
                    //node n2 = nArrayList.get(1);
                    //node n2 = env.get(nArrayList.get(1).toString());
                    node n2 = env.get(((symbol)nArrayList.get(1).value).code);
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
                    //node n2 = nArrayList.get(1);
                    //node n2 = env.get(nArrayList.get(1).toString());
                    node n2 = env.get(((symbol)nArrayList.get(1).value).code);
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
                    return new node(Math.floor(eval(nArrayList.get(1), env).doubleValue()));}
                case CEIL: { // (ceil X)
                    return new node(Math.ceil(eval(nArrayList.get(1), env).doubleValue()));}
                case LN: { // (ln X)
                    return new node(Math.log(eval(nArrayList.get(1), env).doubleValue()));}
                case LOG10: { // (log10 X)
                    return new node(Math.log10(eval(nArrayList.get(1), env).doubleValue()));}
                case RAND: { // (rand)
                    return new node(Math.random());}
                case SET: { // (set SYMBOL-OR-PLACE VALUE)
                    node var = eval(nArrayList.get(1), env);
                    node value = eval(nArrayList.get(2), env);
                    if (var == null) {// new variable
                        return env.set(((symbol)nArrayList.get(1).value).code, value);
                    }
                    else {
                        var.value = value.value;
                        return var;
                    }
                }
                case DEF: { // (def SYMBOL VALUE) ; set in the current environment
                    node value = eval(nArrayList.get(2), env);
                    return env.set(((symbol)nArrayList.get(1).value).code, value);
                }
                case EQ: { // (= X ..) short-circuit, Object.equals()
                    node first = eval(nArrayList.get(1), env);
                    Object firstv = first.value;
                    for (int i = 2; i < nArrayList.size(); i++) {
                        if (!eval(nArrayList.get(i), env).value.equals(firstv)) {return node_false;}
                    }
                    return node_true;}
                case EQEQ: { // (== X ..) short-circuit                    
                    node first = eval(nArrayList.get(1), env);
                    if (first.value instanceof Integer) {
                        int firstv = first.intValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i), env).intValue() != firstv) {return node_false;}
                        }
                    }
                    else if (first.value instanceof Long) {
                	   long firstv = first.longValue();                        
                       for (int i = 2; i < nArrayList.size(); i++) {
                           if (eval(nArrayList.get(i), env).longValue() != firstv) {return node_false;}
                       }
                    }
                    else {
                        double firstv = first.doubleValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i), env).doubleValue() != firstv) {return node_false;}
                        }
                    }
                    return node_true;}
                case NOTEQ: { // (!= X ..) short-circuit                    
                    node first = eval(nArrayList.get(1), env);
                    if (first.value instanceof Integer) {
                        int firstv = first.intValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i), env).intValue() == firstv) {return node_false;}
                        }
                    }
                    else if (first.value instanceof Long) {
                	   long firstv = first.longValue();                        
                       for (int i = 2; i < nArrayList.size(); i++) {
                           if (eval(nArrayList.get(i), env).longValue() == firstv) {return node_false;}
                       }
                    }
                    else {
                        double firstv = first.doubleValue();                        
                        for (int i = 2; i < nArrayList.size(); i++) {
                            if (eval(nArrayList.get(i), env).doubleValue() == firstv) {return node_false;}
                        }
                    }
                    return node_true;}
                case LT: { // (< X Y)
                    node first = eval(nArrayList.get(1), env);
                    node second = eval(nArrayList.get(2), env);
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
                    node first = eval(nArrayList.get(1), env);
                    node second = eval(nArrayList.get(2), env);
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
                    node first = eval(nArrayList.get(1), env);
                    node second = eval(nArrayList.get(2), env);
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
                    node first = eval(nArrayList.get(1), env);
                    node second = eval(nArrayList.get(2), env);
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
                        if (!eval(nArrayList.get(i), env).booleanValue()) {return node_false;}
                    }
                    return node_true;}
                case OROR: { // (|| X ..) short-circuit
                    for (int i = 1; i < nArrayList.size(); i++) {
                        if (eval(nArrayList.get(i), env).booleanValue()) {return node_true;}
                    }
                    return node_false;}
                case NOT: { // (! X)
                    return new node(!(eval(nArrayList.get(1), env).booleanValue()));}
                case IF: { // (if CONDITION THEN_EXPR ELSE_EXPR)
                    node cond = nArrayList.get(1);
                    if (eval(cond, env).booleanValue()) {
                        return eval(nArrayList.get(2), env);
                    }
                    else {
                        return eval(nArrayList.get(3), env);
                    }}
                case WHEN: { // (when CONDITION EXPR ..)
                    node cond = nArrayList.get(1);
                    if (eval(cond, env).booleanValue()) {
                        int len = nArrayList.size();
                        for (int i = 2; i < len - 1; i++) {
                            eval(nArrayList.get(i), env);
                        }
                        return eval(nArrayList.get(len - 1), env); // returns last EXPR
                    }
                    return node_null;}
                case FOR: // (for SYMBOL START END STEP EXPR ..)
                    {
                    	try {
	                        node start = eval(nArrayList.get(2), env);
	                        int len = nArrayList.size();
	                        if (start.value instanceof Integer) {                            
	                            int last = eval(nArrayList.get(3), env).intValue();
	                            int step = eval(nArrayList.get(4), env).intValue();                            
	                            int a = start.intValue();
	                            //node na = nArrayList.get(1);
	                            //node na = env.set(nArrayList.get(1).toString(), new node(a));
	                            node na = env.set(((symbol)nArrayList.get(1).value).code, new node(a));
	                            if (step >= 0) {
	                                for (; a <= last; a += step) {
	                                    na.value = a;
	                                    for (int i = 5; i < len; i++) {
	                                        eval(nArrayList.get(i), env);
	                                    }
	                                }
	                            }
	                            else {
	                                for (; a >= last; a += step) {
	                                    na.value = a;
	                                    for (int i = 5; i < len; i++) {
	                                        eval(nArrayList.get(i), env);
	                                    }
	                                }
	                            }
	                        }
	                        else if (start.value instanceof Long) {
	                            long last = eval(nArrayList.get(3), env).longValue();
	                            long step = eval(nArrayList.get(4), env).longValue();                            
	                            long a = start.longValue();
	                            //node na = nArrayList.get(1);
	                            node na = env.set(((symbol)nArrayList.get(1).value).code, new node(a));
	                            if (step >= 0) {
	                                for (; a <= last; a += step) {
	                                    na.value = a;
	                                    for (int i = 5; i < len; i++) {
	                                        eval(nArrayList.get(i), env);
	                                    }
	                                }
	                            }
	                            else {
	                                for (; a >= last; a += step) {
	                                    na.value = a;
	                                    for (int i = 5; i < len; i++) {
	                                        eval(nArrayList.get(i), env);
	                                    }
	                                }
	                            }
	                        }
	                        else {
	                            double last = eval(nArrayList.get(3), env).doubleValue();
	                            double step = eval(nArrayList.get(4), env).doubleValue();                            
	                            double a = start.doubleValue();                            
	                            //node na = nArrayList.get(1);
	                            node na = env.set(((symbol)nArrayList.get(1).value).code, new node(a));
	                            if (step >= 0) {
	                                for (; a <= last; a += step) {
	                                    na.value = a;
	                                    for (int i = 5; i < len; i++) {
	                                        eval(nArrayList.get(i), env);
	                                    }
	                                }
	                            }
	                            else {
	                                for (; a >= last; a += step) {
	                                    na.value = a;
	                                    for (int i = 5; i < len; i++) {
	                                        eval(nArrayList.get(i), env);
	                                    }
	                                }
	                            }
	                        }
                    	} catch (BreakException e) {
                    		
                    	}
                        return node_null;
                    }
                case WHILE: { // (while CONDITION EXPR ..)
                    try {
	                    node cond = nArrayList.get(1);
	                    int len = nArrayList.size();
	                    while (eval(cond, env).booleanValue()) {
	                        for (int i = 2; i < len; i++) {
	                            eval(nArrayList.get(i), env);
	                        }
	                    }
                    } catch (BreakException E) {
                    
                    }
                    return node_null; }
                case BREAK: { // (break)
                	throw new BreakException();
                }
                case STRLEN: { // (strlen X)
                    return new node(eval(nArrayList.get(1), env).stringValue().length());}
                case STRCAT: { // (strcat X ..)
                    int len = nArrayList.size();
                    if (len <= 1) return new node("");
                    node first = eval(nArrayList.get(1), env);
                    String acc = first.stringValue();
                    for (int i = 2; i < nArrayList.size(); i++) {
                        acc += eval(nArrayList.get(i), env).stringValue();
                    }
                    return new node(acc);}
                case CHAR_AT: { // (char-at X POSITION)
                    return new node((int) eval(nArrayList.get(1), env).stringValue().charAt(eval(nArrayList.get(2), env).intValue()));}
                case CHR: { // (chr X)                    
                    char[] temp = {0};
                    temp[0] = (char) eval(nArrayList.get(1), env).intValue();
                    return new node(new String(temp));}
                case STRING: { // (string X)
                    return new node(eval(nArrayList.get(1), env).stringValue());}
                case DOUBLE: { // (double X)
                    return new node(eval(nArrayList.get(1), env).doubleValue());}
                case INT: { // (int X)
                    return new node(eval(nArrayList.get(1), env).intValue());}
                case LONG: { // (long X)
                    return new node(eval(nArrayList.get(1), env).longValue());}                
                case READ_STRING: { // (read-string X)
                    return new node(parse(eval(nArrayList.get(1), env).stringValue()).get(0).value);}
                case TYPE: { // (type X)
                    return new node(eval(nArrayList.get(1), env).type());}
                case EVAL: { // (eval X)
                    return eval(eval(nArrayList.get(1), env), env);}
                case QUOTE: { // (quote X)
                    return nArrayList.get(1);}
                case FN: {
                    // anonymous function. lexical scoping
                    // (fn (ARGUMENT ..) BODY ..)                    
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(func);
                    
                    for (int i=1; i<nArrayList.size(); i++) {
                        r.add(nArrayList.get(i));
                    }
                    return new node(new fn(r, env));}                 
                case LIST: { // (list X ..)
                    ArrayList<node> ret = new ArrayList<node>();
                    for (int i = 1; i < nArrayList.size(); i++) {
                        ret.add(eval(nArrayList.get(i), env));
                    }
                    return new node(ret);}
                case APPLY: { // (apply FUNC LIST)
                    ArrayList<node> expr = new ArrayList<node>();
                    node f = eval(nArrayList.get(1), env);
                    expr.add(f);
                    ArrayList<node> lst = eval(nArrayList.get(2), env).arrayListValue();
                    for (int i = 0; i < lst.size(); i++) {
                    	ArrayList<node> item = new ArrayList<node>();
                    	item.add(new node(new symbol("quote")));
                    	item.add(lst.get(i));
                        expr.add(new node(item));
                    }                    
                    return eval(new node(expr), env);
                }
                case FOLD: { // (fold FUNC LIST)
                    node f = eval(nArrayList.get(1), env);
                    ArrayList<node> lst = eval(nArrayList.get(2), env).arrayListValue();
                    node acc = eval(lst.get(0), env);
                    ArrayList<node> expr = new ArrayList<node>(); // (FUNC ITEM)
                    expr.add(f);
                    expr.add(null); // first argument
                    expr.add(null); // second argument
                    for (int i = 1; i < lst.size(); i++) {
                        expr.set(1, acc);
                        expr.set(2, lst.get(i));
                        acc = eval(new node(expr), env);
                    }
                    return acc;
                }
                case MAP: { // (map FUNC LIST)
                    node f = eval(nArrayList.get(1), env);
                    ArrayList<node> lst = eval(nArrayList.get(2), env).arrayListValue();
                    ArrayList<node> acc = new ArrayList<node>();
                    ArrayList<node> expr = new ArrayList<node>(); // (FUNC ITEM)                       
                    expr.add(f);
                    expr.add(null);
                    for (int i = 0; i < lst.size(); i++) {
                        expr.set(1, lst.get(i));
                        acc.add(eval(new node(expr), env));
                    }                    
                    return new node(acc);
                }
                case FILTER: { // (filter FUNC LIST)
                    node f = eval(nArrayList.get(1), env);
                    ArrayList<node> lst = eval(nArrayList.get(2), env).arrayListValue();
                    ArrayList<node> acc = new ArrayList<node>();
                    ArrayList<node> expr = new ArrayList<node>(); // (FUNC ITEM)                    
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
                    node start = eval(nArrayList.get(1), env);                    
                    ArrayList<node> ret = new ArrayList<node>();
                    if (start.value instanceof Integer) {
                        int a = eval(nArrayList.get(1), env).intValue();
                        int last = eval(nArrayList.get(2), env).intValue();
                        int step = eval(nArrayList.get(3), env).intValue();                        
                        if (step >= 0) {
                            for (; a <= last; a += step) {
                                ret.add(new node(a));}}
                        else {
                            for (; a >= last; a += step) {
                                ret.add(new node(a));}}
                    }
                    else if (start.value instanceof Long) {
                        long a = eval(nArrayList.get(1), env).longValue();
                        long last = eval(nArrayList.get(2), env).longValue();
                        long step = eval(nArrayList.get(3), env).longValue();                        
                        if (step >= 0) {
                            for (; a <= last; a += step) {
                                ret.add(new node(a));}}
                        else {
                            for (; a >= last; a += step) {
                                ret.add(new node(a));}}
                    }
                    else {
                        double a = eval(nArrayList.get(1), env).doubleValue();
                        double last = eval(nArrayList.get(2), env).doubleValue();
                        double step = eval(nArrayList.get(3), env).doubleValue();                        
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
                    int i = eval(nArrayList.get(1), env).intValue();
                    ArrayList<node> lst = eval(nArrayList.get(2), env).arrayListValue();
                    return lst.get(i);}
                case LENGTH: { // (length LIST)                    
                    ArrayList<node> lst = eval(nArrayList.get(1), env).arrayListValue();
                    return new node(lst.size());}
                case BEGIN: { // (begin X ..)                    
                    int last = nArrayList.size() - 1;
                    if (last <= 0) return node_null;
                    for (int i = 1; i < last; i++) {
                        eval(nArrayList.get(i), env);
                    }
                    return eval(nArrayList.get(last), env);}
                case DOT: {
                    // Java interoperability
                    // (. CLASS METHOD ARGUMENT ..) ; Java method invocation
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nArrayList.get(1).stringValue();
                        //if (nArrayList.get(1).value instanceof symbol) { // class's static method e.g. (. java.lang.Math floor 1.5)
                        if (nArrayList.get(1).value instanceof symbol && env.get(((symbol)nArrayList.get(1).value).code) == null) { // class's static method e.g. (. System.Math Floor 1.5)	
                            cls = Class.forName(className);
                        } else { // object's method e.g. (. "abc" length)
                            obj = eval(nArrayList.get(1), env).value;
                            cls = obj.getClass();
                        }                        
                        Class<?>[] parameterTypes = new Class<?>[nArrayList.size() - 3];
                        ArrayList<Object> parameters = new ArrayList<Object>();                    
                        int last = nArrayList.size() - 1;                        
                        for (int i = 3; i <= last; i++) {
                        	node a = eval(nArrayList.get(i), env);
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
                        return node_null;
                    }}
                case DOTGET: {
                    // Java interoperability
                    // (.get CLASS FIELD) ; get Java field
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nArrayList.get(1).stringValue();
                        //if (nArrayList.get(1).value instanceof symbol) { // class's static field e.g. (.get java.lang.Math PI)
                        if (nArrayList.get(1).value instanceof symbol && env.get(((symbol)nArrayList.get(1).value).code) == null) { // class's static method e.g. (. System.Math Floor 1.5)
                            cls = Class.forName(className);
                        } else { // object's method
                            obj = eval(nArrayList.get(1), env).value;
                            cls = obj.getClass();
                        }                        
                        String fieldName = nArrayList.get(2).stringValue();
                        java.lang.reflect.Field field = cls.getField(fieldName);                        
                        return new node(field.get(cls));
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return node_null;
                    }}
                case DOTSET: {
                    // Java interoperability
                    // (.set CLASS FIELD VALUE) ; set Java field
                    try {                        
                        Class<?> cls;
                        Object obj = null;
                        String className = nArrayList.get(1).stringValue();
                        //if (nArrayList.get(1).value instanceof symbol) { // class's static field e.g. (.get java.lang.Math PI)
                        if (nArrayList.get(1).value instanceof symbol && env.get(((symbol)nArrayList.get(1).value).code) == null) { // class's static method e.g. (. System.Math Floor 1.5)
                            cls = Class.forName(className);
                        } else { // object's method
                            obj = eval(nArrayList.get(1), env).value;
                            cls = obj.getClass();
                        }                        
                        String fieldName = nArrayList.get(2).stringValue();
                        java.lang.reflect.Field field = cls.getField(fieldName);
                        Object value = eval(nArrayList.get(3), env).value;
                        field.set(cls, value);
                        return node_null;                        
                    } catch (Exception e) {                        
                        e.printStackTrace();
                        return node_null;
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
                        	node a = eval(nArrayList.get(i), env);
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
                        return node_null;
                    }}
                case PR: // (pr X ..)
                    {                        
                        for (int i = 1; i < nArrayList.size(); i++) {
                            if (i != 1) System.out.print(" ");
                            System.out.print(eval(nArrayList.get(i), env).stringValue());
                        }
                        return node_null;
                    }
                case PRN: // (prn X ..)
                    {
                        for (int i = 1; i < nArrayList.size(); i++) {
                            if (i != 1) System.out.print(" ");
                            System.out.print(eval(nArrayList.get(i), env).stringValue());
                        }
                        System.out.println();
                        return node_null;
                    }
                case EXIT: { // (exit X)
                    System.out.println();
                    System.exit(eval(nArrayList.get(1), env).intValue());
                    return node_null; }
                case SYSTEM: { // (system "notepad" "a.txt") ; run external program
                    ArrayList<String> args = new ArrayList<String>();
                    for (int i = 1; i < nArrayList.size(); i++) {
                        args.add(eval(nArrayList.get(i), env).stringValue());
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
                    return node_null; 
                    }
                case CONS: { // (cons X LST): Returns a new list where x is the first element and lst is the rest.
                    //node x = new node(eval(nArrayList.get(1)).value);
                    node x = eval(nArrayList.get(1), env);
                    ArrayList<node> lst = eval(nArrayList.get(2), env).arrayListValue();
                    ArrayList<node> r = new ArrayList<node>();
                    r.add(x);
                    for (node n2 : lst) {
                    	r.add(n2);
                    }
                    return new node(r);
                }
                case NULLP: { // (null? X): Returns true if X is null.
                	return new node(eval(nArrayList.get(1), env).value == null);
                }
                case CAST: { // (cast CLASS X): Returns type-hinted object.
                	node x = eval(nArrayList.get(2), env);
                	try {
						x.clazz = Class.forName(nArrayList.get(1).stringValue());
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	return x;
                }
                case READ_LINE: { // (read-line)
                	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                	try {
						return new node(br.readLine());
					} catch (IOException e) {
						return node_null;
					}
                }
                case SLURP: { // (slurp FILENAME)
					String filename = eval(nArrayList.get(1), env).stringValue();
					try {
						return new node(slurp(filename));
					} catch (IOException e) {
						return node_null;
					}
                }
                case SPIT: { // (spit FILENAME STRING)
                	String filename = eval(nArrayList.get(1), env).stringValue();
                	String str = eval(nArrayList.get(2), env).stringValue();
                	return new node(spit(filename, str));
                }
                case THREAD: { // (thread EXPR ..): Creates new thread and starts it.
                	final ArrayList<node> exprs = new ArrayList<node>(nArrayList.subList(1, nArrayList.size()));
                	final environment env2 = env;
                	Thread t = new Thread () {
                		public void run() {
                			for (node n : exprs) {
                				try {
									eval(n, env2);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
                			}
                		}
                	};                    
                    t.start();
                    return new node(t);
                }                
                default: {
                    System.err.println("Not implemented function: [" + func.value.toString() + "]");
                    return node_null;}
                } // end switch(found)                
            }
            else {                
                if (func.value instanceof fn) {
                    // anonymous function application. lexical scoping
                    // (fn (ARGUMENT ..) BODY ..)                	                	
                    fn f = (fn) func.value;                	
                    ArrayList<node> arg_syms = f.def.get(1).arrayListValue();
                    environment local_env = new environment(f.outer_env);
                                        
                    int len = arg_syms.size();
                    for (int i=0; i<len; i++) { // assign arguments
//                        String k = arg_syms.get(i).stringValue();
                    	node k = arg_syms.get(i);
                        node n2 = eval(nArrayList.get(i+1), env);
//                        local_env.env.put(k, n2);
                    	local_env.set(((symbol)k.value).code, n2);                    	
                    }
                    
                    len = f.def.size();
                    node ret = null;
                    for (int i=2; i<len; i++) { // body
                    	ret = eval(f.def.get(i), local_env);
                    }
                    return ret;
                }
                else {
                    System.err.println("Unknown function: [" + func.value.toString() + "]");
                    return node_null;
                }
            }
        }
        else {
//        	return n.clone();
        	return n;
        }
    }
    
    ArrayList<node> compile_all(ArrayList<node> lst) {
        ArrayList<node> compiled = new ArrayList<node>();
        int last = lst.size() - 1;        
        for (int i = 0; i <= last; i++) {
            compiled.add(compile(lst.get(i)));
        }
        return compiled;
    }    

    node eval_all(ArrayList<node> lst) throws Exception {        
        int last = lst.size() - 1;
        if (last < 0) return node_null;
        node ret = null;
        for (int i = 0; i <= last; i++) {
            ret = eval(lst.get(i), global_env);
        }
        return ret;
    }
        
    public node eval_string(String s) throws Exception {
        ArrayList<node> compiled = compile_all(parse(s));
        return eval_all(compiled);
    }
    
    void eval_print(String s) throws Exception {
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
                code = "";
            }
        }
    }
    
    // extracts characters from filename
    public static String slurp(String fileName) throws IOException {
    	return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fileName)));
    }
    
 // Opposite of slurp. Writes str to filename.
    public static int spit(String fileName, String str) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
			bw.write(str);
			bw.close();
			return str.length();
		} catch (IOException e) {
			return -1;
		}
    }
}
