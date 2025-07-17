package com.oglimmer.diceyvicy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ListFilterWithCountsTest {

    private final AiBot aiBot = new AiBot();

    @BeforeAll
    static void setUp() {
        System.setProperty("OPENAI_API_KEY", "fake");
    }

    @Test
    void testExactMatch() {
        List<Integer> list1 = Arrays.asList(1, 1, 2);
        List<Integer> list2 = Arrays.asList(1, 1, 2);
        List<Integer> expected = Arrays.asList(1, 1, 2);
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }

    @Test
    void testPartialMatchDueToFrequency() {
        List<Integer> list1 = Arrays.asList(1, 1, 2);
        List<Integer> list2 = List.of(1);
        List<Integer> expected = List.of(1);
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }

    @Test
    void testNoMatch() {
        List<Integer> list1 = Arrays.asList(3, 4, 5);
        List<Integer> list2 = Arrays.asList(1, 2);
        List<Integer> expected = Collections.emptyList();
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }

    @Test
    void testEmptyFirstList() {
        List<Integer> list1 = Collections.emptyList();
        List<Integer> list2 = Arrays.asList(1, 2);
        List<Integer> expected = Collections.emptyList();
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }

    @Test
    void testEmptySecondList() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Collections.emptyList();
        List<Integer> expected = Collections.emptyList();
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }

    @Test
    void testDuplicatesInBothLists() {
        List<Integer> list1 = Arrays.asList(2, 2, 2, 3, 3);
        List<Integer> list2 = Arrays.asList(2, 2, 3);
        List<Integer> expected = Arrays.asList(2, 2, 3);
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }

    @Test
    void testAllElementsMatchButLimitedByFrequency() {
        List<Integer> list1 = Arrays.asList(4, 4, 4, 4);
        List<Integer> list2 = Arrays.asList(4, 4);
        List<Integer> expected = Arrays.asList(4, 4);
        assertEquals(expected, aiBot.filterByCount(list1, list2));
    }
}