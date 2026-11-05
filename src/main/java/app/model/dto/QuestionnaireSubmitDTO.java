package app.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class QuestionnaireSubmitDTO {

    @NotNull
    private Map<UUID, Integer> scores = new LinkedHashMap<>();
}
