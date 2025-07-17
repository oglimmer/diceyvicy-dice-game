package com.oglimmer.diceyvicy;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.oglimmer.kniffel.model.BookingType;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiBot {

    private final OpenAIClient client;

    public AiBot() {
        String openaiApiKey = System.getProperty("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY"));
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(openaiApiKey)
                .build();
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

            System.out.println("************************************************");
            System.out.println(systemPrompt);
            System.out.println(userPrompt);
            System.out.println("************************************************");

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("ft:gpt-3.5-turbo-0125:personal::BuP2JgWv")
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .maxCompletionTokens(200)
                    .temperature(0.1)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);
            if (chatCompletion.choices().size() != 1) {
                log.error("Unexpected number of choices returned: {}", chatCompletion.choices().size());
                return Arrays.stream(BookingType.values()).filter(bt -> !usedBookingTypes.contains(bt)).findFirst().orElseThrow();
            }
            String responseText = chatCompletion.choices().getFirst().message().content().orElse(null);

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

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("ft:gpt-3.5-turbo-0125:personal::BuP2JgWv")
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .maxCompletionTokens(200)
                    .temperature(0.1)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);
            if (chatCompletion.choices().size() != 1) {
                log.error("Unexpected number of choices returned: {}", chatCompletion.choices().size());
                return new int[0];
            }
            String responseText = chatCompletion.choices().getFirst().message().content().orElse(null);

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