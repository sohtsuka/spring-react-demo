package com.example.app.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 認証・認可
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "ユーザー名またはパスワードが正しくありません", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "アカウントがロックされています。しばらく待ってから再試行してください", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "アカウントが無効化されています", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "認証が必要です", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "この操作を行う権限がありません", HttpStatus.FORBIDDEN),

    // リソース
    USER_NOT_FOUND("USER_NOT_FOUND", "ユーザーが見つかりません", HttpStatus.NOT_FOUND),

    // 重複
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "このユーザー名はすでに使用されています", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "このメールアドレスはすでに使用されています", HttpStatus.CONFLICT),

    // バリデーション
    VALIDATION_ERROR("VALIDATION_ERROR", "入力内容に誤りがあります", HttpStatus.BAD_REQUEST),

    // レート制限
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "リクエストが多すぎます。しばらく待ってから再試行してください", HttpStatus.TOO_MANY_REQUESTS),

    // サーバーエラー
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "サーバー内部エラーが発生しました", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
