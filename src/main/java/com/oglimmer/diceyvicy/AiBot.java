package com.oglimmer.diceyvicy;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oglimmer.kniffel.model.BookingType;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class AiBot {

    private AiModel aiModel;

    public AiBot() {
        this.aiModel = new AiModel4OMini(); // Default model
    }

    public AiBot(String modelType) {
        setAiModel(modelType);
    }

    public void setAiModel(String modelType) {
        switch (modelType) {
            case "4OMini":
                this.aiModel = new AiModel4OMini();
                break;
            case "35TurboFineTuned":
                this.aiModel = new AiModel35TurboFineTuned();
                break;
            default:
                log.warn("Unknown AI model type: {}, using default 4OMini", modelType);
                this.aiModel = new AiModel4OMini();
                break;
        }
        log.info("AI model set to: {}", modelType);
    }

    @ToString
    public static class BookingSelection {
        @JsonPropertyDescription("The booking type to choose")
        public String bookingType;

        @JsonPropertyDescription("Reasoning for the choice")
        public String reasoning;
    }

    public BookingType askAiBookingType(List<Integer> diceRolls, List<BookingType> usedBookingTypes) {
        try {
            List<String> availableTypeNames = Arrays.stream(BookingType.values())
                    .filter(bt -> !usedBookingTypes.contains(bt))
                    .map(BookingType::name)
                    .collect(Collectors.toList());

            String systemPrompt = """
                    You are an expert Yahtzee player. Analyze the dice rolls and suggest the best booking type to maximize score.
                    
                    Never break the Yahtzee rules. Never invent new actions. Never use non existing options. Always follow the rules as strictly as 1000 peoples lives depend on it.
                    Please respond with a JSON object in the following format:
                    {"bookingType": "string", "reasoning": "string"}
                    Where bookingType is one of the available booking types and reasoning explains your choice.""";

            String userPrompt = String.format("Dice rolls: %s\nAvailable booking types: %s\nWhich booking type should I choose?",
                    diceRolls, String.join(", ", availableTypeNames));


            String responseText = aiModel.askModel(systemPrompt, userPrompt, bt -> !usedBookingTypes.contains(bt));

            ObjectMapper mapper = new ObjectMapper();
            BookingSelection selection = null;
            if (responseText != null) {
                try {
                    selection = mapper.readValue(responseText, BookingSelection.class);
                } catch (Exception e) {
                    log.error("Error parsing JSON response: {}", e.getMessage());
                    log.debug("Response text: {}", responseText);
                }
            }

            log.info("OpenAI structured booking type response: {}", selection);

            if (selection != null && selection.bookingType != null) {
                try {
                    BookingType selectedType = BookingType.valueOf(selection.bookingType);
                    if (!usedBookingTypes.contains(selectedType)) {
                        return selectedType;
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Invalid booking type returned: {}", selection.bookingType);
                }
            }

            log.error("No booking type returned");
            return Arrays.stream(BookingType.values()).filter(bt -> !usedBookingTypes.contains(bt)).findFirst().orElseThrow();
        } catch (Exception e) {
            log.error("Error calling OpenAI for booking type: {}", e.getMessage());
            return Arrays.stream(BookingType.values()).filter(bt -> !usedBookingTypes.contains(bt)).findFirst().orElseThrow();
        }
    }

    @ToString
    public static class DiceSelection {
        @JsonPropertyDescription("Array of dice to keep, empty array if none")
        public List<Integer> diceToKeep;

        @JsonPropertyDescription("Reasoning for the choice")
        public String reasoning;
    }

    public int[] askAiWhichDiceToKeep(List<Integer> diceRolls, List<BookingType> usedBookingTypes, int round) {
        try {
            String availableTypes = Arrays.stream(BookingType.values())
                    .filter(bt -> !usedBookingTypes.contains(bt))
                    .map(BookingType::name)
                    .collect(Collectors.joining(", "));

            String systemPrompt = """
                    You are an expert Yahtzee strategist. Analyze the dice and determine which dice to keep for optimal scoring.
                    
                    Never break the Yahtzee rules. Never invent new actions. Never use non existing options. Always follow the rules as strictly as 1000 peoples lives depend on it.
                    Please respond with a JSON object in the following format:
                    {"diceToKeep": [array of dice values], "reasoning": "string"}
                    Where diceToKeep is an array of dice values to keep (empty array if none) and reasoning explains your choice.""";

            String userPrompt = String.format("You will be able to re-roll the dice %s. Your current dice: %s \nAvailable booking types: %s\nWhich dice should I keep and remember to list all dice to keep one by one? Do not list dice which are not in your current dice roll.",
                    round == 0 ? "twice" : (round == 1 ? " once" : null), diceRolls, availableTypes);

            String responseText = aiModel.askModel(systemPrompt, userPrompt, null);

            ObjectMapper mapper = new ObjectMapper();
            DiceSelection selection = null;
            if (responseText != null) {
                try {
                    selection = mapper.readValue(responseText, DiceSelection.class);
                } catch (Exception e) {
                    log.error("Error parsing JSON response: {}", e.getMessage());
                    log.debug("Response text: {}", responseText);
                }
            }

            log.info("OpenAI structured dice response: {}", selection);

            if (selection != null && selection.diceToKeep != null) {
                return filterByCount(diceRolls, selection.diceToKeep).stream().mapToInt(Integer::intValue).toArray();
            }

            log.error("No dice positions to keep returned");
            return new int[0];
        } catch (Exception e) {
            log.error("Error calling OpenAI for dice to keep: {}", e.getMessage());
            return new int[0];
        }
    }

    public List<Integer> filterByCount(List<Integer> source, List<Integer> reference) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Integer num : reference) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        List<Integer> result = new ArrayList<>();
        for (Integer num : source) {
            if (countMap.getOrDefault(num, 0) > 0) {
                result.add(num);
                countMap.put(num, countMap.get(num) - 1);
            }
        }
        return result;
    }
}