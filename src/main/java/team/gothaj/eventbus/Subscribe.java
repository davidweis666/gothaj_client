package team.gothaj.eventbus;

import java.lang.invoke.MethodHandle;
import java.util.Objects;

public record Subscribe(Object parent, MethodHandle methodHandle, int priority) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscribe that = (Subscribe) o;
        return Objects.equals(parent, that.parent) && Objects.equals(methodHandle, that.methodHandle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, methodHandle);
    }
}
