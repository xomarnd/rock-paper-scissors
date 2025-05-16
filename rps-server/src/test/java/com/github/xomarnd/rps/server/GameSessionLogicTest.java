package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.GameLogicService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionLogicTest {

    private static GameLogicService classicLogic;
    private static GameLogicService rpslsLogic;
    private static ServerConfig classicConfig;
    private static ServerConfig rpslsConfig;

    @BeforeAll
    static void setupConfigs() {
        Properties propsClassic = new Properties();
        propsClassic.setProperty("server.port", "5050");
        propsClassic.setProperty("game.combinations", "rock,paper,scissors");
        propsClassic.setProperty("game.beats", "rock:scissors;paper:rock;scissors:paper");
        propsClassic.setProperty("log.level", "INFO");
        classicConfig = new ServerConfig(propsClassic);

        Properties propsRPSLS = new Properties();
        propsRPSLS.setProperty("server.port", "5050");
        propsRPSLS.setProperty("game.combinations", "rock,paper,scissors,lizard,spock");
        propsRPSLS.setProperty("game.beats", "rock:scissors,lizard;paper:rock,spock;scissors:paper,lizard;lizard:spock,paper;spock:scissors,rock");
        propsRPSLS.setProperty("log.level", "INFO");
        rpslsConfig = new ServerConfig(propsRPSLS);

        classicLogic = new GameLogicService(classicConfig.getCombinations(), classicConfig.getBeatsMap());
        rpslsLogic = new GameLogicService(rpslsConfig.getCombinations(), rpslsConfig.getBeatsMap());
    }

    @Test
    void testClassicDraw() {
        GameLogicService.GameResult result = classicLogic.determineResult("rock", "rock", "A", "B");
        assertTrue(result.resultText.toLowerCase().contains("draw"), "Draw expected for same move");
    }

    @Test
    void testClassicWinLose() {
        GameLogicService.GameResult r1 = classicLogic.determineResult("rock", "scissors", "A", "B");
        assertTrue(r1.resultText.contains("A wins"), "A should win with rock vs scissors");
        GameLogicService.GameResult r2 = classicLogic.determineResult("paper", "rock", "A", "B");
        assertTrue(r2.resultText.contains("A wins"), "A should win with paper vs rock");
        GameLogicService.GameResult r3 = classicLogic.determineResult("scissors", "paper", "A", "B");
        assertTrue(r3.resultText.contains("A wins"), "A should win with scissors vs paper");
        GameLogicService.GameResult r4 = classicLogic.determineResult("scissors", "rock", "A", "B");
        assertTrue(r4.resultText.contains("B wins"), "B should win with rock vs scissors");
    }

    @Test
    void testClassicInvalid() {
        GameLogicService.GameResult result = classicLogic.determineResult("rock", "banana", "A", "B");
        assertTrue(result.resultText.toLowerCase().contains("invalid"), "Invalid combination should be detected");
    }

    @Test
    void testRPSLSRules() {
        GameLogicService.GameResult r1 = rpslsLogic.determineResult("rock", "lizard", "A", "B");
        assertTrue(r1.resultText.contains("A wins"), "A wins with rock vs lizard");
        GameLogicService.GameResult r2 = rpslsLogic.determineResult("lizard", "spock", "A", "B");
        assertTrue(r2.resultText.contains("A wins"), "A wins with lizard vs spock");
        GameLogicService.GameResult r3 = rpslsLogic.determineResult("spock", "rock", "A", "B");
        assertTrue(r3.resultText.contains("A wins"), "A wins with spock vs rock");
        GameLogicService.GameResult r4 = rpslsLogic.determineResult("scissors", "spock", "A", "B");
        assertTrue(r4.resultText.contains("B wins"), "B wins with spock vs scissors");
        GameLogicService.GameResult r5 = rpslsLogic.determineResult("paper", "scissors", "A", "B");
        assertTrue(r5.resultText.contains("B wins"), "B wins with scissors vs paper");
    }

    @Test
    void testAllMovesAgainstEachOther() {
        List<String> moves = new ArrayList<>(classicConfig.getCombinations());
        for (String move1 : moves) {
            for (String move2 : moves) {
                GameLogicService.GameResult result = classicLogic.determineResult(move1, move2, "A", "B");
                if (move1.equals(move2)) {
                    assertTrue(result.resultText.toLowerCase().contains("draw"));
                } else if (classicConfig.getBeatsMap().get(move1).contains(move2)) {
                    assertTrue(result.resultText.contains("A wins"), move1 + " should beat " + move2);
                } else if (classicConfig.getBeatsMap().get(move2).contains(move1)) {
                    assertTrue(result.resultText.contains("B wins"), move2 + " should beat " + move1);
                } else {
                    assertTrue(result.resultText.toLowerCase().contains("invalid"));
                }
            }
        }
    }

    @Test
    void testRPSLSInvalidMove() {
        GameLogicService.GameResult result = rpslsLogic.determineResult("banana", "spock", "A", "B");
        assertTrue(result.resultText.toLowerCase().contains("invalid"), "Invalid move must be reported");
    }

    @Test
    void testRPSLSDraw() {
        GameLogicService.GameResult result = rpslsLogic.determineResult("lizard", "lizard", "A", "B");
        assertTrue(result.resultText.toLowerCase().contains("draw"), "Draw for lizard vs lizard");
    }
}
