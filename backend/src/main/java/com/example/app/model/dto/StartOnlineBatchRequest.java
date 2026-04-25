package com.example.app.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartOnlineBatchRequest(
        @NotBlank(message = "ジョブ名は必須です") @Size(max = 40, message = "ジョブ名は40文字以内で入力してください") String jobName,
        @NotNull(message = "処理件数は必須です") @Min(value = 1, message = "処理件数は1件以上で指定してください") @Max(value = 50, message = "処理件数は50件以下で指定してください") Integer totalItems,
        @Min(value = 1, message = "失敗させる件番は1以上で指定してください") @Max(value = 50, message = "失敗させる件番は50以下で指定してください") Integer failureAtItem,
        @Min(value = 0, message = "処理間隔は0以上で指定してください") @Max(value = 2000, message = "処理間隔は2000ms以下で指定してください") Integer processingDelayMs) {
}
