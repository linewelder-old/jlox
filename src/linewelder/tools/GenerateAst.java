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

            writer.println("}");
        }
    }
}
