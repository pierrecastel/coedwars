import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Day6Test {

    private final Inspector inspector = new Inspector();

    @BeforeEach
    void setUpBulletin() {
        inspector.receiveBulletin(
            """
                Entrants require passport
                Allow citizens of Arstotzka
                Wanted by the State: Isaak Strauss
                """);
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria, Impor, Kolechia, Obristan, Republia, United Federation
                Wanted by the State: Sarah Czekowicz
                """);
        inspector.receiveBulletin(
            """
                Foreigners require access permit
                Wanted by the State: Hector Radic
                """);
        inspector.receiveBulletin(
            """
                Deny citizens of Antegria
                Citizens of Arstotzka require ID card
                """);
        inspector.receiveBulletin(
            """
                Deny citizens of Antegria
                """);
    }

    @Test
    void validForeigner_WithNeededVaccination() {
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria
                Deny citizens of Kolechia
                Entrants require polio vaccination
                """);

        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("grant_of_asylum",
            """
                NAME: Wagner, Khalid
                NATION: Antegria
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);

        entrant.put("certificate_of_vaccination",
            """
                NAME: Wagner, Khalid
                ID#: S9GRA-KO17I
                VACCINES: polio, HPV, cowpox
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void invalidForeigner_WithoutNeededVaccination() {
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria
                Deny citizens of Kolechia
                Entrants require polio vaccination
                """);

        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("grant_of_asylum",
            """
                NAME: Wagner, Khalid
                NATION: Antegria
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);

        entrant.put("certificate_of_vaccination",
            """
                NAME: Wagner, Khalid
                ID#: S9GRA-KO17I
                VACCINES: HPV, cowpox
                """);

        assertEquals("Entry denied: missing required polio vaccination.", inspector.inspect(entrant));
    }

    @Test
    void invalidForeigner_WithoutCertificateOfVaccination() {
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria
                Deny citizens of Kolechia
                Entrants require polio vaccination
                """);

        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("grant_of_asylum",
            """
                NAME: Wagner, Khalid
                NATION: Antegria
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);

        assertEquals("Entry denied: missing required polio vaccination.", inspector.inspect(entrant));
    }

    @Test
    void validForeigner_NotTargetedByVaccination() {
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria
                Deny citizens of Kolechia
                Citizens of United Federation, Kolechia require polio vaccination
                """);

        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);
        entrant.put("grant_of_asylum",
            """
                NAME: Wagner, Khalid
                NATION: Antegria
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void validForeigner_TargetedByVaccinationByNation() {
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria
                Deny citizens of Kolechia
                Citizens of United Federation, Kolechia require polio vaccination
                """);

        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);
        entrant.put("grant_of_asylum",
            """
                NAME: Wagner, Khalid
                NATION: Antegria
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);
        entrant.put("certificate_of_vaccination",
            """
                NAME: Wagner, Khalid
                ID#: S9GRA-KO17I
                VACCINES: HPV, polio, cowpox
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void invalidForeigner_TargetedByVaccinationForForeigners() {
        inspector.receiveBulletin(
            """
                Allow citizens of Antegria
                Deny citizens of Kolechia
                Foreigners require polio vaccination
                """);

        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);
        entrant.put("grant_of_asylum",
            """
                NAME: Wagner, Khalid
                NATION: Antegria
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);
        entrant.put("certificate_of_vaccination",
            """
                NAME: Wagner, Khalid
                ID#: S9GRA-KO17I
                VACCINES: HPV, cowpox
                """);

        assertEquals("Entry denied: missing required polio vaccination.", inspector.inspect(entrant));
    }
}