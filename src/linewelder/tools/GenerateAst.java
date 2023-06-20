package linewelder.tools;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        final String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Unary    : Token operator, Expr right"
        ));
    }

    private static void defineAst(
        String outputDir, String baseName, List<String> types
    ) throws IOException {
        final Path path = Paths.get(outputDir, baseName + ".java");
        try (PrintWriter writer = new PrintWriter(path.toString(), StandardCharsets.UTF_8)) {
            writer.println("package linewelder.lox;");
            writer.println();
            writer.println("abstract class " + baseName + " {");

            defineVisitor(writer, baseName, types);

            for (final String type : types) {
                final String className = type.split(":")[0].trim();
                final String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }

            writer.println();
            writer.println("    abstract <R> R accept(Visitor<R> visitor);");

            writer.println("}");
        }
    }

    private static void defineVisitor(
        PrintWriter writer, String baseName, List<String> types
    ) {
        writer.println("    interface Visitor<R> {");

        for (final String type : types) {
            final String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println(" }");
    }

    private static void defineType(
        PrintWriter writer, String baseName,
        String className, String fieldList
    ) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        final String[] fields = fieldList.split(", ");
        for (final String field : fields) {
            writer.println("        final " + field + ";");
        }
        writer.println();

        writer.println("        " + className + " (" + fieldList + ") {");
        for (final String field : fields) {
            final String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");
        writer.println();

        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
    }
}
