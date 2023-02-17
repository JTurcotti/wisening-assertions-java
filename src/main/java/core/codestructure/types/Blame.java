package core.codestructure.types;

import java.util.HashMap;
import java.util.Map;

public class Blame {
    public interface Site {}
    private final Map<Site, Intraflow> data = new HashMap<>();
}
