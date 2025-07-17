package com.oglimmer.diceyvicy;

import com.oglimmer.kniffel.model.BookingType;
import com.oglimmer.kniffel.model.KniffelGame;
import com.oglimmer.kniffel.model.KniffelPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

@Data
@Slf4j
public class GameState {

    private String gameId;
    private KniffelGame game;

    public GameState() {
        this.gameId = UUID.randomUUID().toString();
    }

    public void initializeGame(String playerName) {
        KniffelPlayer player = new KniffelPlayer(playerName);
        KniffelPlayer ai = new KniffelPlayer("Jürgen-AI");
        this.game = new KniffelGame(List.of(player, ai));
    }

    public List<Integer> getDiceRolls() {
        return game.getDiceRolls();
    }

    public KniffelPlayer getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public int getRollCount() {
        return game.getRollRound();
    }

    public void rerollDiceByPos(int[] dicePositionToKeep) {
        int[] diceValueToKeep = filterByPositions(game.getDiceRolls(), dicePositionToKeep);
        log.info("Player {} rerolled dice, dice to keep: {}, current dice: {}", getCurrentPlayer().getName(), diceValueToKeep, game.getDiceRolls());
        game.reRollDice(diceValueToKeep);
        log.info("Player {} after rerolled dice, {}", getCurrentPlayer().getName(), game.getDiceRolls());
    }

    public void rerollDiceByVal(int[] diceValueToKeep) {
        log.info("Player {} rerolled dice, dice to keep: {}, current dice: {}", getCurrentPlayer().getName(), diceValueToKeep, game.getDiceRolls());
        game.reRollDice(diceValueToKeep);
        log.info("Player {} after rerolled dice, {}", getCurrentPlayer().getName(), game.getDiceRolls());
    }

    public void bookDiceRoll(BookingType bookingType) {
        game.bookDiceRoll(bookingType);
    }

    public boolean isGameOver() {
        return getCurrentPlayer().getUsedBookingTypes().size() == BookingType.values().length;
    }

    public Map<String, KniffelPlayer> getPlayers() {
        return game.getPlayers();
    }

    public static int[] filterByPositions(List<Integer> source, int[] positions) {
        if (source == null || positions == null) {
            return new int[0];
        }
        return IntStream.of(positions)
                .mapToObj(pos -> pos > 0 && pos <= source.size()
                        ? source.get(pos - 1)
                        : null)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();
    }
}