package blue.language.samples.paynote.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class JsProgram {

    private final String code;

    private JsProgram(String code) {
        this.code = code;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String code() {
        return code;
    }

    public static final class Builder {
        private final List<String> lines = new ArrayList<String>();
        private int indentLevel;

        public Builder line(String line) {
            StringBuilder prefixed = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                prefixed.append("  ");
            }
            prefixed.append(line);
            lines.add(prefixed.toString());
            return this;
        }

        public Builder blank() {
            lines.add("");
            return this;
        }

        public Builder lines(String... newLines) {
            if (newLines == null) {
                return this;
            }
            for (String line : newLines) {
                line(line);
            }
            return this;
        }

        public Builder block(String header, Consumer<Builder> customizer) {
            line(header + " {");
            indentLevel++;
            customizer.accept(this);
            indentLevel--;
            line("}");
            return this;
        }

        public Builder ifBlock(String condition, Consumer<Builder> customizer) {
            return block("if (" + condition + ")", customizer);
        }

        public Builder ifElseBlock(String condition,
                                   Consumer<Builder> thenCustomizer,
                                   Consumer<Builder> elseCustomizer) {
            line("if (" + condition + ") {");
            indentLevel++;
            thenCustomizer.accept(this);
            indentLevel--;
            line("} else {");
            indentLevel++;
            elseCustomizer.accept(this);
            indentLevel--;
            line("}");
            return this;
        }

        public Builder constVar(String name, String expression) {
            return line("const " + name + " = " + expression + ";");
        }

        public Builder readRequest(String variableName) {
            return constVar(variableName, JsCommon.readRequest());
        }

        public Builder safeNumber(String variableName, String rawExpression, String fallbackExpression) {
            return constVar(variableName, JsCommon.safeNumber(rawExpression, fallbackExpression));
        }

        public Builder returnStatement(String expression) {
            return line("return " + expression + ";");
        }

        public Builder returnObject(JsObjectBuilder objectBuilder) {
            return returnStatement(objectBuilder.build());
        }

        public Builder returnOutput(JsOutputBuilder outputBuilder) {
            return returnStatement(outputBuilder.build());
        }

        public JsProgram build() {
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                codeBuilder.append(lines.get(i));
                if (i < lines.size() - 1) {
                    codeBuilder.append('\n');
                }
            }
            return new JsProgram(codeBuilder.toString());
        }
    }
}
