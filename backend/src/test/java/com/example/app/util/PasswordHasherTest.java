package com.example.app.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void run_withNoArgs_returns1AndPrintsUsage() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{}, System.out, new PrintStream(err, false, StandardCharsets.UTF_8));

        assertThat(code).isEqualTo(1);
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("使い方");
    }

    @Test
    void run_withBlankArg_returns1() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{""}, System.out,
                new PrintStream(err, false, StandardCharsets.UTF_8));

        assertThat(code).isEqualTo(1);
    }

    @Test
    void run_withPasswordOnly_returns0AndPrintsHash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{"Password1!"}, new PrintStream(out, false, StandardCharsets.UTF_8),
                System.err);

        assertThat(code).isEqualTo(0);
        assertThat(out.toString(StandardCharsets.UTF_8).trim()).isNotBlank();
    }

    @Test
    void run_withPasswordAndPepper_returns0() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{"Password1!", "test-pepper"},
                new PrintStream(out, false, StandardCharsets.UTF_8), System.err);

        assertThat(code).isEqualTo(0);
        assertThat(out.toString(StandardCharsets.UTF_8).trim()).isNotBlank();
    }
}
