import java.util.List;
import java.util.Map;

public class Inspector {

    private List<Rule> rules;

    public void receiveBulletin(String bulletin) {
        // Your code here

    }

    public String inspect(Map<String, String> person) {
        // Your code here
        return "";
    }

    interface Rule {
        boolean check(Entrant entrant);
    }

    static class Entrant {
        Passport passport;
    }

    static class Foreigner extends Entrant {

    }

    static class Passport {

    }
}