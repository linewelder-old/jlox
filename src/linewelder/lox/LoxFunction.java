package linewelder.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Token name;
    private final Expr.Function function;

    LoxFunction(Token name, Expr.Function function) {
        this.name = name;
        this.function = function;
    }

    LoxFunction(Expr.Function function) {
        this(null, function);
    }

    @Override
    public int arity() {
        return function.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final Environment environment = new Environment(interpreter.globals);
        for (int i = 0; i < function.params.size(); i++) {
            environment.define(function.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(function.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        if (name != null) {
            return "<fn " + name.lexeme + ">";
        } else {
            return "<anonymous fn>";
        }
    }
}
