package com.oglimmer.diceyvicy;

import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputText;

import java.util.function.Function;
import java.util.stream.Collectors;


public class AiModel4OMini implements AiModel {

    public String askModel(String systemPrompt, String userPrompt, Function<Object, Boolean> verify) {
        ResponseCreateParams createParams = ResponseCreateParams.builder()
                .model("o4-mini")
                .input(ResponseCreateParams.Input.ofText(systemPrompt + "\n" + userPrompt))
                .reasoning(Reasoning.builder().effort(ReasoningEffort.MEDIUM).build())
                .build();


        return client.responses().create(createParams).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(ResponseOutputText::text).collect(Collectors.joining("\n"));
    }

}
