import java.nio.file.*;
import java.util.*;
import java.io.*;

public class basm {

    static List<String> lines = new ArrayList<>();

    static Map<String, Integer> vars =
        new HashMap<>();

    static int localIndex = 1;

    public static void main(
        String[] args
    ) throws Exception {

        if(args.length == 0) {

            System.out.println(
                "usage: java basm test.bas"
            );

            return;
        }

        String input = args[0];

        String className = input;

        if(className.endsWith(".bas")) {

            className =
                className.substring(
                    0,
                    className.length() - 4
                );
        }

        lines =
            Files.readAllLines(
                Paths.get(input)
            );

        List<String> clean =
            new ArrayList<>();

        // コメント除去
        for(String line : lines) {

            int c =
                line.indexOf("//");

            if(c != -1) {

                line =
                    line.substring(0, c);
            }

            line = line.trim();

            if(line.isEmpty()) continue;

            clean.add(line);
        }

        lines = clean;

        String jasmin =
            compile(className);

        String jFile =
            className + ".j";

        Files.writeString(
            Paths.get(jFile),
            jasmin
        );

        Process p =
            new ProcessBuilder(
                "java",
                "-jar",
                "jasmin.jar",
                jFile
            )
            .inheritIO()
            .start();

        p.waitFor();

        Files.deleteIfExists(
            Paths.get(jFile)
        );

        System.out.println(
            "compiled -> " +
            className +
            ".class"
        );
    }

    static String compile(
        String className
    ) {

        StringBuilder out =
            new StringBuilder();

        out.append(
            ".class public " +
            className +
            "\n"
        );

        out.append(
            ".super java/lang/Object\n\n"
        );

        out.append(
            ".method public static main([Ljava/lang/String;)V\n"
        );

        out.append(
            ".limit stack 1000\n"
        );

        out.append(
            ".limit locals 1000\n\n"
        );

        // Scanner
        out.append(
            "new java/util/Scanner\n"
        );

        out.append(
            "dup\n"
        );

        out.append(
            "getstatic java/lang/System/in Ljava/io/InputStream;\n"
        );

        out.append(
            "invokespecial java/util/Scanner/<init>(Ljava/io/InputStream;)V\n"
        );

        out.append(
            "astore 0\n\n"
        );

        // 命令生成
        for(String line : lines) {

            // ラベル
            if(line.endsWith(":")) {

                String label =
                    line.substring(
                        0,
                        line.length() - 1
                    );

                out.append(
                    label + ":\n"
                );

                continue;
            }

            // let
            if(line.startsWith("let ")) {

                String body =
                    line.substring(4)
                    .trim();

                body =
                    body.substring(
                        0,
                        body.length() - 1
                    );

                String[] defs =
                    body.split(",");

                for(String def : defs) {

                    String[] p =
                        def.split("=");

                    String name =
                        p[0].trim();

                    String expr =
                        p[1].trim();

                    alloc(name);

                    emitExpr(out, expr);

                    out.append(
                        "dstore " +
                        vars.get(name) +
                        "\n"
                    );
                }

                continue;
            }

            // put
            if(line.startsWith("put ")) {

                String body =
                    line.substring(4)
                    .trim();

                body =
                    body.substring(
                        0,
                        body.length() - 1
                    );

                // 文字列
                if(
                    body.startsWith("\"")
                    &&
                    body.endsWith("\"")
                ) {

                    out.append(
                        "getstatic java/lang/System/out Ljava/io/PrintStream;\n"
                    );

                    out.append(
                        "ldc " +
                        body +
                        "\n"
                    );

                    out.append(
                        "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n"
                    );

                } else {

                    // PrintStream
                    out.append(
                        "getstatic java/lang/System/out Ljava/io/PrintStream;\n"
                    );

                    // double値
                    emitExpr(out, body);

                    out.append(
                        "invokevirtual java/io/PrintStream/println(D)V\n"
                    );
                }

                continue;
            }

            // inp
            if(line.startsWith("inp ")) {

                String name =
                    line.substring(4)
                    .trim();

                name =
                    name.substring(
                        0,
                        name.length() - 1
                    );

                alloc(name);

                out.append(
                    "aload 0\n"
                );

                out.append(
                    "invokevirtual java/util/Scanner/nextDouble()D\n"
                );

                out.append(
                    "dstore " +
                    vars.get(name) +
                    "\n"
                );

                continue;
            }

            // jmp
            if(line.startsWith("jmp ")) {

                String label =
                    line.substring(4)
                    .trim();

                label =
                    label.substring(
                        0,
                        label.length() - 1
                    );

                out.append(
                    "goto " +
                    label +
                    "\n"
                );

                continue;
            }

            // if
            if(line.startsWith("if(")) {

                int end =
                    line.indexOf(')');

                String cond =
                    line.substring(
                        3,
                        end
                    );

                String label =
                    line.substring(
                        end + 1
                    ).trim();

                label =
                    label.substring(
                        0,
                        label.length() - 1
                    );

                emitCond(
                    out,
                    cond,
                    label
                );

                continue;
            }

            // 代入
            if(line.contains("=")) {

                line =
                    line.substring(
                        0,
                        line.length() - 1
                    );

                String[] p =
                    line.split("=");

                String name =
                    p[0].trim();

                String expr =
                    p[1].trim();

                alloc(name);

                emitExpr(out, expr);

                out.append(
                    "dstore " +
                    vars.get(name) +
                    "\n"
                );

                continue;
            }

            throw new RuntimeException(
                "unknown syntax: " +
                line
            );
        }

        out.append(
            "return\n"
        );

        out.append(
            ".end method\n"
        );

        return out.toString();
    }

