# KLox

KLox is an interpreter written by me in kotlin, for a programming language called lox. lox is a language designed for the book [crafting interpreters](https://craftinginterpreters.com/) by Bob Nystrom.

The interpreter is a tree-walk interpreter, and uses recursive decent parsing. In addition to what lox offers, I added the following features:
escape characters in strings, such as \n, \\, \", etc. 
int, float types, power operator (e.g 2\*\*5).
builtin functions: print(instead of a statement), readline, str, int, float, type.

I also improved the lexer to support statements that end with '{'.
I intend to add more features to this interprer in the future, i will update this readme appropriately.

## Usage

Windows:
```bat
gradlew.bat build clean
gradlew.bat build
cd build\libs
java -jar KLox-1.0.jar
```

Linux:
```bash
./gradlew build clean
./gradlew build
cd build/libs
java -jar KLox-1.0.jar
```
## Examples
### Closures
```lox
fun makeCounter() {
  var i = 0;
  fun count() {
    i = i + 1;
    print(i);
  }

  return count;
}

var counter = makeCounter();
counter(); // 1
counter(); // 2
```
### Recursion
```lox
fun fib(n)
{
  if (n <= 1)
    return n;
  return fib(n - 2) + fib(n - 1);
}

for (var i = 0; i < 30; i = i + 1)
  print(fib(i));
```
### Classes and inheritance
```lox
class UseNum{
  init(num){
    this.num = num;
  }
  
  use(){}
}

class UseNumPrint < UseNum{
    use(){
        print(this.num);
    }  
}

class UseNumMultiply < UseNum{
    use(otherNum){
        return this.num * otherNum;
    }
}

UseNumPrint(123).use();
print(UseNumMultiply(111).use(3));
```

## Try it using repl.it (Please go to https://erez-shlingbaum.github.io/KLox/)
<iframe height="400px" width="100%" src="https://repl.it/@ErezShlingbaum/KLox?lite=true" scrolling="no" frameborder="no" allowtransparency="true" allowfullscreen="true" sandbox="allow-forms allow-pointer-lock allow-popups allow-same-origin allow-scripts allow-modals"></iframe>

## License
[MIT](https://choosealicense.com/licenses/mit/)
