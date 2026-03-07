package com.example.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PepperPasswordEncoderTest {

    private final PasswordEncoder delegate = mock(PasswordEncoder.class);
    private final PepperPasswordEncoder encoder = new PepperPasswordEncoder(delegate, "testPepper");

    @Test
    void encode_producesDelimitedFormat() {
        given(delegate.encode(anyString())).willReturn("delegateHash");

        String encoded = encoder.encode("myPassword");

        assertThat(encoded).contains(":");
        String[] parts = encoded.split(":", 2);
        assertThat(parts).hasSize(2);
        assertThat(parts[1]).isEqualTo("delegateHash");
    }

    @Test
    void matches_withValidFormat_returnsTrue() {
        given(delegate.encode(anyString())).willReturn("delegateHash");
        given(delegate.matches(anyString(), anyString())).willReturn(true);

        String encoded = encoder.encode("myPassword");
        boolean result = encoder.matches("myPassword", encoded);

        assertThat(result).isTrue();
    }

    @Test
    void matches_withWrongPassword_returnsFalse() {
        given(delegate.encode(anyString())).willReturn("delegateHash");
        given(delegate.matches(anyString(), anyString())).willReturn(false);

        String encoded = encoder.encode("myPassword");
        boolean result = encoder.matches("wrongPassword", encoded);

        assertThat(result).isFalse();
    }

    @Test
    void matches_withNoDelimiter_returnsFalse() {
        boolean result = encoder.matches("anyPassword", "noDelimiterHere");

        assertThat(result).isFalse();
    }

    @Test
    void upgradeEncoding_withValidFormat_delegatesToInner() {
        given(delegate.upgradeEncoding("delegateHash")).willReturn(true);

        boolean result = encoder.upgradeEncoding("appSalt:delegateHash");

        assertThat(result).isTrue();
    }

    @Test
    void upgradeEncoding_withNoDelimiter_returnsFalse() {
        boolean result = encoder.upgradeEncoding("noDelimiterHere");

        assertThat(result).isFalse();
    }
}
