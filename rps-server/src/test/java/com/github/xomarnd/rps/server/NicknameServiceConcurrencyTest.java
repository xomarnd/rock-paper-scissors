package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.NicknameService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NicknameServiceConcurrencyTest {

    @Test
    void testConcurrentNickReservation() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        String nick = "superNick";
        AtomicInteger successfulReserves = new AtomicInteger(0);
        NicknameService.releaseNick(nick); // очистим перед тестом

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    NicknameService ns = new NicknameService();
                    if (ns.reserveNick(nick)) {
                        successfulReserves.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await();

        assertEquals(1, successfulReserves.get(),
                "Only one thread should successfully reserve the nickname");

        NicknameService.releaseNick(nick);
        assertTrue(new NicknameService().reserveNick(nick),
                "Nickname should be available after release");
        NicknameService.releaseNick(nick);
    }
}
