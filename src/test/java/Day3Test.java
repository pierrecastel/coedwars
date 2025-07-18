import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Day3Test {

    private final Inspector inspector = new Inspector();

    @BeforeEach
    void setUpBulletin() {
        inspector.receiveBulletin(
            """
                Entrants require passport
                Allow citizens of Arstotzka
                Wanted by the State: Isaak Strauss
                Allow citizens of Antegria, Impor, Kolechia, Obristan, Republia, United Federation
                Wanted by the State: Sarah Czekowicz
                Foreigners require access permit
                Wanted by the State: Hector Radic
                """);
    }

    @Test
    void validForeigner_AlternativeAccessPermit_GrantOfAsylum() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Obristan
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
                NATION: Obristan
                ID#: S9GRA-KO17I
                DOB: 1919.02.10
                HEIGHT: 176.0cm
                WEIGHT: 84.0kg
                EXP: 1984.02.26
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void validForeigner_AlternativeAccessPermit_DiplomaticAuthorization_WithArstotzkaInAccessiblesNations () {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Obristan
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("diplomatic_authorization",
            """
                NAME: Wagner, Khalid
                NATION: Obristan
                ID#: S9GRA-KO17I
                ACCESS: Impor, Arstotzka, United Federation
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void invalidForeigner_AlternativeAccessPermit_DiplomaticAuthorization_WithoutArstotzkaInAccessiblesNations () {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Obristan
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("diplomatic_authorization",
            """
                NAME: Wagner, Khalid
                NATION: Obristan
                ID#: S9GRA-KO17I
                ACCESS: Impor, United Federation
                """);

        assertEquals("Entry denied: missing required access permit.", inspector.inspect(entrant));
    }

    @Test
    void conflictingInformation_NationalityMismatch() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Obristan
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1984.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("diplomatic_authorization",
            """
                NAME: Wagner, Khalid
                NATION: Impor
                ID#: S9GRA-KO17I
                ACCESS: Impor, Arstotzka, United Federation
                """);

        assertEquals("Detainment: nationality mismatch.", inspector.inspect(entrant));
    }

    @Test
    void conflictingInformationAndExpiredPassport () {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Obristan
                DOB: 1919.02.10
                SEX: M
                ISS: Mergerous
                ID#: S9GRA-KO17I
                EXP: 1974.11.01
                NAME: Wagner, Khalid
                """);

        entrant.put("diplomatic_authorization",
            """
                NAME: Wagner, Khalid
                NATION: Impor
                ID#: S9GRA-KO17I
                ACCESS: Impor, Arstotzka, United Federation
                """);

        assertEquals("Detainment: nationality mismatch.", inspector.inspect(entrant));
    }
}