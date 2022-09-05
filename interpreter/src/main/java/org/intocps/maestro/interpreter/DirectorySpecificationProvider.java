package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.node.ARootDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectorySpecificationProvider implements ITransitionManager.ISpecificationProvider {
    final static Logger logger = LoggerFactory.getLogger(DirectorySpecificationProvider.class);
    final File root;
    final Function<File, ARootDocument> parseAndCheck;
    final Map<Path, ARootDocument> candidates = new HashMap<>();
    final Set<Path> removedCandidates = new HashSet<>();
    final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.mabl");
    private final int checkFrequency;
    private final int minimumStepBeforeOffering;
    int offeringCounter = 0;
    long lastChecked;

    public DirectorySpecificationProvider(File root, Function<File, ARootDocument> parseAndCheck) {
        this(root, parseAndCheck, 10, 0);
    }

    public DirectorySpecificationProvider(File root, Function<File, ARootDocument> parseAndCheck, int checkFrequency, int minimumStepBeforeOffering) {
        this.root = root;
        this.parseAndCheck = parseAndCheck;
        this.checkFrequency = checkFrequency;
        this.minimumStepBeforeOffering = minimumStepBeforeOffering;
    }

    @Override
    public Map<Path, ARootDocument> get() {
        if (System.currentTimeMillis() - lastChecked > (checkFrequency * 1000L)) {
            if (root != null && root.exists()) {
                try (Stream<Path> walker = Files.walk(root.toPath())) {
                    List<Path> specPaths = walker.filter(Files::isRegularFile).filter(pathMatcher::matches).collect(Collectors.toList());

                    for (Path path : specPaths) {
                        if (!removedCandidates.contains(path) && !candidates.containsKey(path)) {
                            try {
                                logger.debug("Processing path: {}", path);
                                ARootDocument spec = parseAndCheck.apply(path.toFile());
                                if (spec != null) {
                                    logger.debug("Processing path: {}. Done spec found.", path);
                                    candidates.put(path, spec);
                                } else {
                                    logger.debug("Processing path: {}. Invalid spec path.", path);
                                }
                            } catch (Exception e) {
                                logger.error("Failed to parse candidate: " + path, e);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            lastChecked = System.currentTimeMillis();
        }

        if (!candidates.isEmpty()) {
            offeringCounter++;
            if (minimumStepBeforeOffering > offeringCounter) {
                return new HashMap<>();
            }
        }

        return candidates;
    }

    @Override
    public Map<Path, ARootDocument> get(String name) {
        return get();
    }

    @Override
    public void remove(ARootDocument specification) {
        candidates.entrySet().stream().filter(map -> map.getValue().equals(specification)).map(Map.Entry::getKey).findFirst().ifPresent(key -> {
            candidates.remove(key);
            removedCandidates.add(key);
            offeringCounter = 0;
        });
    }
}
