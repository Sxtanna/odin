

Example
-------
##### Keywords
```kotlin
type ({Expr})      // returns the type of the result of the expression

// standard out
push {Expr}  

// standard in
pull               
pull::[{Type}]     // pull a type 
pull({Txt Prompt}) // pull with a prompt

pull::[{Type}]({Txt Prompt})

// properties
var               // mutable reference
val               // immutable reference

// functions
fun               // function

// types
trait             // interface like type
class             // a class...

// flow control

when ({Bit Expr}) // evaluator
else              // failing case of when

loop ({Bit Expr}) // repeats a block of code
stop              // cancels the repetition of a loop

redo              // reruns the entire file
redo {Int}        // ^ that but a certain amount of times
```

##### Property
```kotlin
val sansType = 10

val withType: Int = 20
```

##### Function
```kotlin
fun basic {
    push "Basic Function"
}

fun param(var0: Int) {
    
}

fun params(var0: Int, var1: Txt) {

}

fun multiParams(var0, var1: Int) {

}

fun returns: Txt {
    => "Hey"
}
```

##### Flow Control
```kotlin
// when

val num0 = pull::[Int]("1st number: ")
val num1 = pull::[Int]("2nd number: ")

when (num0 > num1) {
    push "First number is larger"
}

when (num1 > num0) {
    push "Second number is larger"
} else {
    push "First number is equal or less"
}


// loop

var count0 = 0

loop ((count0 = count0 + 1) < 10) {
    push "Count = " + count0
}

var count1 = 0

loop (true)
{
    push "Count = " + count1

    when((count1 = count1 + 1) > 10)
    {
        stop
    }
}

redo 2 // this entire thing will run 2 more times
```