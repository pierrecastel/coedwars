package codewars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Inspector {

    private static class Documents {
        private static final String PASSPORT = "passport";
    }

    private static class DocumentInformation {
        private static final String ID = "ID#";
        private static final String NATION = "NATION";
    }

    private static class Nation {
        private static final String ARSTOTZKA = "Arstotzka";
    }

    private final List<Rule> rules = Arrays.asList(
        new RequiredDocumentsRule(),
        new ConflictingInformationRule()
    );

    public void receiveBulletin(final String bulletin) {
        updateRules(bulletin);
    }

    private void updateRules(final String bulletin) {
        for (final String bulletinLine : bulletin.split("\n")) {
            rules.forEach(rule -> rule.extractFromBulletinLine(bulletinLine));
        }
    }

    public String inspect(final Map<String, String> person) {
        final Map<String, Map<String, String>> documents = person.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, this::extractValue));
        final Entrant entrant = new Entrant(documents);
        for (final Rule rule : rules) {
            final Optional<String> checkResult = rule.check(entrant);
            if (checkResult.isPresent()) {
                return checkResult.get();
            }
        }
        return "Glory to Arstotzka.";
    }

    private Map<String, String> extractValue(final Map.Entry<String, String> entry) {
        return Arrays.stream(entry.getValue().split("\n"))
            .collect(Collectors.toMap(s -> s.split(":")[0], s -> s.split(":")[1]));
    }

    interface Rule {
        int getPriority();

        Optional<String> check(Entrant entrant);

        void extractFromBulletinLine(String bulletinLine);
    }

    static class RequiredDocumentsRule implements Rule {

        private static final Pattern PATTERN = Pattern.compile(" require ");
        private static final int PRIORITY = 2;

        private final List<String> requiredDocumentsForAll = new ArrayList<>();

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            for (final String requiredDocument : requiredDocumentsForAll) {
                if (!entrant.documents().containsKey(requiredDocument)) {
                    return Optional.of("Entry denied: missing required " + requiredDocument + '.');
                }
            }
            return Optional.empty();
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            if (bulletinLine.contains(" require ")) {
                List<String> requiredDocuments = Arrays.stream(PATTERN.split(bulletinLine)[1].split(","))
                    .map(String::trim)
                    .toList();
                requiredDocumentsForAll.addAll(requiredDocuments);
            }
        }
    }

    static class ConflictingInformationRule implements Rule {

        private static final int PRIORITY = 2;

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            if (entrant.isForeigner()) {
                final String idFromPassport = entrant.documents.get(Documents.PASSPORT).get(DocumentInformation.ID);
                final String idFromGrantOfAsylum = entrant.documents.get("grant_of_asylum").get(DocumentInformation.ID);
                if (!idFromPassport.equals(idFromGrantOfAsylum)) {
                    return Optional.of("Detainment: ID number mismatch.");
                }
            }
            return Optional.empty();
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            // Nothing to do
        }
    }

    record Entrant(Map<String, Map<String, String>> documents) {
        public boolean isForeigner() {
            return Nation.ARSTOTZKA.equals(documents.get(Documents.PASSPORT).get(DocumentInformation.NATION));
        }
    }
}