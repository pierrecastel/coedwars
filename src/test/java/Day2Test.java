import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Day2Test {

    private final Inspector inspector = new Inspector();

    @BeforeEach
    void setUpBulletin() {
        inspector.receiveBulletin(
            """
                Entrants require passport
                Allow citizens of Arstotzka
                Wanted by the State: Victoria Steinberg
                Allow citizens of Antegria, Impor, Kolechia, Obristan, Republia, United Federation
                Wanted by the State: Andrew Rosebrova
                """);
    }

    @Test
    void validForeigner() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Antegria
                DOB: 1941.02.27
                SEX: F
                ISS: Glorian
                ID#: ANMB3-XGHGD
                EXP: 1983.03.19
                NAME: Ramos, Sofia
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void validForeignerWithSpaceInCountryName() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: United Federation
                DOB: 1937.08.14
                SEX: M
                ISS: Korista City
                ID#: XRLIE-D1AT3
                EXP: 1985.01.28
                NAME: Stanislov, Vadim
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }

    @Test
    void wantedCriminal() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Kolechia
                DOB: 1941.03.07
                SEX: M
                ISS: Yurko City
                ID#: GYLEZ-WRL8T
                EXP: 1984.10.24
                NAME: Rosebrova, Andrew
                """);

        assertEquals("Detainment: Entrant is a wanted criminal.", inspector.inspect(entrant));
    }

    @Test
    void formerWantedCriminal() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                passport=NATION: Antegria
                DOB: 1955.11.13
                SEX: F
                ISS: St. Marmero
                ID#: CSBWD-WQRW1
                EXP: 1983.01.13
                NAME: Steinberg, Victoria
                """);

        assertEquals("Entry denied: citizen of banned nation.", inspector.inspect(entrant));
    }

    @Test
    void errorPassportExpired() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Republia
                DOB: 1935.09.17
                SEX: F
                ISS: True Glorian
                ID#: UCOWN-RBMVS
                EXP: 1982.11.26
                NAME: Fischer, Anastasia
                """);

        assertEquals("Cause no trouble.", inspector.inspect(entrant));
    }
}