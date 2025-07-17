package com.oglimmer.diceyvicy;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.oglimmer.kniffel.model.BookingType;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
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

            String systemPrompt = "You are an expert Kniffel/Yahtzee player. Analyze the dice rolls and suggest the best booking type to maximize score.\n\n" +
                    "GAME RULES RULES:\n" +
                    "- Upper section: Ones (1s), Twos (2s), Threes (3s), Fours (4s), Fives (5s), Sixes (6s) - sum of matching dice\n" +
                    "- Three of a Kind: At least 3 dice with same value - sum of all dice\n" +
                    "- Four of a Kind: At least 4 dice with same value - sum of all dice\n" +
                    "- Full House: 3 of one kind + 2 of another - 25 points\n" +
                    "- Small Straight: 4 consecutive numbers - 30 points\n" +
                    "- Large Straight: 5 consecutive numbers - 40 points\n" +
                    "- Five of a Kind / Kniffel: All 5 dice same value - 50 points\n" +
                    "- Chance: Any combination - sum of all dice\n";
            String userPrompt = String.format("Dice rolls: %s\nAvailable booking types: %s\nWhich booking type should I choose?",
                    diceRolls, String.join(", ", availableTypeNames));

            System.out.println("************************************************");
            System.out.println(systemPrompt);
            System.out.println(userPrompt);
            System.out.println("************************************************");

            StructuredChatCompletionCreateParams<BookingSelection> params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4O_MINI)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .responseFormat(BookingSelection.class)
                    .maxCompletionTokens(200)
//                    .temperature(0.1)
                    .build();

            StructuredChatCompletion<BookingSelection> chatCompletion = client.chat().completions().create(params);
            if (chatCompletion.choices().size() != 1) {
                log.error("Unexpected number of choices returned: {}", chatCompletion.choices().size());
                return Arrays.stream(BookingType.values()).filter(bt -> !usedBookingTypes.contains(bt)).findFirst().orElseThrow();
            }
            BookingSelection selection = chatCompletion.choices().getFirst().message().content().orElse(null);

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

            String systemPrompt = "You are an expert Kniffel/Yahtzee strategist. Analyze the dice and determine which dice to keep for optimal scoring.\n\n" +
                    "GAME RULES:\n" +
                    "- Upper section: Ones (1s), Twos (2s), Threes (3s), Fours (4s), Fives (5s), Sixes (6s) - sum of matching dice\n" +
                    "- Three of a Kind: At least 3 dice with same value - sum of all dice\n" +
                    "- Four of a Kind: At least 4 dice with same value - sum of all dice\n" +
                    "- Full House: 3 of one kind + 2 of another - 25 points\n" +
                    "- Small Straight: 4 consecutive numbers - 30 points\n" +
                    "- Large Straight: 5 consecutive numbers - 40 points\n" +
                    "- Five of a Kind / Kniffel: All 5 dice same value - 50 points\n" +
                    "- Chance: Any combination - sum of all dice\n";
            String userPrompt = String.format("You will be able to re-roll the dice %s. Current dice rolls: %s \nAvailable booking types: %s\nWhich dice should I keep?",
                    round == 0 ? "twice" : (round == 1 ? " once" : null), diceRolls, availableTypes);

            StructuredChatCompletionCreateParams<DiceSelection> params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.GPT_4O_MINI)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .responseFormat(DiceSelection.class)
                    .maxCompletionTokens(200)
//                    .temperature(0.1)
                    .build();

            StructuredChatCompletion<DiceSelection> chatCompletion = client.chat().completions().create(params);
            if (chatCompletion.choices().size() != 1) {
                log.error("Unexpected number of choices returned: {}", chatCompletion.choices().size());
                return new int[0];
            }
            DiceSelection selection = chatCompletion.choices().getFirst().message().content().orElse(null);

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