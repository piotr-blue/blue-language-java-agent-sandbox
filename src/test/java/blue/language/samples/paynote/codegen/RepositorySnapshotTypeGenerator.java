package blue.language.samples.paynote.codegen;

import blue.language.Blue;
import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RepositorySnapshotTypeGenerator {

    public Map<String, String> generate(String yamlSnapshotContent) {
        Blue blue = new Blue();
        Node snapshot = blue.yamlToNode(yamlSnapshotContent);
        List<Node> typeDefs = snapshot.getAsNode("/types").getItems();

        Map<String, String> generatedByFqn = new LinkedHashMap<String, String>();
        for (Node typeDef : typeDefs) {
            String packageName = typeDef.getAsText("/packageName/value");
            String className = typeDef.getAsText("/className/value");
            String alias = typeDef.getAsText("/alias/value");
            String blueId = typeDef.getAsText("/blueId/value");
            List<Node> fields = typeDef.getAsNode("/fields").getItems();

            String source = generateTypeSource(packageName, className, alias, blueId, fields);
            generatedByFqn.put(packageName + "." + className, source);
        }
        return generatedByFqn;
    }

    private String generateTypeSource(String packageName,
                                      String className,
                                      String alias,
                                      String blueId,
                                      List<Node> fields) {
        StringBuilder source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n");
        source.append("import blue.language.model.TypeBlueId;\n");
        source.append("import blue.language.samples.paynote.dsl.TypeAlias;\n\n");
        source.append("@TypeAlias(\"").append(alias).append("\")\n");
        source.append("@TypeBlueId(\"").append(blueId).append("\")\n");
        source.append("public class ").append(className).append(" {\n");
        for (Node field : fields) {
            String fieldName = field.getAsText("/name/value");
            String javaType = field.getAsText("/javaType/value");
            source.append("    public ").append(javaType).append(" ").append(fieldName).append(";\n");
        }
        source.append("}\n");
        return source.toString();
    }
}
