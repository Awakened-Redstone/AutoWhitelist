package com.awakenedredstone.autowhitelist.discord.api;

public enum Constants {
    CONFIG_VERSION(2);

    public final Object value;

    <T> Constants(T value) {
        this.value = value;
    }
}
