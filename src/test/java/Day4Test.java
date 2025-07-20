import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Day4Test {

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
    }

    @Test
    void validForeigner_AlternativeAccessPermit_GrantOfAsylum() {
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

        assertEquals("Entry denied: citizen of banned nation.", inspector.inspect(entrant));
    }

    @Test
    void valid_CitizenOfArstotzka_requireIdCard() {
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

        entrant.put("ID_card",
            """
                NAME: Jager, Cameron
                DOB: 1956.10.16
                HEIGHT: 162.0cm
                WEIGHT: 65.0kg
                """);

        assertEquals("Glory to Arstotzka.", inspector.inspect(entrant));
    }

    @Test
    void invalid_citizenOfArstotzka_requireIdCard() {
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

        assertEquals("Entry denied: missing required ID card.", inspector.inspect(entrant));
    }
}