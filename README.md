# Parenj: The Paren Programming Language written in Java #

(C) 2013 Kim, Taegyoon

## Compile ##
```
cd src
javac parenj.java
```

## Run ##
```
cd src
java parenj
```

## Reference ##
```
Parenj (C) 2013 Kim, Taegyoon
Press Enter key twice to evaluate.
Constants:
true false e pi
Functions:
+ - * / ^ % sqrt inc dec ++ -- floor ceil ln log10 rand
== != < > <= >= && || !
if when for while
'string [string] strlen strcat char-at chr
int double string read-string type set
(list) eval quote fn
pr prn exit
; end-of-line comment
```

## Examples ##
### Hello, World! ###
```
(prn [Hello, World!])
```

### Function ###
```
> ((fn (x y) (+ x y)) 1 2)

3 : class java.lang.Integer
> ((fn (x) (* x 2)) 3)

6 : class java.lang.Integer
> (set sum (fn (x y) (+ x y)))

 : null
> sum

[fn, [x, y], [+, x, y]] : class java.util.Vector
> (sum 1 2)

3 : class java.lang.Integer
> (set even? (fn (x) (== 0 (% x 2))))

 : null
> (even? 3)

false : class java.lang.Boolean
> (even? 4)

true : class java.lang.Boolean
```

#### Recursion ####
```
> (set factorial (fn (x) (if (<= x 1) x (* x (factorial (dec x))))))

 : null
> (for i 1 5 1 (prn i (factorial i)))

1 1
2 2
3 6
4 24
5 120
 : null
```

In a function, you cannot change outer environment.

### [Project Euler Problem 1](http://projecteuler.net/problem=1) ###
```
(set s 0)
(for i 1 999 1
    (when (|| (== 0 (% i 3)) (== 0 (% i 5)))
        (set s (+ s i))))
(prn s)
```
=> 233168

### [Project Euler Problem 2](http://projecteuler.net/problem=2) ###
```
(set a 1)
(set b 1)
(set sum 0)
(while (<= a 4000000)
  (set c (+ a b))
  (set a b)
  (set b c)
  (when (== 0 (% a 2))
    (set sum (+ sum a))))
(prn sum)
```
=> 4613732

### [Project Euler Problem 4](http://projecteuler.net/problem=4) ###
```
(set maxP 0)
(for i 100 999 1
  (for j 100 999 1	
    (set p (* i j))
    (set ps (string p))
    (set len (strlen ps))
    (set to (/ len 2))
    (set pal true)
    (set k 0)
    (set k2 (dec len))
    (while
      (&& (< k to) pal)
	  (when (!= (char-at ps k) (char-at ps k2))
		(set pal false))
	  (++ k)
	  (-- k2))
	(when pal
	  (when (> p maxP)
		(set maxP p)))))
(prn maxP)
```
=> 906609

### [99 Bottles of Beer](http://en.wikipedia.org/wiki/99_Bottles_of_Beer) ###
```
(for i 99 1 -1
  (pr i)
  (pr [ bottles of beer on the wall, ])
  (pr i)
  (prn [ bottles of beer.])
  (pr [Take one down and pass it around, ])
  (pr (dec i))
  (prn [ bottle of beer on the wall.]))
```
