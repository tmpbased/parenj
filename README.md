# ParenJ: The Paren Programming Language written in Java #

(C) 2013-2014 Kim, Taegyoon

Paren is a dialect of Lisp. It is designed to be an embedded language. You can use Java in your Paren program.

## Compile ##
```
cd src
javac paren.java parenj.java
```

## Run ##
```
Usage:
java paren.parenj [OPTIONS...] [FILES...]
java -cp parenj-version.jar paren.parenj [OPTIONS...] [FILES...]
java -jar parenj-version.jar [OPTIONS...] [FILES...]

OPTIONS:
    -h    print this screen.
    -v    print version.
```

## Reference ##
```
Predefined Symbols:
 ! != % && * + ++ - -- . .get .set / < <= = == > >= E PI ^ apply begin cast ceil char-at chr cons dec def defmacro double eval exit false filter floor fn fold for if inc int length list ln log10 long map new nth null null? pr prn quote rand range read-line read-string set slurp spit sqrt strcat string strlen system thread true type when while ||
Macros:
 defn join setfn
```

## Files ##
* paren.java: Paren language library
* parenj.java: Paren REPL executable

## Examples ##
### Hello, World! ###
```
(prn "Hello, World!")
```

### Comment ###
```
# comment
; comment
```

### Function ###

In a function, [lexical scoping](http://en.wikipedia.org/wiki/Lexical_scoping#Lexical_scoping) is used.

```
> ((fn (x y) (+ x y)) 1 2)
3 : java.lang.Integer
> ((fn (x) (* x 2)) 3)
6 : java.lang.Integer
> (setfn sum (x y) (+ x y))
[FN, [x, y], [+, x, y]] : paren$fn
> (sum 1 2)
3 : java.lang.Integer
> (fold sum (range 1 10 1))
55 : java.lang.Integer
> (set even? (fn (x) (== 0 (% x 2))))
[FN, [x], [==, 0, [%, x, 2]]] : paren$fn
> (even? 3)
false : java.lang.Boolean
> (even? 4)
true : java.lang.Boolean
> (apply + (list 1 2 3))
6 : java.lang.Integer
> (map sqrt (list 1 2 3 4))
[1.0, 1.4142135623730951, 1.7320508075688772, 2.0] : java.util.ArrayList
> (filter even? (list 1 2 3 4 5))
[2, 4] : java.util.ArrayList
> (= "abc" "abc") ; Object.equals()
true : java.lang.Boolean
> (set x 1)
  ((fn (x) (prn x) (set x 3) (prn x)) 4) ; lexical scoping
  x    
4
3
1 : java.lang.Integer
> (set adder (fn (amount) (fn (x) (+ x amount)))) ; lexical scoping
  (set add3 (adder 3))
  (add3 4)    
7 : java.lang.Integer
> (cons 1 (list 2 3))
[1, 2, 3] : java.util.ArrayList
```

#### Recursion ####
```
> (set factorial (fn (x) (if (<= x 1) x (* x (factorial (dec x))))))
  (for i 1 5 1 (prn i (factorial i)))
1 1
2 2
3 6
4 24
5 120
 : null
```

### List ###
```
> (nth 1 (list 2 4 6))
4 : java.lang.Integer
> (length (list 1 2 3))
3 : java.lang.Integer
```

### Macro ###
```
> (defmacro infix (a op ...) (op a ...)) (infix 3 + 4 5)
12
```

### Thread ###
```
> (set t1 (thread (for i 1 10 1 (pr "" i)))) (set t2 (thread (for j 11 20 1 (pr "" j)))) (join t1) (join t2)
 1 11 2 12 3  4 5 136 7 8 9  1014 15 16 17 18 19 20 : null
```

### Java interoperability (from Paren) ###
```
> (. javax.swing.JOptionPane showMessageDialog (cast java.awt.Component null) (cast java.lang.Object "Hello, World!")) ; GUI Hello, World!
> (. java.lang.Math random) ; class's static method
0.4780254852371699 : java.lang.Double
> (. java.lang.Math floor 1.5)
1.0 : java.lang.Double
> (. "abc" length) ; object's method
3 : java.lang.Integer
> (. true toString)
true : java.lang.String
> (set i 3)
3 : java.lang.Integer
> (. i doubleValue)
3.0 : java.lang.Double
> (.get java.lang.Math PI) ; get field
3.141592653589793 : java.lang.Double
> (.get parenj testField)
 : null
> (.set parenj testField 1) ; set field
  (.get parenj testField)
1 : java.lang.Integer
> (.set parenj testField "abc")
  (.get parenj testField)
abc : java.lang.String
> (. (new java.math.BigInteger "2") pow 100) ; 2 ^ 100
1267650600228229401496703205376 : java.math.BigInteger
```

#### KOSPI200 Ticker
```
; KOSPI200 Ticker (C) 2013 KIM Taegyoon
(set read-url (fn (address)
  (set url (new java.net.URL address))
  (set stream (. url openStream))
  (set buf (new java.io.BufferedReader (cast java.io.Reader (new java.io.InputStreamReader (cast java.io.InputStream stream)))))
  (set r "")
  (while (! (null? (set s (. buf readLine))))
    (set r (strcat r s "\n")))
  (. buf close)
  r))

(set get-quote (fn ()
  (set text (read-url "http://kosdb.koscom.co.kr/main/jisuticker.html"))
  (set p (. java.util.regex.Pattern compile "KOSPI200.*</font>"))
  (set m (. p matcher (cast java.lang.CharSequence text)))
  (if (. m find) (. m group) "")))

(while true
  (prn (new java.util.Date))
  (prn (get-quote))
  (. java.lang.Thread sleep 2000L))
```

### Java interoperability (from Java) ###
player.java
```
public class player {
    private int life;
    public int getLife() {
        return life;
    }
    public void setLife(int life) {
        this.life = life;
    }
}
```

parenjTest.java
```
import paren.paren;
public class parenjTest {
    public static Object testField;
    public static void main(String[] args) {
        paren p = new paren();
        player pl = new player();
        
        // Method 1: using class's field
        testField = pl;
        p.eval_string("(set pl (.get parenjTest testField))");
        p.eval_string("(. pl setLife 100)");
        System.out.println(p.eval_string("(. pl getLife)").intValue());
        
        // Method 2: not using class's field, set variable to Java's local variable
        p.global_env.env.put("pl2", new paren.node(pl));
        p.eval_string("(. pl2 setLife 200)");
        System.out.println(p.eval_string("(. pl2 getLife)").intValue());
        p.global_env.env.remove("p12"); // remove variable
    }
}

```

### System Command ###
```
(system "notepad" "a.txt")
```

[Project Euler solutions in Paren](https://bitbucket.org/ktg/euler-paren)

### [99 Bottles of Beer](http://en.wikipedia.org/wiki/99_Bottles_of_Beer) ###
```
(for i 99 1 -1
  (prn i "bottles of beer on the wall," i "bottles of beer.")
  (prn "Take one down and pass it around," (dec i) "bottle of beer on the wall."))
```

## Alternative Implementations ##
* [Paren](https://bitbucket.org/ktg/paren) (Paren running natively)
* [ParenJ](https://bitbucket.org/ktg/parenj) (Paren running on the Java Virtual Machine)
* [ParenJS](https://bitbucket.org/ktg/parenjs) (Paren compiler targeting JavaScript)
* [Paren#](https://bitbucket.org/ktg/parensharp) (Paren running on the .Net Framework)

## License ##

   Copyright 2013-2014 Kim, Taegyoon

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
