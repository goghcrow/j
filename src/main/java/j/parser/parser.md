https://en.wikipedia.org/wiki/Lexical_analysis
https://en.wikipedia.org/wiki/Lexical_grammar
https://en.wikipedia.org/wiki/Compiler#Front_end


```
参考 MDN 裁剪写修改算符优先级
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Operator_Precedence
使用 https://ozh.github.io/ascii-tables/ 绘制

+------------+---------------------------------+---------------+----------------------+
| Precedence |          Operator type          | Associativity | Individual operators |
+------------+---------------------------------+---------------+----------------------+
|        240 | Grouping                        | n/a           | ( … )                |
|        230 | Member Access                   | left-to-right | … . …                |
|            | Computed Member Access          | left-to-right | … [ … ]              |
|            | (不是Operator) ArrowFunction     |               | … -> …               |
|            | Optional chaining               | left-to-right | ?.                   |
|        220 | Function Call                   | left-to-right | … ( … )              |
|        210 | Postfix Increment               | n/a           | … ++                 |
|            | Postfix Decrement               |               | … --                 |
|        200 | Logical NOT                     | right-to-left | ! …                  |
|            | Bitwise NOT                     |               | ~ …                  |
|            | Unary Plus                      |               | + …                  |
|            | Unary Negation                  |               | - …                  |
|            | Prefix Increment                |               | ++ …                 |
|            | Prefix Decrement                |               | -- …                 |
|            | typeof                          |               | typeof …             |
|            | delete                          |               | delete …             |
|        190 | Exponentiation                  | right-to-left | … ** …               |
|        180 | Multiplication                  | left-to-right | … * …                |
|            | Division                        |               | … / …                |
|            | Modulo                          |               | … % …                |
|        170 | Addition                        | left-to-right | … + …                |
|            | Subtraction                     |               | … - …                |
|        160 | Bitwise Left Shift              | left-to-right | … << …               |
|            | Bitwise Right Shift             |               | … >> …               |
|            | Bitwise Unsigned Right Shift    |               | … >>> …              |
|        150 | Less Than                       | left-to-right | … < …                |
|            | Less Than Or Equal              |               | … <= …               |
|            | Greater Than                    |               | … > …                |
|            | Greater Than Or Equal           |               | … >= …               |
|            | in                              |               | … in …               |
|            | instanceof                      |               | … instanceof …       |
|        140 | Equality                        | left-to-right | … == …               |
|            | Inequality                      |               | … != …               |
|            | Strict Equality                 |               | … === …              |
|            | Strict Inequality               |               | … !== …              |
|        130 | Bitwise AND                     | left-to-right | … & …                |
|        120 | Bitwise XOR                     | left-to-right | … ^ …                |
|        110 | Bitwise OR                      | left-to-right | … | …                |
|        100 | Logical AND                     | left-to-right | … && …               |
|         90 | Logical OR                      | left-to-right | … || …               |
|         80 | Conditional                     | right-to-left | … ? … : …            |
|         80 | (不是Operator) if then else     |               |                      |
|         70 | Assignment                      | right-to-left | … = …                |
|            |                                 |               | … += …               |
|            |                                 |               | … -= …               |
|            |                                 |               | … **= …              |
|            |                                 |               | … *= …               |
|            |                                 |               | … /= …               |
|            |                                 |               | … %= …               |
|            |                                 |               | … <<= …              |
|            |                                 |               | … >>= …              |
|            |                                 |               | … >>>= …             |
|            |                                 |               | … &= …               |
|            |                                 |               | … ^= …               |
|            |                                 |               | … |= …               |
|         60 | yield                           | right-to-left | yield …              |
|         50 | Comma / Sequence                | left-to-right | … , …                |
|          0 | non-binding operators n/a   … ; |               |                      |
+------------+---------------------------------+---------------+----------------------+
240     Grouping
230     New / Member
220     Call
210     Increment / Decrement
200     LogicalNot / BitwiseNot / UnaryPlus / UnaryMinus / TypeOf / Void / Delete
190     Exponentiation
180     Multiply  Divide  Modulo
170     Add / Subtract
160     BitwiseShift
150     Relational / In / InstanceOf
140     Equality / Inequality / StrictEquality / StrictInequality
130     BitwiseAnd
120     BitwiseXor
110     BitwiseOr
100     LogicalAnd
90      LogicalOr
80      Condition
70      Assignment
60      Yield
50      Comma,Sequence
0       Non-binding operators
```


一个 lexeme 只能对应一个 tokenType ??? ，比如 '(' 可能是 group 或者 call，比如 - 可能是 neg 或者 minus

```
https://stackoverflow.com/questions/30284394/what-is-the-difference-between-lexical-grammar-and-syntactic-grammar

Semantics:
Semantics is the study of meaning.

Meaning:
Meaning, in semantics, is defined as being Extension: The thing in the world that the word/phrase refers to, plus Intention: The concepts/mental images that the word/phrase evokes.

Syntax:
Syntax is all about the structure of sentences, and what determines which words go where.

Production:
A production or production rule in computer science is a rewrite rule specifying a symbol substitution that can be recursively performed to generate new symbol sequences.

Alphabet:
A non-empty set is called alphabet when its intended use in string operations shall be indicated.

Lexeme:
A lexeme is a string of characters which forms a syntactic unit.

Syntactic unit:
Sentence is the "highest" (i.e., largest) syntactic unit,
the lowest (i.e., smallest) syntactic units are words,
the intermediate syntactic units are the phrases.

Token:
A token is a structure representing a lexeme that explicitly indicates its categorization for the purpose of parsing.

Grammar:
A grammar (when the context is not given, often called a formal grammar for clarity) is a set of production rules for strings in a formal language. The rules describe how to form strings from the language's alphabet that are valid according to the language's syntax. A formal grammar is a set of rules for rewriting strings, along with a "start symbol" from which rewriting starts.

Lexical grammar:
A lexical grammar is a formal grammar defining the syntax of tokens.
```


```java
/**
 * @author chuxiaofeng
 */
public class LineEater extends Lexer.TokenIterator<TokenType> {
    boolean eatLines;

    public LineEater(@NotNull Lexer<TokenType> lexer) {
        super(lexer);
    }

    @Override
    public boolean hasNext() {
        // 因为最后有个 EOF, 不会存在 hasNext 等于 true 但是 next 获取不到数据的情况
        return super.hasNext();
    }

    @Override
    public Token<TokenType> next() {
        return super.next();
//            while (true) {
//                Token<TokenType> tok = super.next();
//                switch (tok.type) {
//                    // 以下 token 的后的换行忽略...
//                    case COMMA:
//                    case DOT:
//                    case LEFT_PAREN:
//                    case LEFT_BRACKET:
//                    case LEFT_BRACE:
//                    case MUL:
//                    case DIV:
//                    case MOD:
//                    case PLUS:
//                    case MINUS:
//                    case LT:
//                    case GT:
//                    case LE:
//                    case GE:
//                    case ASSIGN:
//                    case EQ:
//                    case NE:
//                    case LOGIC_AND:
//                    case LOGIC_OR:
//                        eatLines = true;
//                        break;
//
//                    case NEWLINE:
//                        if (eatLines) {
//                            continue;
//                        } else {
//                            eatLines = true;
//                            break;
//                        }
//                    default:
//                        eatLines = false;
//                        break;
//                }
//
//                return tok;
//            }
    }
}
```