package serializable;

import core.codemodel.events.Event;

import java.io.Serializable;
import java.util.HashMap;

public record SerialResults(HashMap<Event, Float> data) implements Serializable {
}
