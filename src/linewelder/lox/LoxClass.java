package linewelder.lox;

import java.util.*;

public class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;

    private LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
        super(null);
        this.superclass = superclass;
        this.name = name;
        this.methods = methods;
    }

    LoxClass(String name, LoxClass superclass,
             Map<String, LoxFunction> methods, Map<String, LoxFunction> classMethods) {
        super(new LoxClass(
            name + " metaclass",
            superclass == null ? null : superclass.klass,
            classMethods
        ));
        this.superclass = superclass;
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        final LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final LoxInstance instance = new LoxInstance(this);
        final LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }
}
