package blue.language.blueid.v2;

import java.util.Map;

public interface BlueIdIndex {

    String blueIdAt(String jsonPointer);

    Map<String, String> asMap();
}
