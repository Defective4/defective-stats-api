package net.defekt.stats.api;

public interface StatsUpdateCallback {
    public boolean updating(StatsCollector collector);
}
