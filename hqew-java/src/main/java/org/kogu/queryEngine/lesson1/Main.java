package org.kogu.queryEngine.lesson1;

import java.util.*;

import static java.util.stream.Collectors.*;

final class Main {
    private Main() {}

    public static void main() {
        System.out.println("aaaa");
        // SELECT
        //    passenger_count,
        //    AVG(fare_amount) AS avg_fare
        //FROM trips
        //WHERE trip_distance > 5
        //GROUP BY passenger_count
        //ORDER BY avg_fare DESC;
    }

    record Trip(int passengerCount, int tripDistance, int fareAmount) {
        Trip {
            require(tripDistance > 0, "tripDistance >= 0");
            require(passengerCount > 0, "passengerCount >= 0");
            require(fareAmount > 0, "fareAmount >= 0");
        }
    }

    private static void require(boolean predicate, String message) {
        if (!predicate) {
            throw new IllegalArgumentException(message);
        }
    }

    record Result(int passengerCount, double avgFare) {
        public static Collection<Result> rowEval(Collection<Trip> trips, int minimumDistance) {
            Map<Integer, Double> collect = trips.stream()
                    .filter(t -> t.tripDistance > minimumDistance)
                    .collect(groupingBy(Trip::passengerCount, averagingInt(Trip::fareAmount)));

            List<Result> results = collect.entrySet().stream()
                    .map(entry -> new Result(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(Result::avgFare).reversed())
                    .toList();

            return results;
        }

        public static Collection<Result> columnEval(int[] tripDistances, int[] passengerCounts, int[] fareAmounts,
                                                    int minimumDistance) {
            require(tripDistances.length == passengerCounts.length &&
                    tripDistances.length == fareAmounts.length, "same length of each column array");

            int n = tripDistances.length;

            Map<Integer, SumCount> collect = new HashMap<>();
            for (int i = 0; i < n; i++) {
                if (tripDistances[i] > minimumDistance) {
                    collect.merge(passengerCounts[i], new SumCount(fareAmounts[i]), SumCount::add);
                }
            }

            List<Result> results = collect.entrySet().stream()
                    .map(kv -> new Result(kv.getKey(), kv.getValue().average()))
                    .sorted(Comparator.comparing(Result::avgFare).reversed())
                    .toList();

            return results;
        }
    }

    private static final class SumCount {
        public int sum, count;

        public SumCount(int fareAmount) {
            this.sum = fareAmount;
            this.count = 1;
        }

        public void add(int fareAmount) {
            this.sum += fareAmount;
            this.count++;
        }

        public double average() {
            return (double) sum / count;
        }

        public SumCount add(SumCount other) {
            this.sum += other.sum;
            this.count += other.count;
            return this;
        }
    }


}
