package org.atlanmod.sexp2emf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SexpParser {

  static private class TokenStream {
    private String[] tokens;
    private int index;

    TokenStream(String[] tokens) {
      this.tokens = tokens;
      index = 0;
    }

    boolean eof() {
      return index >= tokens.length;
    }

    String peek() {
      return tokens[index];
    }

    String advance() {
      return tokens[index++];
    }

    void expect(String s) {
      String t = advance();
      if (!t.equals(s)) {
        throw new ParseException("Expected '%s', got '%s'", s, t);
      }
    }
  }

  static class ParseException extends RuntimeException {
    private static final long serialVersionUID = 8589484277741808200L;

    public ParseException(String msg, Object... args) {
      super(String.format(msg, args));
    }
  }

  public static Sexp parse(String source) {
    // Tokenize
    // @Correctness: parens (or brackets) that appear in a string will break this
    // also spaces in string will break!
    List<String> tokens = new ArrayList<>();
    for (String s : source.replaceAll("([()\\[\\]])", " $1 ").split("\\s")) {
      if (s.length() > 0) {
        tokens.add(s);
      }
    }
    TokenStream t = new TokenStream(tokens.toArray(new String[0]));

    // Parse
    return parseSexp(t);
  }

  private static Sexp parseSexp(TokenStream t) {
    switch (t.peek().charAt(0)) {
    case '(':
      return parseList(t, "(", ")", Call::new);
    case '[':
      return parseList(t, "[", "]", Node::new);
    case '#':
      return parseTarget(t);
    case '@':
      return parseRef(t);
    default:
      return parseAtom(t);
    }
  }

  private static Sexp parseList(TokenStream t, String open, String close,
                                Function<Sexp[], Sexp> f) {
    t.expect(open);

    List<Sexp> children = new ArrayList<>();
    while (!t.eof() && !t.peek().equals(close)) {
      children.add(parseSexp(t));
    }

    t.expect(close);

    return f.apply(children.toArray(new Sexp[0]));
  }

  private static Target parseTarget(TokenStream t) {
    String s = t.advance();
    if (!s.startsWith("#")) {
      throw new ParseException("Expected '#', got %s", s.charAt(0));
    }

    int id = Integer.parseInt(s.substring(1));
    return new Target(id, parseSexp(t));
  }

  private static Ref parseRef(TokenStream t) {
    String s = t.advance();
    if (!s.startsWith("@")) {
      throw new ParseException("Expected '@', got %s", s.charAt(0));
    }

    int id = Integer.parseInt(s.substring(1));
    return new Ref(id);
  }

  private static Sexp parseAtom(TokenStream t) {
    String s = t.advance();

    switch (s) {
    case "true": {
      BoolLiteral a = new BoolLiteral();
      a.value = true;
      return a;
    }
    case "false": {
      BoolLiteral a = new BoolLiteral();
      a.value = false;
      return a;
    }
    }

    switch (s.charAt(0)) {
    case '\'':
    case '"': {
      char open = s.charAt(0);
      // @Correctness: this should go into the tokenizer
      if (s.charAt(s.length() - 1) != open) {
        throw new ParseException("Unterminated string literal '%s'", s);
      }
      StringLiteral a = new StringLiteral();
      a.value = s.substring(1, s.length() - 1);
      return a;
    }
    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9':
    case '-': {
      IntLiteral a = new IntLiteral();
      a.value = Integer.parseInt(s);
      return a;
    }
    default: {
      Atom a = new Atom();
      a.value = s;
      return a;
    }
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
    Sexp[] children;

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
    int id;
    Sexp sexp;

    Target(int id, Sexp sexp) {
      this.id = id;
      this.sexp = sexp;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onTarget(this);
    }
  }

  public static class Ref implements Sexp {
    int id;

    Ref(int id) {
      this.id = id;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onRef(this);
    }
  }

  public static class Atom implements Sexp {
    String value;

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onAtom(this);
    }
  }

  public static class StringLiteral implements Sexp {
    String value;

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onString(this);
    }
  }

  public static class IntLiteral implements Sexp {
    int value;

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onInt(this);
    }
  }

  public static class BoolLiteral implements Sexp {
    boolean value;

    @Override
    public <T> T accept(Visitor<T> v) {
      return v.onBool(this);
    }
  }

}
