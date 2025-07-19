package com.oglimmer.diceyvicy;

import com.oglimmer.kniffel.model.BookingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@AllArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;

    @PostMapping("/start")
    public ResponseEntity<GameResponse> startGame(@RequestBody StartGameRequest request) {
        log.info("Starting new game for player: {} with AI model: {}", request.getPlayerName(), request.getAiModel());
        GameState gameState = gameService.startNewGame(request.getPlayerName(), request.getAiModel());
        return ResponseEntity.ok(GameResponse.fromGameState(gameState));
    }

    @Getter
    @Setter
    public static class StartGameRequest {
        private String playerName;
        private String aiModel;
    }

    @Getter
    @Setter
    public static class GameResponse {
        private String gameId;
        private List<Integer> diceRolls;
        private String currentPlayer;
        private int rollCount;
        private boolean gameOver;
        private Map<String, PlayerData> players;
        private String aiAction;

        public static GameResponse fromGameState(GameState gameState) {
            GameResponse response = new GameResponse();
            response.gameId = gameState.getGameId();
            response.diceRolls = gameState.getDiceRolls();
            response.rollCount = gameState.getRollCount();
            response.gameOver = gameState.isGameOver();

            if (gameState.getCurrentPlayer() != null) {
                response.currentPlayer = gameState.getCurrentPlayer().getName();
            }

            if (gameState.getPlayers() != null) {
                response.players = new HashMap<>();
                gameState.getPlayers().forEach((name, player) -> {
                    PlayerData playerData = new PlayerData();
                    playerData.name = name;
                    playerData.score = player.getScore();
                    playerData.usedBookingTypes = player.getUsedBookingTypes();
                    response.players.put(name, playerData);
                });
            }

            return response;
        }
    }

    @Getter
    @Setter
    public static class PlayerData {
        private String name;
        private int score;
        private List<BookingType> usedBookingTypes;
    }
}