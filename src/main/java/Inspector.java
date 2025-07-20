import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Inspector {

    private static final LocalDate INITIAL_DAY = LocalDate.of(1982, 11, 22);

    private LocalDate today = INITIAL_DAY;

    private static class DiplomaticAuthorizationInformation {
        private static final String ACCESSIBLE_NATIONS = "ACCESS";
    }

    private static class Documents {
        private static final String PASSPORT = "passport";
        private static final String ACCESS_PERMIT = "access_permit";
        private static final String GRANT_OF_ASYLUM = "grant_of_asylum";
        private static final String DIPLOMATIC_AUTHORIZATION = "diplomatic_authorization";
        private static final String CERTIFICATE_OF_VACCINATION = "certificate_of_vaccination";
    }

    private static class DocumentInformation {
        private static final String ID = "ID#";
        private static final String NATION = "NATION";
        private static final String NAME = "NAME";
        private static final String VACCINES = "VACCINES";
    }

    private static class Nation {
        private static final String ARSTOTZKA = "Arstotzka";
    }

    private final List<Rule> rules = Arrays.asList(
        new WantedCriminalRule(),
        new ConflictingInformationRule(),
        new RequiredDocumentsRule(),
        new ExpiredDocumentRule(),
        new AllowedNationsRule(),
        new VaccinationRule()
    );

    public void receiveBulletin(final String bulletin) {
        addOneDay();
        System.err.println("Today is " + today);
        updateRules(bulletin);
    }

    public String inspect(final Map<String, String> person) {
        System.err.println(person);
        final String result = checkRules(person);
        System.err.println(result);
        return result;
    }

    private void addOneDay() {
        today = today.plusDays(1);
    }

    private void updateRules(final String bulletin) {
        System.err.println(bulletin);
        for (final String bulletinLine : bulletin.split("\n")) {
            rules.forEach(rule -> rule.extractFromBulletinLine(bulletinLine));
        }
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
        Optional<String> check(Entrant entrant);

        void extractFromBulletinLine(String bulletinLine);
    }

    private static class WantedCriminalRule implements Rule {

        private static final Pattern PATTERN = Pattern.compile(":");
        private static final String UNDEFINED_CRIMINAL = "";

        private String criminalName = UNDEFINED_CRIMINAL;

        @Override
        public Optional<String> check(final Entrant entrant) {
            final boolean isCriminal = entrant.documents.values().stream()
                .anyMatch(document -> criminalName.equals(document.get(DocumentInformation.NAME)));
            if (isCriminal) {
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

    private static class AllowedNationsRule implements Rule {

        private static final Pattern NATION_SEPARATOR = Pattern.compile(" citizens of ");

        private final Set<String> allowedNations = new HashSet<>();

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
            if (bulletinLine.contains("Allow citizens of ")) { // TODO à améliorer
                Arrays.stream(NATION_SEPARATOR.split(bulletinLine)[1].split(","))
                    .map(String::trim)
                    .forEach(allowedNations::add);
            } else if (bulletinLine.contains("Deny citizens of ")) {
                Arrays.stream(NATION_SEPARATOR.split(bulletinLine)[1].split(","))
                    .map(String::trim)
                    .forEach(allowedNations::remove);
            }
        }
    }

    private static class RequiredDocumentsRule implements Rule {

        public static final Pattern REQUIRED_DOCUMENTS_PATTERN = Pattern.compile("(.*) require (.*)(?<!vaccination)$");
        private static final Pattern COMMA_SEPARATOR = Pattern.compile(", ");

        private final Set<String> requiredDocumentsForAll = new HashSet<>();
        private final Set<String> requiredDocumentsForForeigners = new HashSet<>();
        private final Set<String> requiredDocumentsForCitizens = new HashSet<>();

        @Override
        public Optional<String> check(final Entrant entrant) {
            return checkInvalidDiplomaticAuthorization(entrant).map(diplomaticAuthorization -> "Entry denied: invalid diplomatic authorization.")
                .or(() -> checkRequiredDocuments(entrant)
                    .map(missingDocument -> String.format("Entry denied: missing required %s.", missingDocument.replace('_', ' '))));
        }

        private Optional<String> checkInvalidDiplomaticAuthorization(final Entrant entrant) {
            return entrant.documents.entrySet().stream()
                .filter(document ->
                    Documents.DIPLOMATIC_AUTHORIZATION.equals(document.getKey()) && !isArstotzkaInAccessibleNations(document.getValue()))
                .map(stringMapEntry -> "")
                .findFirst();
        }

        private Optional<String> checkRequiredDocuments(final Entrant entrant) {
            return checkMissingDocument(requiredDocumentsForAll, entrant)
                .or(() -> checkMissingDocumentForForeigner(entrant))
                .or(() -> checkMissingDocumentForCitizen(entrant));
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

        private Optional<String> checkMissingDocumentForCitizen(final Entrant entrant) {
            if (!entrant.isForeigner()) {
                for (final String requiredDocument : requiredDocumentsForCitizens) {
                    if (!entrant.documents().containsKey(requiredDocument)) {
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
                        || Documents.DIPLOMATIC_AUTHORIZATION.equals(document.getKey()));
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
            final Matcher matcher = REQUIRED_DOCUMENTS_PATTERN.matcher(bulletinLine);
            if (matcher.matches()) {
                final String target = matcher.group(1);
                final Set<String> targetList = switch (target) {
                    case "Entrants" -> requiredDocumentsForAll;
                    case "Foreigners" -> requiredDocumentsForForeigners;
                    case "Citizens of Arstotzka" -> requiredDocumentsForCitizens;
                    default -> throw new IllegalStateException("Unexpected value: " + target);
                };
                final String requiredDocuments = matcher.group(2);
                addRequireDocuments(requiredDocuments, targetList);
            }
        }

        private void addRequireDocuments(final String requiredDocuments, final Set<String> requiredDocumentsForAll) {
            Arrays.stream(COMMA_SEPARATOR.split(requiredDocuments))
                .map(s -> s.replace(' ', '_'))
                .forEach(requiredDocumentsForAll::add);
        }
    }

    private static class VaccinationRule implements Rule {

        private static final Pattern VACCINATION_PATTERN = Pattern.compile("(.*) require (.*) vaccination");
        private static final Pattern COMMA_SEPARATOR = Pattern.compile(", ");

        private final Set<String> requiredVaccinationsForAll = new HashSet<>();
        private final Map<String, Set<String>> requiredVaccinationsByNation = new HashMap<>();

        @Override
        public Optional<String> check(final Entrant entrant) {
            return checkRequiredVaccinationsForAll(entrant)
                .or(() -> checkRequiredVaccinationsByCountry(entrant))
                .map("Entry denied: missing required %s vaccination."::formatted);
        }

        private Optional<String> checkRequiredVaccinationsForAll(final Entrant entrant) {
            for (final String requiredVaccination : requiredVaccinationsForAll) {
                final Map<String, String> certificateOfVaccination = entrant.documents.get(Documents.CERTIFICATE_OF_VACCINATION);
                if (certificateOfVaccination == null || hasNotRequiredVaccination(requiredVaccination, certificateOfVaccination)) {
                    return Optional.of(requiredVaccination);
                }
            }
            return Optional.empty();
        }

        private Optional<String> checkRequiredVaccinationsByCountry(final Entrant entrant) {
            if (requiredVaccinationsByNation.containsKey(entrant.getNation())) {
                for (final String requiredVaccination : requiredVaccinationsByNation.get(entrant.getNation())) {
                    final Map<String, String> certificateOfVaccination = entrant.documents.get(Documents.CERTIFICATE_OF_VACCINATION);
                    if (certificateOfVaccination == null || hasNotRequiredVaccination(requiredVaccination, certificateOfVaccination)) {
                        return Optional.of(requiredVaccination);
                    }
                }
            }
            return Optional.empty();
        }

        private boolean hasNotRequiredVaccination(final String requiredVaccination, final Map<String, String> certificateOfVaccination) {
            return Arrays.stream(COMMA_SEPARATOR.split(certificateOfVaccination.get(DocumentInformation.VACCINES)))
                .noneMatch(requiredVaccination::equals);
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            final Matcher matcher = VACCINATION_PATTERN.matcher(bulletinLine);
            if (matcher.matches()) {
                final List<String> vaccinations = Arrays.asList(COMMA_SEPARATOR.split(matcher.group(2)));
                final String target = matcher.group(1);
                if (target.equals("Entrants")) {
                    requiredVaccinationsForAll.addAll(vaccinations);
                } else {
                    addRequiredVaccinationsForNations(target, vaccinations);
                }
            }
        }

        private void addRequiredVaccinationsForNations(final String target, final List<String> vaccinations) {
            Arrays.stream(COMMA_SEPARATOR.split(target))
                .forEach(nation -> requiredVaccinationsByNation.put(nation, new HashSet<>(vaccinations)));
        }
    }

    private class ExpiredDocumentRule implements Rule {

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        @Override
        public Optional<String> check(final Entrant entrant) {
            for (final Entry<String, Map<String, String>> document : entrant.documents().entrySet()) {
                if (isDateExpired(document)) {
                    return Optional.of(String.format("Entry denied: %s expired.", document.getKey().replace('_', ' '))); // TODO à améliorer
                }
            }
            return Optional.empty();
        }

        private boolean isDateExpired(final Entry<String, Map<String, String>> document) {
            final String expirationDate = document.getValue().get("EXP");
            return expirationDate != null && LocalDate.parse(expirationDate, DATE_FORMATTER).isBefore(today);
        }

        @Override
        public void extractFromBulletinLine(final String bulletinLine) {
            // Nothing to do.
        }
    }

    private static class ConflictingInformationRule implements Rule {

        @Override
        public Optional<String> check(final Entrant entrant) {
            return checkConflictingInformation(entrant, DocumentInformation.ID, "Detainment: ID number mismatch.")
                .or(() -> checkConflictingInformation(entrant, DocumentInformation.NATION, "Detainment: nationality mismatch."));
        }

        private Optional<String> checkConflictingInformation(final Entrant entrant, final String information, final String value) {
            final long differentIdValues = entrant.documents.values().stream()
                .map(document -> document.get(information))
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
            return !Nation.ARSTOTZKA.equals(getNation());
        }

        public String getNation() {
            return documents.get(Documents.PASSPORT).get(DocumentInformation.NATION);
        }
    }
}