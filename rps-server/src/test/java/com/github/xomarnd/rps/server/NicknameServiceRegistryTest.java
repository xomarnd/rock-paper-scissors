package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.NicknameService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NicknameServiceRegistryTest {

    @AfterEach
    void cleanup() {
        NicknameService.releaseNick("test");
        NicknameService.releaseNick("TEST");
        NicknameService.releaseNick("Another");
        NicknameService.releaseNick("other");
    }

    @Test
    void testReserveNickOnce() {
        NicknameService ns = new NicknameService();
        assertTrue(ns.reserveNick("test"));
        assertFalse(ns.reserveNick("test")); // второй раз нельзя
    }

    @Test
    void testCaseInsensitiveNick() {
        NicknameService ns = new NicknameService();
        assertTrue(ns.reserveNick("test"));
        assertFalse(ns.reserveNick("TEST")); // тоже нельзя
    }

    @Test
    void testReleaseNick() {
        NicknameService ns = new NicknameService();
        assertTrue(ns.reserveNick("another"));
        NicknameService.releaseNick("another");
        assertTrue(ns.reserveNick("another")); // снова можно
    }

    @Test
    void testReserveDifferentNicks() {
        NicknameService ns = new NicknameService();
        assertTrue(ns.reserveNick("test"));
        assertTrue(ns.reserveNick("other"));
    }

    @Test
    void testReleaseNullNickDoesNotThrow() {
        assertDoesNotThrow(() -> NicknameService.releaseNick(null));
    }
}
