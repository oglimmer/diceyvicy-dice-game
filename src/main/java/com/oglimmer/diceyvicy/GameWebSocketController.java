package com.oglimmer.diceyvicy;

import com.oglimmer.kniffel.model.BookingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final GameService gameService;

    @MessageMapping("/game/{gameId}/reroll")
    public void rerollDice(@DestinationVariable String gameId, @Payload RerollRequest request) {
        log.info("WebSocket reroll request for gameId: {}, {}", gameId, request);
        gameService.handlePlayerReroll(gameId, request.getDiceToKeep());
    }

    @MessageMapping("/game/{gameId}/book")
    public void bookDiceRoll(@DestinationVariable String gameId, @Payload BookRequest request) {
        log.info("WebSocket book request for gameId: {} with booking type: {}", gameId, request.getBookingType());
        gameService.handlePlayerBook(gameId, request.getBookingType());
    }

    @Getter
    @Setter
    @ToString
    public static class RerollRequest {
        private int[] diceToKeep;
    }

    @Getter
    @Setter
    @ToString
    public static class BookRequest {
        private BookingType bookingType;
    }

}