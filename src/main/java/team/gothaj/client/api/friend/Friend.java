package team.gothaj.client.api.friend;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author vajzii
 * @since 2025
 */

@Getter
@Setter
@AllArgsConstructor
public class Friend {

    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Friend friend)) return false;
        return name != null && name.equalsIgnoreCase(friend.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.toLowerCase().hashCode() : 0;
    }
}
