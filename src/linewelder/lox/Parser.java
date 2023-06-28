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

    List<Stmt> parse(boolean replPrompt) {
        final List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration(replPrompt));
        }

        return statements;
    }

    private Stmt declaration(boolean replPrompt) {
        try {
            if (match(CLASS)) return classDeclaration();
            if (check(FUN) && peekNext().type != LEFT_PAREN) {
                advance();
                return function();
            }
            if (match(VAR)) return varDeclaration();
            return statement(replPrompt);
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        final Token name = consume(IDENTIFIER, "Expect class name.");
        Expr.Variable superclass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(previous());
        }

        consume(LEFT_BRACE, "Expect '{' before class body.");

        final List<Stmt.Method> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            final boolean isClass = match(CLASS);
            methods.add(method(isClass));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt.Method method(boolean isClass) {
        final String kind = isClass ? "class method" : "method";

        final Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' before " + kind + " parameters.");
        final List<Token> parameters = parameterList();

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        final List<Stmt> body = block();
        return new Stmt.Method(name, new Expr.Function(parameters, body), isClass);
    }

    private Stmt varDeclaration() {
        final Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer =  null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement(boolean replPrompt) {
        if (match(BREAK)) return breakStatement();
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement(replPrompt);
    }

    private Stmt breakStatement() {
        final Token token = previous();
        consume(SEMICOLON, "Expect ';' after 'break'.");

        return new Stmt.Break(token);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");
        final Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement(false);
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement(false);
        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        final Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        final Stmt thenBranch = statement(false);
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement(false);
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        final Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        final Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        final Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        final Stmt body = statement(false);

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement(boolean replPrompt) {
        final Expr expr = expression();
        if (match(SEMICOLON)) {
            return new Stmt.Expression(expr);
        }

        if (!replPrompt) {
            throw error(peek(), "Expect ';' after expression.");
        }

        if (isAtEnd()) {
            return new Stmt.Print(expr);
        }

        throw error(peek(), "Unexpected token after expression.");
    }

    private Stmt.Function function() {
        final Token name = consume(IDENTIFIER, "Expect function name.");
        final Expr.Function function = anonymousFunction("function");
        return new Stmt.Function(name, function);
    }

    private Expr.Function anonymousFunction(String kind) {
        consume(LEFT_PAREN, "Expect '(' before " + kind + " parameters.");
        final List<Token> parameters = parameterList();

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        final List<Stmt> body = block();
        return new Expr.Function(parameters, body);
    }

    private List<Token> parameterList() {
        final List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() == 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        return parameters;
    }

    private List<Stmt> block() {
        final List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration(false));
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        final Expr expr = ternary();

        if (match(EQUAL)) {
            final Token equals = previous();
            final Expr value = assignment();
            if (expr instanceof Expr.Variable name) {
                return new Expr.Assign(name.name, value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr ternary() {
        final Expr expr = or();
        if (match(QUESTION)) {
            final Expr ifTrue = or();
            consume(COLON, "Expect ':' between expressions.");
            final Expr ifFalse = ternary();
            return new Expr.Ternary(expr, ifTrue, ifFalse);
        }

        return expr;
    }

    private Expr or() {
        return binary(Expr.Logical::new, this::and, OR);
    }

    private Expr and() {
        return binary(Expr.Logical::new, this::equality, AND);
    }

    private Expr equality() {
        return binary(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return binary(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        if (match(PLUS)) {
            error(previous(), "Lox does not support unary '+'.");
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

    private interface BinaryFactory {
        Expr create(Expr left, Token operator, Expr right);
    }

    private Expr binary(BinaryFactory factory, Supplier<Expr> operand, TokenType... operators) {
        if (match(operators)) {
            error(previous(), "Is a binary operation, left operand missing.");
        }

        Expr expr = operand.get();
        while (match(operators)) {
            final Token operator = previous();
            final Expr right = operand.get();
            expr = factory.create(expr, operator, right);
        }

        return expr;
    }

    private Expr binary(Supplier<Expr> operand, TokenType... operators) {
        return binary(Expr.Binary::new, operand, operators);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            final Token operator = previous();
            final Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                final Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        final List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() == 255) {
                    error(peek(), "Can't have more than 255.arguments");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        final Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            final Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(FUN)) {
            return anonymousFunction("anonymous function");
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

    private Token peekNext() {
        if (tokens.size() < current + 2) return tokens.get(tokens.size() - 1);
        return tokens.get(current + 1);
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
                case CLASS, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
                case FUN -> {
                    if (peekNext().type == IDENTIFIER) return;
                }
            }

            advance();
        }
    }
}
