package com.oglimmer.diceyvicy;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import java.util.function.Function;

public interface AiModel {

    OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(System.getProperty("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY")))
            .build();

    String askModel(String systemPrompt, String userPrompt, Function<Object, Boolean> verify);
}
