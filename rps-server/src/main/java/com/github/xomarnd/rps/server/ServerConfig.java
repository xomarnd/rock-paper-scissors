package com.github.xomarnd.rps.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ServerConfig {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    private static final String CONFIG_FILE = "/server.properties";
    private static final int MIN_COMBINATIONS = 3;

    private Properties props = new Properties();
    private final Set<String> combinations;
    private final Map<String, Set<String>> beatsMap;
    private final int port;

    public ServerConfig() {
        try (InputStream in = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                log.error("Config file not found: {}", CONFIG_FILE);
                throw new RuntimeException("Config file not found: " + CONFIG_FILE);
            }
            props.load(in);
            log.info("Loaded config file: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to load config: {}", CONFIG_FILE, e);
            throw new RuntimeException("Failed to load config: " + CONFIG_FILE, e);
        }
        this.port = Integer.parseInt(props.getProperty("server.port", "5050"));
        log.info("Server port: {}", port);

        this.combinations = parseCombinations(props.getProperty("game.combinations"));
        log.info("Combinations loaded: {}", combinations);

        this.beatsMap = parseBeatsMap(props.getProperty("game.beats"));
        log.info("Beats map loaded: {}", beatsMap);

        validateConfig();
        log.info("Config validated successfully");
    }

    public ServerConfig(Properties props) {
        this.props = props;
        this.port = Integer.parseInt(props.getProperty("server.port", "5050"));
        log.debug("Server port: {}", port);
        this.combinations = parseCombinations(props.getProperty("game.combinations"));
        log.debug("Combinations loaded: {}", combinations);
        this.beatsMap = parseBeatsMap(props.getProperty("game.beats"));
        log.debug("Beats map loaded: {}", beatsMap);
        validateConfig();
        log.debug("Config validated successfully");
    }

    private Set<String> parseCombinations(String raw) {
        if (raw == null || raw.isBlank()) {
            log.error("game.combinations property missing or empty!");
            throw new RuntimeException("game.combinations property missing or empty!");
        }
        String[] split = raw.split(",");
        Set<String> set = new LinkedHashSet<>();
        for (String s : split) {
            String move = s.trim().toLowerCase();
            if (!move.isEmpty()) {
                set.add(move);
            }
        }
        log.debug("Parsed combinations: {}", set);
        return Collections.unmodifiableSet(set);
    }

    private Map<String, Set<String>> parseBeatsMap(String raw) {
        Map<String, Set<String>> map = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            log.error("game.beats property missing or empty!");
            throw new RuntimeException("game.beats property missing or empty!");
        }
        String[] pairs = raw.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length != 2) {
                log.error("Invalid game.beats pair: '{}'. Format must be move1:move2,move3...", pair);
                throw new RuntimeException("Invalid game.beats pair: '" + pair + "'. Format must be move1:move2,move3...");
            }
            String move = parts[0].trim().toLowerCase();
            String[] beats = parts[1].split(",");
            Set<String> beatsSet = new HashSet<>();
            for (String b : beats) {
                String beat = b.trim().toLowerCase();
                if (!beat.isEmpty()) {
                    beatsSet.add(beat);
                }
            }
            map.put(move, beatsSet);
            log.debug("Parsed beats: {} beats {}", move, beatsSet);
        }
        return map;
    }

    private void validateConfig() {
        if (combinations.size() < MIN_COMBINATIONS) {
            log.error("Too few combinations in game.combinations: must be at least {}, found: {}", MIN_COMBINATIONS, combinations.size());
            throw new RuntimeException("Too few combinations in game.combinations: must be at least " + MIN_COMBINATIONS + ", found: " + combinations.size());
        }
        if (combinations.size() % 2 == 0) {
            log.error("Number of combinations must be odd! Found: {}", combinations.size());
            throw new RuntimeException("Number of combinations must be odd! Found: " + combinations.size());
        }
        for (String move : combinations) {
            if (!beatsMap.containsKey(move)) {
                log.error("No beats mapping for move: {}", move);
                throw new RuntimeException("No beats mapping for move: " + move);
            }
        }
        for (Map.Entry<String, Set<String>> entry : beatsMap.entrySet()) {
            String move = entry.getKey();
            Set<String> beats = entry.getValue();
            if (beats.isEmpty()) {
                log.error("Move '{}' does not beat anyone!", move);
                throw new RuntimeException("Move '" + move + "' does not beat anyone! Each move must beat at least one other.");
            }
            for (String b : beats) {
                if (!combinations.contains(b)) {
                    log.error("Move '{}' beats unknown move '{}'. Check spelling in game.beats!", move, b);
                    throw new RuntimeException("Move '" + move + "' beats unknown move '" + b + "'. Check spelling in game.beats!");
                }
                if (b.equals(move)) {
                    log.error("Move '{}' cannot beat itself!", move);
                    throw new RuntimeException("Move '" + move + "' cannot beat itself!");
                }
            }
        }
    }

    public int getPort() {
        return port;
    }

    public Set<String> getCombinations() {
        return combinations;
    }

    public Map<String, Set<String>> getBeatsMap() {
        return beatsMap;
    }
}
