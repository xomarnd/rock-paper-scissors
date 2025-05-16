package com.github.xomarnd.rps.server;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    private ServerConfig makeConfig(String combos, String beats) {
        Properties props = new Properties();
        props.setProperty("server.port", "5050");
        props.setProperty("game.combinations", combos);
        props.setProperty("game.beats", beats);
        props.setProperty("log.level", "INFO");
        return new ServerConfig(props);
    }

    @Test
    void testValidClassic() {
        ServerConfig config = makeConfig(
                "rock,paper,scissors",
                "rock:scissors;paper:rock;scissors:paper"
        );
        assertEquals(3, config.getCombinations().size());
        assertTrue(config.getBeatsMap().get("rock").contains("scissors"));
    }

    @Test
    void testMoveMustBeatAtLeastOne() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                makeConfig("rock,paper,scissors", "rock: ;paper:rock;scissors:paper")
        );
        assertTrue(ex.getMessage().contains("does not beat anyone"));
    }

    @Test
    void testInvalidBeatsFormat() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                makeConfig("rock,paper,scissors", "rock:;paper:rock;scissors:paper")
        );
        assertTrue(ex.getMessage().contains("Invalid game.beats pair"));
    }


    @Test
    void testInvalidBeatsReference() {
        Exception ex = assertThrows(RuntimeException.class, () -> makeConfig(
                "rock,paper,scissors", "rock:paper;paper:rock;scissors:banana"
        ));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown move"));
    }

    @Test
    void testMoveCantBeatItself() {
        Exception ex = assertThrows(RuntimeException.class, () -> makeConfig(
                "rock,paper,scissors", "rock:rock;paper:rock;scissors:paper"
        ));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot beat itself"));
    }
}
