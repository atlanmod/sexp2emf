package org.atlanmod.sexp2emf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SexpParser {

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Scanner
  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  static private class CharStream {
    final String input;
    int index;

    CharStream(String input) {
      this.input = input;
      this.index = 0;
    }

    boolean eof() {
      return index >= input.length();
    }

    char peek() {
      return input.charAt(index);
    }

    char next() {
      return input.charAt(index++);
    }

    void expect(char c) {
      if (eof()) {
        throw new ParseException("Expected '%c', got end of input", c);
      }

      char a = next();
      if (a != c) {
        throw new ParseException("Expected '%c', got '%c'", c, a);
      }
    }
  }

  static private enum TokenType {
    EOF,
    LeftParen,
    RightParen,
    LeftBracket,
    RightBracket,
    Pound,
    At,
    Number,
    Keyword,
    Word,
    String,
  }

  static private enum Keyword {
    True,
    False,
  }

  static final Map<String, Keyword> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("true",  Keyword.True);
    keywords.put("false", Keyword.False);
  }

  static private class Token {
    final TokenType type;
    final Object literal;

    Token(TokenType type, Object literal) {
      this.type = type;
      this.literal = literal;
    }

  }

  static private class TokenStream {
    final CharStream input;
    Token current;

    TokenStream(CharStream input) {
      this.input = input;
    }

    boolean eof() {
      return peek().type == TokenType.EOF;
    }

    Token peek() {
      if (current == null) {
        current = advance();
      }
      return current;
    }

    Token next() {
      if (current != null) {
        Token r = current;
        current = null;
        return r;
      } else {
        return advance();
      }
    }

    void eatSpace() {
      while (!input.eof()) {
        char c = input.peek();
        if (c == ' ' || c == '\n' || c == '\t') {
          input.next();
        } else {
          break;
        }
      }
    }

    Token readWord() {
      StringBuilder w = new StringBuilder();

      while (!input.eof()) {
        char c = input.peek();
        if (c == '(' || c == ')' ||
            c == '[' || c == ']' ||
            c == '#' || c == '@' ||
            c == '\'' ||
            c == '"' || c == '\n' ||
            c == ' ' || c == '\t') {
          break;
        } else {
          w.append(input.next());
        }
      }

      String word = w.toString();
      if (keywords.containsKey(word)) {
        return emit(TokenType.Keyword, keywords.get(word));
      } else {
        return emit(TokenType.Word, w.toString());
      }
    }

    Token readNumber() {
      StringBuilder w = new StringBuilder();

      if (input.peek() == '-') {
        w.append(input.next());
      }

      while (!input.eof()) {
        char c = input.peek();
        if (c >= '0' && c <= '9') {
          w.append(input.next());
        } else {
          break;
        }
      }

      return emit(TokenType.Number, Integer.parseInt(w.toString(), 10));
    }

    Token readString(char delimiter) {
      input.expect(delimiter);

      StringBuilder s = new StringBuilder();

      while (!input.eof()) {
        char c = input.peek();
        if (c == delimiter) {
          break;
        } else {
          s.append(input.next());
        }
      }

      input.expect(delimiter);

      return emit(TokenType.String, s.toString());
    }

    Token advance() {
      eatSpace();

      if (input.eof()) {
        return emit(TokenType.EOF);
      }

      switch (input.peek()) {
      case '(':  input.next(); return emit(TokenType.LeftParen);
      case ')':  input.next(); return emit(TokenType.RightParen);
      case '[':  input.next(); return emit(TokenType.LeftBracket);
      case ']':  input.next(); return emit(TokenType.RightBracket);
      case '#':  input.next(); return emit(TokenType.Pound);
      case '@':  input.next(); return emit(TokenType.At);
      case '"':  return readString('"');
      case '\'': return readString('\'');
      case '-':
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        return readNumber();
      default:   return readWord();
      }
    }

    Token emit(TokenType type) {
      return emit(type, null);
    }

    Token emit(TokenType type, Object literal) {
      return new Token(type, literal);
    }

    Token expect(TokenType type) {
      if (eof()) {
        throw new ParseException("Expected '%s', got end of input", type);
      }

      Token t = next();
      if (t.type != type) {
        throw new ParseException("Expected '%s', got '%s'", type, t.type);
      }

      return t;
    }
  }

  static class ParseException extends RuntimeException {
    private static final long serialVersionUID = 8589484277741808200L;

    public ParseException(String msg, Object... args) {
      super(String.format(msg, args));
    }
  }

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Parser
  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  public static Sexp parse(String source) {
    return parseSexp(new TokenStream(new CharStream(source)));
  }

  private static Sexp parseSexp(TokenStream ts) {
    switch (ts.peek().type) {
    case LeftParen:
      return parseList(ts, TokenType.LeftParen, TokenType.RightParen, Call::new);
    case LeftBracket:
      return parseList(ts, TokenType.LeftBracket, TokenType.RightBracket, Node::new);
    case Pound:
      return parseTarget(ts);
    case At:
      return parseRef(ts);
    default:
      return parseAtom(ts);
    }
  }

  private static Sexp parseList(TokenStream t, TokenType open, TokenType close,
                                Function<Sexp[], Sexp> f) {
    t.expect(open);

    List<Sexp> children = new ArrayList<>();
    while (!t.eof()) {
      if (t.peek().type != close) {
        children.add(parseSexp(t));
      } else {
        break;
      }
    }

    t.expect(close);

    return f.apply(children.toArray(new Sexp[0]));
  }

  private static Target parseTarget(TokenStream ts) {
    ts.expect(TokenType.Pound);
    Object id = parseTargetIdentifier(ts);
    return new Target(id, parseSexp(ts));
  }

  private static Ref parseRef(TokenStream ts) {
    ts.expect(TokenType.At);
    Object id = parseTargetIdentifier(ts);
    return new Ref(id);
  }

  private static Object parseTargetIdentifier(TokenStream ts) {
    Token t = ts.next();
    switch (t.type) {
    case Number: case Word:
      return t.literal;
    default: throw new ParseException("Expected number or identifier, got '%s'", t.literal);
    }
  }

  private static Sexp parseAtom(TokenStream ts) {
    Token t = ts.next();

    switch (t.type) {
    case Keyword: {
      switch ((Keyword) t.literal) {
        case True:  return new BoolLiteral(true);
        case False: return new BoolLiteral(false);
      }
    }

    case String: return new StringLiteral((String) t.literal);
    case Number: return new IntLiteral((int) t.literal);
    default: return new Atom((String) t.literal);
    }
  }

  // AST node types

  public interface Sexp {
    <T> T accept(Visitor<T> v);
  }

  public interface Visitor<T> {
    T onNode(Node n);
    T onCall(Call c);
    T onTarget(Target t);
    T onRef(Ref r);
    T onAtom(Atom a);
    T onString(StringLiteral s);
    T onInt(IntLiteral i);
    T onBool(BoolLiteral b);
  }

  public static class Node implements Sexp {
    final Sexp[] children;

    Node(Sexp[] children) {
      this.children = children;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onNode(this);
    }
  }

  public static class Call extends Node {
    Call(Sexp[] children) {
      super(children);
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onCall(this);
    }
  }

  public static class Target implements Sexp {
    final Object id;
    final Sexp sexp;

    Target(Object id, Sexp sexp) {
      this.id = id;
      this.sexp = sexp;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onTarget(this);
    }
  }

  public static class Ref implements Sexp {
    final Object id;

    Ref(Object id) {
      this.id = id;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onRef(this);
    }
  }

  public static class Atom implements Sexp {
    final String value;

    Atom(String value) {
      this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onAtom(this);
    }
  }

  public static class StringLiteral implements Sexp {
    final String value;

    StringLiteral(String value) {
      this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onString(this);
    }
  }

  public static class IntLiteral implements Sexp {
    final int value;

    IntLiteral(int i) {
      value = i;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onInt(this);
    }
  }

  public static class BoolLiteral implements Sexp {
    final boolean value;

    BoolLiteral(boolean b) {
      value = b;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onBool(this);
    }
  }

}
