package blue.language.processor.model.examples;

import blue.language.model.BlueDescription;
import blue.language.model.BlueId;
import blue.language.model.BlueName;
import blue.language.model.BlueType;

import java.util.List;

@BlueType("Example.Document.Base")
public class ExampleDocumentBase {
    @BlueId
    public String template;
    @BlueName("lineItems")
    public String itemsName;
    @BlueDescription("lineItems")
    public String itemsDescription;
    public List<String> lineItems;
    public String customer;
}
