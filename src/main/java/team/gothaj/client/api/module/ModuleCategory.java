package team.gothaj.client.api.module;

import lombok.Getter;

@Getter
public enum ModuleCategory {
    COMBAT("Combat"),
    MISC("Misc"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    VISUAL("Visual"),
    WORLD("World"),
    GHOST("Ghost"),
    CLIENT("Client");

    private final String name;

    ModuleCategory(String name) {
        this.name = name;
    }
}

