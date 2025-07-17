package com.oglimmer.diceyvicy;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
        gameService.handlePlayerReroll(gameId, request.getDiceToKeep());
    }

    @MessageMapping("/game/{gameId}/book")
    public void bookDiceRoll(@DestinationVariable String gameId, @Payload BookRequest request) {
        gameService.handlePlayerBook(gameId, request.getBookingType());
    }

    @Getter
    @Setter
    @ToString
    public static class RerollRequest {
        @JsonPropertyDescription("The positions dice to keep. This is 1-based positions in the array.")
        private int[] diceToKeep;
    }

    @Getter
    @Setter
    @ToString
    public static class BookRequest {
        private BookingType bookingType;
    }

}