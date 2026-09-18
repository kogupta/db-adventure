package org.kogu.queryengine.expression;

import org.kogu.queryengine.columnar.RecordBatch;
import org.kogu.queryengine.columnar.Vector;

import java.util.ArrayList;
import java.util.List;

public final class ExprTracer {
    private ExprTracer() {}

    public record TraceStep(
            int stepNumber,
            Expr expr,
            String description,
            List<Integer> inputStepNumbers,
            Vector resultVector
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("Step ").append(stepNumber).append(": ").append(description).append(System.lineSeparator());
            if (!inputStepNumbers.isEmpty()) {
                sb.append("  ↳ Inputs: ")
                        .append(inputStepNumbers.stream().map(s -> "Step " + s).toList())
                        .append(System.lineSeparator());
            }
            sb.append("  ↳ Result: ")
                    .append(resultVector.getClass().getSimpleName())
                    .append(" (").append(resultVector.type()).append(")")
                    .append(", length=").append(resultVector.length())
                    .append(", nulls=").append(formatNulls(resultVector))
                    .append(System.lineSeparator());
            sb.append("  ↳ Values: ").append(formatVectorValues(resultVector));
            return sb.toString();
        }
    }

    public record TraceReport(
            Expr rootExpr,
            RecordBatch batch,
            Vector finalResult,
            List<TraceStep> steps
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================").append(System.lineSeparator());
            sb.append("EVALUATION EXECUTION TRACE").append(System.lineSeparator());
            sb.append("Batch: ").append(batch.rowCount()).append(" rows").append(System.lineSeparator());
            sb.append("Infix: ").append(ExprPrinter.toInfix(rootExpr)).append(System.lineSeparator());
            sb.append("AST:").append(System.lineSeparator());
            sb.append(ExprPrinter.printTree(rootExpr, batch.schema())).append(System.lineSeparator());
            sb.append("--------------------------------------------------------------------------------").append(System.lineSeparator());
            for (TraceStep step : steps) {
                sb.append(step.format()).append(System.lineSeparator()).append(System.lineSeparator());
            }
            sb.append("================================================================================");
            return sb.toString();
        }
    }

    public static TraceReport trace(Expr expr, RecordBatch batch) {
        List<TraceStep> steps = new ArrayList<>();
        Vector finalResult = traceRecursive(expr, batch, steps);
        return new TraceReport(expr, batch, finalResult, List.copyOf(steps));
    }

    private static Vector traceRecursive(Expr expr, RecordBatch batch, List<TraceStep> steps) {
        List<Integer> inputSteps = new ArrayList<>();
        switch (expr) {
            case Expr.ColumnRef _, Expr.Literal _ -> {}
            case Expr.BinaryExpr(var left, _, var right) -> {
                traceRecursive(left, batch, steps);
                int leftStep = steps.size();
                traceRecursive(right, batch, steps);
                int rightStep = steps.size();
                inputSteps.add(leftStep);
                inputSteps.add(rightStep);
            }
            case Expr.UnaryExpr(_, var e) -> {
                traceRecursive(e, batch, steps);
                inputSteps.add(steps.size());
            }
            case Expr.LogicalExpr(var left, _, var right) -> {
                traceRecursive(left, batch, steps);
                int leftStep = steps.size();
                traceRecursive(right, batch, steps);
                int rightStep = steps.size();
                inputSteps.add(leftStep);
                inputSteps.add(rightStep);
            }
        }

        Vector evaluated = ExprEvaluator.eval(expr, batch);
        int stepNum = steps.size() + 1;
        String desc = formatStepDescription(expr);
        steps.add(new TraceStep(stepNum, expr, desc, inputSteps, evaluated));
        return evaluated;
    }

    private static String formatStepDescription(Expr expr) {
        return switch (expr) {
            case Expr.ColumnRef(var name) -> "EVAL ColumnRef(\"" + name + "\")";
            case Expr.Literal lit -> "EVAL Literal: " + ExprPrinter.toInfix(lit);
            case Expr.BinaryExpr(var left, var op, var right) ->
                    "EVAL BinaryExpr " + op.token() + " (" + ExprPrinter.toInfix(left) + " " + op.token() + " " + ExprPrinter.toInfix(right) + ")";
            case Expr.UnaryExpr(var op, var e) ->
                    "EVAL UnaryExpr " + op.name() + " (" + ExprPrinter.toInfix(e) + ")";
            case Expr.LogicalExpr(var left, var op, var right) ->
                    "EVAL LogicalExpr " + op.name() + " (" + ExprPrinter.toInfix(left) + " " + op.name() + " " + ExprPrinter.toInfix(right) + ")";
        };
    }

    private static String formatNulls(Vector vector) {
        List<Integer> nullSlots = new ArrayList<>();
        for (int i = 0; i < vector.length(); i++) {
            if (vector.isNull(i)) {
                nullSlots.add(i);
            }
        }
        if (nullSlots.isEmpty()) {
            return "none";
        }
        return "slots=" + nullSlots + " (count=" + nullSlots.size() + ")";
    }

    private static String formatVectorValues(Vector vector) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < vector.length(); i++) {
            if (vector.isNull(i)) {
                values.add("null");
            } else {
                switch (vector) {
                    case Vector.IntVec v -> values.add(String.valueOf(v.value(i)));
                    case Vector.LongVec v -> values.add(String.valueOf(v.value(i)));
                    case Vector.DoubleVec v -> values.add(String.valueOf(v.value(i)));
                    case Vector.BoolVec v -> values.add(String.valueOf(v.value(i)));
                    case Vector.Utf8Vec v -> values.add("\"" + v.value(i) + "\"");
                }
            }
        }
        return values.toString();
    }
}
