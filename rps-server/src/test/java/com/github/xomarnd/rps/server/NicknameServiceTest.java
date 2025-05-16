package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.NicknameService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NicknameServiceTest {
    private final NicknameService service = new NicknameService();

    @Test void testValid() {
        assertTrue(service.isValid("Abc_123"));
        assertTrue(service.isValid("XxX_Player42"));
    }

    @Test void testInvalid() {
        assertFalse(service.isValid(""));
        assertFalse(service.isValid("ab"));
        assertFalse(service.isValid("john@doe"));
        assertFalse(service.isValid("саша")); // кириллица не допускается
        assertFalse(service.isValid("player player")); // пробелы запрещены
        assertFalse(service.isValid(null));
    }
}
