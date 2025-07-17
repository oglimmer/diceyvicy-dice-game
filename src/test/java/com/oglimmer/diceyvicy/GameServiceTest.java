package com.oglimmer.diceyvicy;

import com.oglimmer.kniffel.service.KniffelRules;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameServiceTest {

    private final KniffelRules diceyvicyRules = new KniffelRules();

    @Test
    void testGetScoreFullHouse_ValidFullHouse_ThreeOfAKindAndPair() {
        // Three 2s and two 5s (2,2,2,5,5)
        List<Integer> diceRolls = Arrays.asList(2, 2, 2, 5, 5);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(25, score);
    }

    @Test
    void testGetScoreFullHouse_ValidFullHouse_ThreeFivesAndTwoThrees() {
        // Three 5s and two 3s (5,5,5,3,3)
        List<Integer> diceRolls = Arrays.asList(5, 5, 5, 3, 3);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(25, score);
    }

    @Test
    void testGetScoreFullHouse_ValidFullHouse_DifferentOrder() {
        // Full house in different order (1,4,4,1,1)
        List<Integer> diceRolls = Arrays.asList(1, 4, 4, 1, 1);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(25, score);
    }

    @Test
    void testGetScoreFullHouse_NoFullHouse_AllSame() {
        // All same numbers (not a full house)
        List<Integer> diceRolls = Arrays.asList(3, 3, 3, 3, 3);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }

    @Test
    void testGetScoreFullHouse_NoFullHouse_TwoPairs() {
        // Two pairs (not a full house)
        List<Integer> diceRolls = Arrays.asList(1, 1, 2, 2, 3);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }

    @Test
    void testGetScoreFullHouse_NoFullHouse_ThreeOfAKind() {
        // Three of a kind but no pair
        List<Integer> diceRolls = Arrays.asList(4, 4, 4, 1, 2);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }

    @Test
    void testGetScoreFullHouse_NoFullHouse_Straight() {
        // Straight sequence (not a full house)
        List<Integer> diceRolls = Arrays.asList(1, 2, 3, 4, 5);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }

    @Test
    void testGetScoreFullHouse_NoFullHouse_OnePair() {
        // Only one pair
        List<Integer> diceRolls = Arrays.asList(6, 6, 1, 2, 3);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }

    @Test
    void testGetScoreFullHouse_NoFullHouse_AllDifferent() {
        // All different numbers
        List<Integer> diceRolls = Arrays.asList(1, 2, 3, 4, 6);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }

    @Test
    void testGetScoreFullHouse_EdgeCase_FourOfAKind() {
        // Four of a kind with one different (not a full house)
        List<Integer> diceRolls = Arrays.asList(2, 2, 2, 2, 5);
        int score = diceyvicyRules.getScoreFullHouse(diceRolls);
        assertEquals(0, score);
    }
}