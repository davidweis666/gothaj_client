package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import team.gothaj.client.api.util.Logger;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ListProperty extends Property<List<String>> {

    private final LinkedHashMap<String, Boolean> choices = new LinkedHashMap<>();
    public boolean expanded = false;

    public ListProperty(String name, String... options) {
        super(name, new ArrayList<>());
        for (String option : options) {
            choices.put(option, false);
        }
    }

    @Override
    public List<String> getProperty() {
        return choices.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void setProperty(List<String> newValue) {
        choices.keySet().forEach(key -> choices.put(key, false));

        for (String key : newValue) {
            if (choices.containsKey(key)) {
                choices.put(key, true);
            }
        }
    }


    public void toggleOption(String key) {
        if (!choices.containsKey(key)) {
            Logger.log("Couldn't find option '" + key + "' for property " + getName());
            return;
        }
        choices.put(key, !choices.get(key));
    }

    public void setOptionState(String key, boolean state) {
        if (!choices.containsKey(key)) {
            Logger.log("Couldn't find option '" + key + "' for property " + getName());
            return;
        }
        choices.put(key, state);
    }

    public boolean getOptionState(String key) {
        return choices.getOrDefault(key, false);
    }

    public List<String> getOptions() {
        return new ArrayList<>(choices.keySet());
    }
}
