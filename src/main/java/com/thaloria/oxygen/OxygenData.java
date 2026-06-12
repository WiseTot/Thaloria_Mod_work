package com.thaloria.oxygen;

public class OxygenData implements IOxygenData {

    private int oxygen = 100; // максимум 100

    @Override
    public int getOxygen() {
        return oxygen;
    }

    @Override
    public void setOxygen(int value) {
        this.oxygen = Math.max(0, Math.min(100, value));
    }

    @Override
    public void addOxygen(int value) {
        setOxygen(this.oxygen + value);
    }

    @Override
    public void removeOxygen(int value) {
        setOxygen(this.oxygen - value);
    }
}