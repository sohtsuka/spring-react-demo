package com.example.app.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    @Test
    void run_withNoArgs_returns1AndPrintsUsage() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{}, System.out, new PrintStream(err));

        assertThat(code).isEqualTo(1);
        assertThat(err.toString()).contains("使い方");
    }

    @Test
    void run_withBlankArg_returns1() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{""}, System.out, new PrintStream(err));

        assertThat(code).isEqualTo(1);
    }

    @Test
    void run_withPasswordOnly_returns0AndPrintsHash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{"Password1!"}, new PrintStream(out), System.err);

        assertThat(code).isEqualTo(0);
        assertThat(out.toString().trim()).isNotBlank();
    }

    @Test
    void run_withPasswordAndPepper_returns0() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = PasswordHasher.run(new String[]{"Password1!", "test-pepper"}, new PrintStream(out), System.err);

        assertThat(code).isEqualTo(0);
        assertThat(out.toString().trim()).isNotBlank();
    }
}
