package net.logstash.logback.mask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.NullNode;

class RegexValueMaskerTest {

    @Test
    void regexMatchesFullString() {
        RegexValueMasker masker = new RegexValueMasker("^test$", "****");
        assertThat(masker.mask(null, "test")).isEqualTo("****");
        assertThat(masker.mask(null, "testtest")).isNull();
    }

    @Test
    void regexMatchesPartialString() {
        RegexValueMasker masker = new RegexValueMasker("test", "****");
        assertThat(masker.mask(null, "test")).isEqualTo("****");
        assertThat(masker.mask(null, "test-test")).isEqualTo("****-****");
        assertThat(masker.mask(null, "foo")).isNull();
    }

    @Test
    void nullMask() {
        RegexValueMasker masker = new RegexValueMasker("test", NullNode.instance);
        assertThat(masker.mask(null, "test")).isEqualTo(NullNode.instance);
        assertThat(masker.mask(null, "test-test")).isNull();
    }

    @Test
    void nonString() {
        RegexValueMasker masker = new RegexValueMasker("test", "****");
        assertThat(masker.mask(null, 1)).isNull();
    }

}