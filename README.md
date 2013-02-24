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
Predefined Symbols:
 E PI false true
Functions:
 ! != % && * + ++ - -- /
 < <= == > >= ^ apply ceil char-at chr
 dec double eval exit filter floor fn for if inc
 int list ln log10 map pr prn quote rand range
 read-string set sqrt strcat string strlen type when while ||

Etc.:
 (list) [string] ; end-of-line comment
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
> (apply + (list 1 2 3))
  
6 : class java.lang.Integer
> (map sqrt (list 1 2 3 4))
  
[1.0, 1.4142135623730951, 1.7320508075688772, 2.0] : class java.util.Vector
> (filter even? (list 1 2 3 4 5))
  
[2, 4] : class java.util.Vector
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

```
(apply + (filter (fn (x) (|| (== 0 (% x 3)) (== 0 (% x 5)))) (range 1 999 1)))
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

## License ##

   Copyright 2013 Kim, Taegyoon

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
