import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Day1Test {

    private final Inspector inspector = new Inspector();

    @BeforeEach
    void setUpBulletin() {
        inspector.receiveBulletin(
            """
                Entrants require passport
                Allow citizens of Arstotzka
                Wanted by the State: Victoria Steinberg
                """);
    }

    @Test
    void citizenOfArstotzka() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Arstotzka
                DOB: 1956.10.16
                SEX: F
                ISS: Orvech Vonor
                ID#: V8K6H-FIS4C
                EXP: 1983.03.17
                NAME: Jager, Cameron
                """);

        assertEquals("Glory to Arstotzka.", inspector.inspect(entrant));
    }

    @Test
    void bannedNation() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Republia
                DOB: 1948.11.16
                SEX: F
                ISS: Lesrenadi
                ID#: D0ILI-ASEPD
                EXP: 1984.10.31
                NAME: Peterson, Alberta
                """);

        assertEquals("Entry denied: citizen of banned nation.", inspector.inspect(entrant));
    }

    @Test
    void passeportExpired() {
        Map<String, String> entrant = new HashMap<>();
        entrant.put("passport",
            """
                NATION: Impor
                DOB: 1929.04.15
                SEX: M
                ISS: Enkyo
                ID#: WZPFX-U44G8
                EXP: 1981.02.07
                NAME: Ortiz, Petr
                """);

        assertEquals("Entry denied: passport expired.", inspector.inspect(entrant));
    }

    @Test
    void wantedCriminal() {
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

        assertEquals("Detainment: Entrant is a wanted criminal.", inspector.inspect(entrant));
    }
}