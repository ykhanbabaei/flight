package com.yousef.flightrecovery.service;

import com.yousef.flightrecovery.model.Route;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;


public final class RouteFinder {

    // Static Constraints
    private static final int MIN_CONNECTION_MINUTES = 45;
    private static final int MAX_LEGS = 4;

    // Scoring weights
    private static final double PRICE_WEIGHT = 1.0;   // score units per currency unit
    private static final double TIME_WEIGHT = 0.2;    // score units per minute (flight + layover)
    private static final double HOP_WEIGHT = 20.0;    // flat score penalty per additional leg

    private RouteFinder() {
    }

    public static List<Route> findBestRoutesForJourney(
            String fromAirport,
            String toAirport,
            OffsetDateTime deadlineConstraint,
            int passengersCount,
            List<Route> routes,
            Map<String, Integer> remainingSeats) {

        // Adjacent list representing routes Graph. Each airport is node with list of adjacent routes.
        // Remove the routes which does not satisfy constraints like available seats
        Map<String, List<Route>> adjacentRoutes = routes.stream()
                .filter(r -> remainingSeats.getOrDefault(r.id(), 0) >= passengersCount)
                .collect(Collectors.groupingBy(Route::fromAirport));

        //Based on label-setting Dijkstra
        PriorityQueue<Label> queue = new PriorityQueue<>(Comparator.comparingDouble(l -> l.totalScore));
        queue.add(new Label(fromAirport, null, 0.0, 0.0, 0, List.of()));

        // Non-dominated labels seen per airport, used to prune the search space.
        Map<String, List<Label>> frontier = new HashMap<>();

        Label best = null;

        // Scan graph in BFS and calculate labels for each node
        while (!queue.isEmpty()) {
            Label current = queue.poll();

            if (isDominated(current, frontier.getOrDefault(current.airport, List.of()))) {
                continue; // a strictly better-or-equal label already reached this airport

            }
            register(frontier, current);

            if (current.airport.equals(toAirport)) {
                if (best == null || current.totalScore < best.totalScore) {
                    best = current;
                }
                continue; // no reason to connect onward from the destination
            }

            if (current.legs >= MAX_LEGS) {
                continue;
            }

            for (Route candidate : adjacentRoutes.getOrDefault(current.airport, List.of())) {
                Label next = nextLabel(current, candidate, deadlineConstraint);
                if (next != null && !isDominated(next, frontier.getOrDefault(next.airport, List.of()))) {
                    queue.add(next);
                }
            }
        }

        return best != null ? best.path : List.of();
    }

    /** Creating next label based on Route and current label. Return null when it is not possible due to deadline or other constraints */
    private static Label nextLabel(Label current, Route candidate, OffsetDateTime deadline) {
        OffsetDateTime departure = candidate.departureDateTime();
        OffsetDateTime arrival = candidate.arrivalDateTime();

        if (arrival.isAfter(deadline)) {
            return null; // would breach the passenger's required arrival window
        }

        long layoverMinutes = 0;
        if (current.arrivalTime != null) {
            Duration layover = Duration.between(current.arrivalTime, departure);
            if (layover.isNegative() || layover.toMinutes() < MIN_CONNECTION_MINUTES) {
                return null; // impossible or unrealistically tight connection
            }
            layoverMinutes = layover.toMinutes();
        }

        BigDecimal price = candidate.price();
        double legMinutes = Duration.between(departure, arrival).toMinutes();
        double edgeScore = price.doubleValue() * PRICE_WEIGHT
                + (legMinutes + layoverMinutes) * TIME_WEIGHT
                + 1 * HOP_WEIGHT ;

        List<Route> newPath = new ArrayList<>(current.path);
        newPath.add(candidate);

        return new Label(
                candidate.toAirport(),
                arrival,
                current.totalScore + edgeScore,
                current.totalDurationMinutes + legMinutes + layoverMinutes,
                current.legs + 1,
                newPath
        );
    }

    private static boolean isDominated(Label candidate, List<Label> existing) {
        for (Label other : existing) {
            if (dominates(other, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void register(Map<String, List<Label>> frontier, Label label) {
        List<Label> labels = frontier.computeIfAbsent(label.airport, k -> new ArrayList<>());
        labels.removeIf(existing -> dominates(label, existing));
        labels.add(label);
    }

    private static boolean dominates(Label a, Label b) {
        int arrivalCmp = compareArrival(a.arrivalTime, b.arrivalTime);
        boolean arrivalNotWorse = arrivalCmp <= 0;
        boolean scoreNotWorse = a.totalScore <= b.totalScore;
        boolean strictlyBetter = arrivalCmp < 0 || a.totalScore < b.totalScore;
        return arrivalNotWorse && scoreNotWorse && strictlyBetter;
    }

    private static int compareArrival(OffsetDateTime a, OffsetDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private record Label (
        String airport,
        OffsetDateTime arrivalTime,
        double totalScore,
        double totalDurationMinutes,
        int legs,
        List<Route> path) { }

}
