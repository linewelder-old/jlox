package linewelder.lox;

import java.util.*;
import java.util.function.Supplier;

import static linewelder.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return ternary();
    }

    private Expr ternary() {
        final Expr expr = equality();
        if (match(QUESTION)) {
            final Expr ifTrue = equality();
            consume(COLON, "Expect ':' between expressions.");
            final Expr ifFalse = ternary();
            return new Expr.Ternary(expr, ifTrue, ifFalse);
        }

        return expr;
    }

    private Expr equality() {
        return binary(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return binary(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        if (match(PLUS)) {
            error(peek(), "Lox does not support unary '+'.");
        }

        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            final Token operator = previous();
            final Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        return binary(this::unary, SLASH, STAR);
    }

    private Expr binary(Supplier<Expr> operand, TokenType... operators) {
        if (match(operators)) {
            error(peek(), "Is a binary operation, left operand missing.");
        }

        Expr expr = operand.get();
        while (match(operators)) {
            final Token operator = previous();
            final Expr right = operand.get();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            final Token operator = previous();
            final Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            final Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (final TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while(!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
            }

            advance();
        }
    }
}
