package com.automation.hunger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Simple "top riders" ranking logic, mirroring a Hunger Station report. */
public class TopRidersCalculator {

    public record Rider(String name, int orders, double cash) {}

    /** Returns riders sorted by orders desc, then cash desc. Null-safe. */
    public List<Rider> topRiders(List<Rider> riders, int limit) {
        if (riders == null) return List.of();
        List<Rider> copy = new ArrayList<>(riders);
        copy.sort(Comparator.comparingInt(Rider::orders).reversed()
                .thenComparing(Comparator.comparingDouble(Rider::cash).reversed()));
        if (limit <= 0 || limit >= copy.size()) return copy;
        return copy.subList(0, limit);
    }
}
