package ru.constantdynamic.hpke.misc;

public interface Memoable {
    Memoable copy();

    void reset(Memoable other);
}
