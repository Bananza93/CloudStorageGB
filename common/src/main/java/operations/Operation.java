package operations;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuthOperation.class, name = "authOp"),
        @JsonSubTypes.Type(value = FileOperation.class, name = "fileOp")
})
public class Operation {

    private OperationType type;

    public Operation() {
    }

    public Operation(OperationType type) {
        this.type = type;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }
}