    static void alloc(
        String name
    ) {

        if(!vars.containsKey(name)) {

            vars.put(
                name,
                localIndex
            );

            // doubleは2スロット
            localIndex += 2;
        }
    }

    static void emitExpr(
        StringBuilder out,
        String expr
    ) {

        expr = expr.trim();

        int p;

        // +
        p = expr.lastIndexOf('+');

        if(p > 0) {

            emitExpr(
                out,
                expr.substring(0, p)
            );

            emitExpr(
                out,
                expr.substring(p + 1)
            );

            out.append(
                "dadd\n"
            );

            return;
        }

        // -
        p = expr.lastIndexOf('-');

        if(p > 0) {

            emitExpr(
                out,
                expr.substring(0, p)
            );

            emitExpr(
                out,
                expr.substring(p + 1)
            );

            out.append(
                "dsub\n"
            );

            return;
        }

        // *
        p = expr.lastIndexOf('*');

        if(p > 0) {

            emitExpr(
                out,
                expr.substring(0, p)
            );

            emitExpr(
                out,
                expr.substring(p + 1)
            );

            out.append(
                "dmul\n"
            );

            return;
        }

        // /
        p = expr.lastIndexOf('/');

        if(p > 0) {

            emitExpr(
                out,
                expr.substring(0, p)
            );

            emitExpr(
                out,
                expr.substring(p + 1)
            );

            out.append(
                "ddiv\n"
            );

            return;
        }

        // 数値
        try {

            Double.parseDouble(expr);

double d =
    Double.parseDouble(expr);

String value =
    Double.toString(d);

if(!value.contains(".")) {

    value += ".0";
}

out.append(
    "ldc2_w " +
    value +
    "\n"
);

            return;

        } catch(Exception e) {
        }

        // 変数
        alloc(expr);

        out.append(
            "dload " +
            vars.get(expr) +
            "\n"
        );
    }

    static void emitCond(
        StringBuilder out,
        String cond,
        String label
    ) {

        String op = null;

        if(cond.contains("==")) op = "==";
        else if(cond.contains("!=")) op = "!=";
        else if(cond.contains(">=")) op = ">=";
        else if(cond.contains("<=")) op = "<=";
        else if(cond.contains(">")) op = ">";
        else if(cond.contains("<")) op = "<";

        if(op == null) {

            throw new RuntimeException(
                "invalid condition: " +
                cond
            );
        }

        String[] p =
            cond.split(
                java.util.regex.Pattern.quote(op)
            );

        String left =
            p[0].trim();

        String right =
            p[1].trim();

        emitExpr(out, left);

        emitExpr(out, right);

        out.append(
            "dcmpg\n"
        );

        switch(op) {

            case "==":

                out.append(
                    "ifeq " +
                    label +
                    "\n"
                );

                break;

            case "!=":

                out.append(
                    "ifne " +
                    label +
                    "\n"
                );

                break;

            case ">":

                out.append(
                    "ifgt " +
                    label +
                    "\n"
                );

                break;

            case "<":

                out.append(
                    "iflt " +
                    label +
                    "\n"
                );

                break;

            case ">=":

                out.append(
                    "ifge " +
                    label +
                    "\n"
                );

                break;

            case "<=":

                out.append(
                    "ifle " +
                    label +
                    "\n"
                );

                break;
        }
    }
}
