package com.oglimmer.diceyvicy;

import com.oglimmer.kniffel.model.BookingType;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.function.Function;

@Slf4j
public class AiModel35TurboFineTuned implements AiModel {

    @Override
    public String askModel(String systemPrompt, String userPrompt, Function<Object, Boolean> verify) {
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
            if (verify != null && Arrays.stream(BookingType.values()).anyMatch(verify::apply)) {
                return null;
            }
        }

        return chatCompletion.choices().getFirst().message().content().orElse(null);
    }
}
