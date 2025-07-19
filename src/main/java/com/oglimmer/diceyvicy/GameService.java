package com.oglimmer.diceyvicy;

import com.oglimmer.kniffel.model.BookingType;
import com.oglimmer.kniffel.model.KniffelPlayer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();
    private final Map<String, AiBot> gameBots = new ConcurrentHashMap<>();

    public GameState startNewGame(String playerName, String aiModel) {
        GameState gameState = new GameState();
        gameState.initializeGame(playerName);
        
        // Create AI bot with selected model for this game
        AiBot aiBot = new AiBot(aiModel);
        gameBots.put(gameState.getGameId(), aiBot);
        
        gameStates.put(gameState.getGameId(), gameState);
        return gameState;
    }

    public void handlePlayerReroll(String gameId, int[] dicePositionToKeep) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.error("Game not found: {}", gameId);
            return;
        }

        if (gameState.getRollCount() >= 3) {
            log.error("Cannot reroll, already rolled 2 times for game: {}", gameId);
            return;
        }

        gameState.rerollDiceByPos(dicePositionToKeep);
        log.info("Player rerolled dice for game: {}, roll count: {}", gameId, gameState.getRollCount());
        broadcastGameState(gameId, gameState);
    }

    public void handlePlayerBook(String gameId, BookingType bookingType) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.error("Game not found: {}", gameId);
            return;
        }

        if (gameState.getCurrentPlayer().getUsedBookingTypes().contains(bookingType)) {
            log.error("Booking type {} already used for game: {}", bookingType, gameId);
            return;
        }

        gameState.bookDiceRoll(bookingType);
        log.info("Player booked dice roll for game: {} with booking type: {}", gameId, bookingType);
        broadcastGameState(gameId, gameState);

        // Check if AI's turn
        if (!gameState.isGameOver() && gameState.getCurrentPlayer().getName().equals("Jürgen-AI")) {
            handleAiTurn(gameId, gameState);
        }
    }

    private void handleAiTurn(String gameId, GameState gameState) {
        // Get the AI bot for this specific game
        AiBot aiBot = gameBots.get(gameId);
        if (aiBot == null) {
            log.error("No AI bot found for game: {}", gameId);
            return;
        }
        
        // AI reroll logic
        KniffelPlayer currentPlayer = gameState.getCurrentPlayer();
        broadcastGameStateWithAction(gameId, gameState, "Jürgen is thinking about " + gameState.getDiceRolls() + "...");
        while (gameState.getRollCount() < 3) {
            int[] diceToKeep = aiBot.askAiWhichDiceToKeep(
                    gameState.getDiceRolls(),
                    currentPlayer.getUsedBookingTypes(),
                    gameState.getRollCount()
            );

            log.info("{} will reroll dice for game: {}, current dice: {}, keeping: {}", currentPlayer.getName(), gameId, gameState.getDiceRolls(), diceToKeep);
            gameState.rerollDiceByVal(diceToKeep);

            String aiAction = String.format("Jürgen kept dice: %s and re-rolled to %s - thinking again...",
                    Arrays.toString(diceToKeep),
                    gameState.getDiceRolls().stream().map(String::valueOf).collect(Collectors.joining(", ")));
            broadcastGameStateWithAction(gameId, gameState, aiAction);

            // Add delay for better UX
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // AI book
        BookingType bookingType = aiBot.askAiBookingType(
                gameState.getDiceRolls(),
                currentPlayer.getUsedBookingTypes()
        );

        int previousScore = currentPlayer.getScore();
        String finalDiceRoll = gameState.getDiceRolls().stream().map(String::valueOf).collect(Collectors.joining(", "));
        gameState.bookDiceRoll(bookingType);
        int newScore = currentPlayer.getScore();
        int scoreGained = newScore - previousScore;

        log.info("{} booked dice roll for game: {} with booking type: {} on dice: {}", currentPlayer.getName(), gameId, bookingType, finalDiceRoll);

        String aiAction = String.format("Jürgen booked %s for %d points with dice: [%s] - It's your turn now!",
                bookingType.toString().replace("_", " "),
                scoreGained,
                finalDiceRoll);
        broadcastGameStateWithAction(gameId, gameState, aiAction);
    }

    private void broadcastGameState(String gameId, GameState gameState) {
        broadcastGameStateWithAction(gameId, gameState, null);
    }

    private void broadcastGameStateWithAction(String gameId, GameState gameState, String aiAction) {
        log.debug("Broadcasting game state for game: {}, action: {}", gameId, aiAction);
        GameController.GameResponse response = GameController.GameResponse.fromGameState(gameState);
        response.setAiAction(aiAction);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, response);
        
        // Clean up if game is over
        if (gameState.isGameOver()) {
            cleanupGame(gameId);
        }
    }
    
    private void cleanupGame(String gameId) {
        gameStates.remove(gameId);
        gameBots.remove(gameId);
        log.info("Cleaned up game: {}", gameId);
    }
}