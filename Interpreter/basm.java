import java.nio.file.*;
import java.util.*;
import java.io.*;

public class basm {

    static Map<String, Double> vars = new HashMap<>();
    static Map<String, Integer> labels = new HashMap<>();
    static List<String> lines = new ArrayList<>();

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        if(args.length == 0) {
            System.out.println("usage: java basm file.bas");
            return;
        }

        lines = Files.readAllLines(Paths.get(args[0]));

        // 空行・コメント削除
        List<String> clean = new ArrayList<>();

        for(String line : lines) {

            line = line.trim();

            if(line.isEmpty()) continue;

            int comment = line.indexOf("//");
            if(comment != -1) {
                line = line.substring(0, comment).trim();
            }

            if(line.isEmpty()) continue;

            clean.add(line);
        }

        lines = clean;

        // ラベル収集
        for(int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);

            if(line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                labels.put(label, i);
            }
        }

        // 実行
        int pc = 0;

        while(pc < lines.size()) {

            String line = lines.get(pc).trim();

            // ラベル
            if(line.endsWith(":")) {
                pc++;
                continue;
            }

            // let
            if(line.startsWith("let ")) {

                String body = line.substring(4).trim();

                if(body.endsWith(";")) {
                    body = body.substring(0, body.length() - 1);
                }

                String[] defs = body.split(",");

                for(String def : defs) {

                    String[] parts = def.split("=");

                    String name = parts[0].trim();
                    double value = eval(parts[1].trim());

                    vars.put(name, value);
                }

                pc++;
                continue;
            }

            // put
            if(line.startsWith("put ")) {

                String body = line.substring(4).trim();

                if(body.endsWith(";")) {
                    body = body.substring(0, body.length() - 1);
                }

                if(body.startsWith("\"") && body.endsWith("\"")) {

                    System.out.println(
                        body.substring(1, body.length() - 1)
                    );

                } else {

                    System.out.println(eval(body));
                }

                pc++;
                continue;
            }

            // inp
            if(line.startsWith("inp ")) {

                String name = line.substring(4).trim();

                if(name.endsWith(";")) {
                    name = name.substring(0, name.length() - 1);
                }

                double value = sc.nextDouble();

                vars.put(name, value);

                pc++;
                continue;
            }

            // jmp
            if(line.startsWith("jmp ")) {

                String label = line.substring(4).trim();

                if(label.endsWith(";")) {
                    label = label.substring(0, label.length() - 1);
                }

                pc = labels.get(label);
                continue;
            }

            // if
            if(line.startsWith("if(")) {

                int end = line.indexOf(')');

                String cond = line.substring(3, end).trim();

                String label = line.substring(end + 1).trim();

                if(label.endsWith(";")) {
                    label = label.substring(0, label.length() - 1);
                }

                if(check(cond)) {
                    pc = labels.get(label);
                } else {
                    pc++;
                }

                continue;
            }

            // 代入
            if(line.contains("=")) {

                if(line.endsWith(";")) {
                    line = line.substring(0, line.length() - 1);
                }

                String[] parts = line.split("=");

                String name = parts[0].trim();
                double value = eval(parts[1].trim());

                vars.put(name, value);

                pc++;
                continue;
            }

            throw new RuntimeException("Unknown syntax: " + line);
        }
    }

    // 条件式
    static boolean check(String cond) {

        if(cond.contains("==")) {

            String[] p = cond.split("==");
            return eval(p[0]) == eval(p[1]);
        }

        if(cond.contains("!=")) {

            String[] p = cond.split("!=");
            return eval(p[0]) != eval(p[1]);
        }

        if(cond.contains(">=")) {

            String[] p = cond.split(">=");
            return eval(p[0]) >= eval(p[1]);
        }

        if(cond.contains("<=")) {

            String[] p = cond.split("<=");
            return eval(p[0]) <= eval(p[1]);
        }

        if(cond.contains(">")) {

            String[] p = cond.split(">");
            return eval(p[0]) > eval(p[1]);
        }

        if(cond.contains("<")) {

            String[] p = cond.split("<");
            return eval(p[0]) < eval(p[1]);
        }

        return false;
    }

    // 式評価
    static double eval(String expr) {

        expr = expr.trim();

        // +
        int p = expr.lastIndexOf('+');

        if(p > 0) {
            return eval(expr.substring(0, p))
                 + eval(expr.substring(p + 1));
        }

        // -
        p = expr.lastIndexOf('-');

        if(p > 0) {
            return eval(expr.substring(0, p))
                 - eval(expr.substring(p + 1));
        }

        // *
        p = expr.lastIndexOf('*');

        if(p > 0) {
            return eval(expr.substring(0, p))
                 * eval(expr.substring(p + 1));
        }

        // /
        p = expr.lastIndexOf('/');

        if(p > 0) {
            return eval(expr.substring(0, p))
                 / eval(expr.substring(p + 1));
        }

        // 数値
        try {
            return Double.parseDouble(expr);
        } catch(Exception e) {
        }

        // 変数
        if(vars.containsKey(expr)) {
            return vars.get(expr);
        }

        throw new RuntimeException("Unknown expr: " + expr);
    }
}
