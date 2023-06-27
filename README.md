# jlox

A Lox interpreter made along with Robert Nystrom's wonderful book
["Crafting Interpreters"](http://craftinginterpreters.com).

## Differences from the original version

The interpreter supports features that were suggested to be added in the Challenges sections.

- Dividing by zero is an error

    ```javascript
    1 / 0; // Error!
    ```

- Accessing an uninitialized variable is an error

    ```javascript
    var a;
    print a; // Error!
    ```

- Not using a local variable is an error

    ```javascript
    {
        var a = "unused";
        // Error!
    }
    ```

- C-style comments

    ```javascript
    /* This is a comment. */
    ```

- The ternary operator

    ```javascript
    print 2 == 3 ? "How?" : "Nice"; // Prints "Nice".
    ```

- Concatenating strings with other types

    ```javascript
    print "Hello " + 123; // Prints "Hello 123".
    ```

- The REPL accepts raw expressions and prints their values

    ```javascript
    > 2 + 3
    5
    ```

- Break statements

    ```javascript
    // Prints "4".
    for (var i = 0; i < 10; i = i + 1) {
        if (i * i == 16) {
            print i;
            break;
        }
    }
    ```

- Anonymous functions

    ```javascript
    var hello = fun() {
        print "Hello";
    };
    hello(); // Prints "Hello".
    ```

- Class methods

    ```javascript
    class Test {
        class greet() {
            print "Hello!";
        }
    }
    Test.greet(); // Prints "Hello!".
    ```
