package com.example.app.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartOnlineBatchRequest(
        @NotBlank(message = "ジョブ名は必須です") @Size(max = 40, message = "40文字以内で入力してください") String jobName,
        @NotNull(message = "処理件数は必須です") @Min(1) @Max(50) Integer totalItems, @Min(1) @Max(50) Integer failureAtItem,
        @Min(0) @Max(2000) Integer processingDelayMs) {
}
