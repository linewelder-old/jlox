package linewelder.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Token name;
    private final Expr.Function function;
    private final Environment closure;

    private final boolean isInitializer;

    LoxFunction(Token name, Expr.Function function, Environment closure, boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.name = name;
        this.function = function;
        this.closure = closure;
    }

    LoxFunction(Token name, Expr.Function function, Environment closure) {
        this(name, function, closure, false);
    }

    LoxFunction(Expr.Function function, Environment closure) {
        this(null, function, closure);
    }

    LoxFunction bind(LoxInstance instance) {
        final Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(name, function, environment, isInitializer);
    }

    @Override
    public int arity() {
        return function.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final Environment environment = new Environment(closure);
        for (int i = 0; i < function.params.size(); i++) {
            environment.define(function.params.get(i).lexeme, arguments.get(i));
        }

        Object result = null;
        try {
            interpreter.executeBlock(function.body, environment);
        } catch (Return returnValue) {
            result = returnValue.value;
        }

        if (isInitializer) result = closure.getAt(0, "this");
        return result;
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
