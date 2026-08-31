package org.kogu.queryEngine.lesson1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void collectionAndColumnEvaluatorsReturnEquivalentResults() {
        List<Main.Trip> trips = List.of(
                new Main.Trip(1, 6, 10),
                new Main.Trip(1, 7, 20),
                new Main.Trip(2, 8, 30),
                new Main.Trip(2, 5, 100),
                new Main.Trip(3, 10, 40),
                new Main.Trip(4, 4, 200));
        int[] tripDistances = {6, 7, 8, 5, 10, 4};
        int[] passengerCounts = {1, 1, 2, 2, 3, 4};
        int[] fareAmounts = {10, 20, 30, 100, 40, 200};

        Collection<Main.Result> collectionResults = Main.Result.rowEval(trips, 5);
        Collection<Main.Result> columnResults = Main.Result.columnEval(
                tripDistances, passengerCounts, fareAmounts, 5);

        List<Main.Result> expected = List.of(
                new Main.Result(3, 40.0),
                new Main.Result(2, 30.0),
                new Main.Result(1, 15.0));
        assertEquals(expected, List.copyOf(collectionResults));
        assertEquals(expected, List.copyOf(columnResults));
        assertEquals(List.copyOf(collectionResults), List.copyOf(columnResults));
    }
}
