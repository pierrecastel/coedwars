import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Inspector {

    private static class DiplomaticAuthorizationInformation {
        public static final String ACCESSIBLE_NATIONS = "ACCESSIBLE_NATIONS";
    }

    private static class Documents {
        private static final String PASSPORT = "passport";
        public static final String ACCESS_PERMIT = "access_permit";
        public static final String GRANT_OF_ASYLUM = "grant_of_asylum";
        public static final String DIPLOMATIC_AUTHORIZATION = "diplomatic_authorization";
    }

    private static class DocumentInformation {
        private static final String ID = "ID#";
        private static final String NATION = "NATION";
        private static final String NAME = "NAME";
    }

    private static class Nation {
        private static final String ARSTOTZKA = "Arstotzka";
    }

    private final List<Rule> rules = Arrays.asList(
        new RequiredDocumentsRule(),
        new WantedCriminalRule(),
        new ExpiredDocumentRule(),
        new ConflictingInformationRule(),
        new AllowedNationsRule()
    );

    public void receiveBulletin(final String bulletin) {
        updateRules(bulletin);
    }

    private void updateRules(final String bulletin) {
        System.err.println(bulletin);
        for (final String bulletinLine : bulletin.split("\n")) {
            rules.forEach(rule -> rule.extractFromBulletinLine(bulletinLine));
        }
    }

    public String inspect(final Map<String, String> person) {
        System.err.println(person);
        final String result = checkRules(person);
        System.err.println(result);
        return result;
    }

    private String checkRules(final Map<String, String> person) {
        final Map<String, Map<String, String>> documents = person.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, this::extractValue));
        final Entrant entrant = new Entrant(documents);
        for (final Rule rule : rules) {
            final Optional<String> checkResult = rule.check(entrant);
            if (checkResult.isPresent()) {
                return checkResult.get();
            }
        }
        return entrant.isForeigner() ? "Cause no trouble." : "Glory to Arstotzka.";
    }

    private Map<String, String> extractValue(final Entry<String, String> entry) {
        return Arrays.stream(entry.getValue().split("\n"))
            .collect(Collectors.toMap(s -> s.split(":")[0].trim(), s -> s.split(":")[1].trim()));
    }

    interface Rule {
        int getPriority();

        Optional<String> check(Entrant entrant);

        void extractFromBulletinLine(String bulletinLine);
    }

    static class WantedCriminalRule implements Rule {

        private static final int PRIORITY = 1;
        private static final Pattern PATTERN = Pattern.compile(":");

        private String criminalName = "UNDEFINED";

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            final String entrantName = entrant.documents.get(Documents.PASSPORT).get(DocumentInformation.NAME);
            if (criminalName.equals(entrantName)) {
                return Optional.of("Detainment: Entrant is a wanted criminal.");
            }
            return Optional.empty();
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            if (bulletinLine.contains("Wanted by the State")) { // TODO à améliorer
                final String[] firstnameAndName = PATTERN.split(bulletinLine)[1].trim().split(" ");
                criminalName = firstnameAndName[1] + ", " + firstnameAndName[0];
            }
        }
    }

    static class AllowedNationsRule implements Rule {

        private static final int PRIORITY = 3;
        private static final Pattern PATTERN = Pattern.compile(" citizens of ");

        private final Set<String> allowedNations = new HashSet<>();

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            final String entrantNation = entrant.documents.get(Documents.PASSPORT).get(DocumentInformation.NATION);
            if (!allowedNations.contains(entrantNation)) {
                return Optional.of("Entry denied: citizen of banned nation.");
            }
            return Optional.empty();
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            if (bulletinLine.contains(" citizens of ")) { // TODO à améliorer
                List<String> allowedNationsFromBulletin = Arrays.stream(PATTERN.split(bulletinLine)[1].split(","))
                    .map(String::trim)
                    .toList();
                allowedNations.addAll(allowedNationsFromBulletin);
            }
        }
    }

    static class RequiredDocumentsRule implements Rule {

        private static final int PRIORITY = 2;

        private static final Pattern REQUIRE_SEPARATOR = Pattern.compile(" require ");
        private static final Pattern COMMA_SEPARATOR = Pattern.compile(", ");

        private final Set<String> requiredDocumentsForAll = new HashSet<>();
        private final Set<String> requiredDocumentsForForeigners = new HashSet<>();

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            return checkRequiredDocuments(entrant)
                .map(missingDocument -> String.format("Entry denied: missing required %s.", missingDocument.replace('_', ' ')));
        }

        private Optional<String> checkRequiredDocuments(final Entrant entrant) {
            return checkMissingDocument(requiredDocumentsForAll, entrant)
                .or(() -> checkMissingDocumentForForeigner(entrant));
        }

        private Optional<String> checkMissingDocumentForForeigner(final Entrant entrant) {
            if (entrant.isForeigner()) {
                for (final String requiredDocument : requiredDocumentsForForeigners) {
                    if (!entrant.documents().containsKey(requiredDocument) && !entrantHasAlternativeDocument(entrant, requiredDocument)) {
                        return Optional.of(requiredDocument);
                    }
                }
            }
            return Optional.empty();
        }

        private boolean entrantHasAlternativeDocument(final Entrant entrant, final String requiredDocument) {
            if (Documents.ACCESS_PERMIT.equals(requiredDocument)) {
                return entrant.documents.entrySet().stream()
                    .anyMatch(document -> Documents.GRANT_OF_ASYLUM.equals(document.getKey())
                        || (Documents.DIPLOMATIC_AUTHORIZATION.equals(document.getKey()) && isArstotzkaInAccessibleNations(document.getValue())));
            }
            return false;
        }

        private boolean isArstotzkaInAccessibleNations(final Map<String, String> diplomaticAuthorization) {
            return Arrays.asList(COMMA_SEPARATOR.split(diplomaticAuthorization.get(DiplomaticAuthorizationInformation.ACCESSIBLE_NATIONS)))
                .contains(Nation.ARSTOTZKA);
        }

        private Optional<String> checkMissingDocument(final Set<String> requiredDocumentsForAll, final Entrant entrant) {
            for (final String requiredDocument : requiredDocumentsForAll) {
                if (!entrant.documents().containsKey(requiredDocument)) {
                    return Optional.of(requiredDocument);
                }
            }
            return Optional.empty();
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            if (bulletinLine.contains("Entrants require ")) { // TODO à améliorer
                addRequireDocuments(bulletinLine, requiredDocumentsForAll);
            } else if (bulletinLine.contains("Foreigners require ")) {
                addRequireDocuments(bulletinLine, requiredDocumentsForForeigners);
            }
        }

        private void addRequireDocuments(final String bulletinLine, final Set<String> requiredDocumentsForAll1) {
            List<String> requiredDocuments = Arrays.stream(COMMA_SEPARATOR.split(REQUIRE_SEPARATOR.split(bulletinLine)[1]))
                .map(s -> s.replace(' ', '_'))
                .toList();
            requiredDocumentsForAll1.addAll(requiredDocuments);
        }
    }

    static class ExpiredDocumentRule implements Rule {

        private static final int PRIORITY = 3;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        private static final LocalDate INITIAL_DAY = LocalDate.of(1982, 11, 22);

        private LocalDate today = INITIAL_DAY;

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            for (final Entry<String, Map<String, String>> document : entrant.documents().entrySet()) {
                if (LocalDate.parse(document.getValue().get("EXP"), DATE_FORMATTER).isBefore(today)) {
                    return Optional.of(String.format("Entry denied: %s expired.", document.getKey().replace('_', ' '))); // TODO à améliorer
                }
            }
            return Optional.empty();
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            today = today.plusDays(1);
        }
    }

    static class ConflictingInformationRule implements Rule {

        private static final int PRIORITY = 1;

        @Override
        public int getPriority() {
            return PRIORITY;
        }

        @Override
        public Optional<String> check(final Entrant entrant) {
            return checkConflictiongInformation(entrant, DocumentInformation.ID, "Detainment: ID number mismatch.")
                .or(() -> checkConflictiongInformation(entrant, DocumentInformation.NATION, "Detainment: nationality mismatch."));
        }

        private Optional<String> checkConflictiongInformation(final Entrant entrant, final String id, final String value) {
            final long differentIdValues = entrant.documents.values().stream()
                .map(document -> document.get(id))
                .filter(Objects::nonNull)
                .distinct()
                .count();

            if (differentIdValues > 1) {
                return Optional.of(value);
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
            return !Nation.ARSTOTZKA.equals(documents.get(Documents.PASSPORT).get(DocumentInformation.NATION));
        }
    }
}