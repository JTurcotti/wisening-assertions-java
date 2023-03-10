package core.codemodel.elements;

import java.io.Serializable;

public record CallInput(Call call, PhiInput input) implements BetaSite, Serializable {
}
