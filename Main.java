import java.util.*;

/*
  super simple concept demo.
    no frameworks or any fancy algorithms
    just a few hardcoded routes that print nicely
*/

public class Main {

    // tiny step struct
    static class Step {
        String mode;   // WALK, MONORAIL, BUS, SKYLINER, BOAT
        String from;
        String to;
        int minutes;   // super rough estimate

        Step(String mode, String from, String to, int minutes) {
            this.mode = mode;
            this.from = from;
            this.to = to;
            this.minutes = minutes;
        }
    }

    public static void main(String[] args) {
        // defaults show off the Poly -> EPCOT example
        String start = "Disney's Polynesian Village Resort";
        String dest  = "EPCOT";
        String timeParam = null; // optional "time=HH:MM"

        // read args; allow multi-word names; last non-time arg is destination
        List<String> words = new ArrayList<>();
        for (String a : args) {
            if (a.toLowerCase().startsWith("time=")) timeParam = a.substring(5);
            else words.add(a);
        }

        // if no non-time args were passed, prompt the user interactively
        if (words.isEmpty()) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Enter start (e.g., Poly, EPCOT, TTC, Pop, MK, HS, AK, WL): ");
            start = sc.nextLine().trim();
            System.out.print("Enter destination (e.g., EPCOT, MK, HS, AK): ");
            dest = sc.nextLine().trim();
            System.out.print("Optional time=HH:MM (or press Enter to skip): ");
            String t = sc.nextLine().trim();
            if (!t.isEmpty()) timeParam = t;
        } else {
            if (words.size() >= 1) start = join(words, 0, words.size() - 1);
            if (words.size() >= 2) dest  = words.get(words.size() - 1);
        }

        String normStart = normalize(applySynonyms(start));
        String normDest  = normalize(applySynonyms(dest));

        System.out.println();
        System.out.println("Disney Transport Concept — Beginner Demo");
        System.out.println("From: " + start);
        System.out.println("To:   " + dest + (timeParam != null ? ("   (time=" + timeParam + ")") : ""));
        System.out.println();

        // fetch route options for this pair
        List<List<Step>> options = findRoutes(normStart, normDest, timeParam);

        if (options.isEmpty()) {
            System.out.println("Sorry, no mock route found yet for that pair.");
            System.out.println("Try one of these examples:");
            for (String s : samplePairs()) System.out.println(" - " + s);
            System.out.println("\nTip: edit findRoutes() to add your own.");
            return;
        }

        int optNum = 1;
        for (List<Step> steps : options) {
            int total = 0;
            System.out.println("Option " + optNum++ + ":");
            int i = 1;
            for (Step s : steps) {
                System.out.printf("%d) %s: %s → %s (≈ %d min)%n",
                        i++, label(s.mode), s.from, s.to, s.minutes);
                total += s.minutes;
            }
            System.out.printf("Total estimated time: ≈ %d min%n%n", total);
        }

        System.out.println("Note: all times are mocked for a simple concept demo.");
    }

    // this is intentionally simple so it’s easy to tweak.
    private static List<List<Step>> findRoutes(String start, String dest, String timeParam) {
        List<List<Step>> out = new ArrayList<>();

        // helpers for common names
        String POLY = normalize("Disney's Polynesian Village Resort");
        String TTC  = normalize("Transportation & Ticket Center (TTC)");
        String EPCOT = normalize("EPCOT");
        String MK   = normalize("Magic Kingdom");
        String POP  = normalize("Disney's Pop Century Resort");
        String HS   = normalize("Disney's Hollywood Studios");
        String CBR  = normalize("Caribbean Beach Skyliner Hub");
        String AK   = normalize("Disney's Animal Kingdom");
        String WL   = normalize("Disney's Wilderness Lodge");
        String IG   = normalize("EPCOT (International Gateway)"); // for skyliner example

        // 1) Polynesian to EPCOT
        if (start.equals(POLY) && dest.equals(EPCOT)) {
            out.add(List.of(
                new Step("WALK", "Disney's Polynesian Village Resort", "Transportation & Ticket Center (TTC)", 5),
                new Step("MONORAIL", "Transportation & Ticket Center (TTC)", "EPCOT", 15)
            ));
            out.add(List.of(
                new Step("MONORAIL", "Disney's Polynesian Village Resort", "Transportation & Ticket Center (TTC)", 20), // wait + ride
                new Step("MONORAIL", "Transportation & Ticket Center (TTC)", "EPCOT", 15)
            ));
        }

        // 2) TTC -> Pop Century (monorail to MK, then bus)
        if (start.equals(TTC) && dest.equals(POP)) {
            out.add(List.of(
                new Step("MONORAIL", "Transportation & Ticket Center (TTC)", "Magic Kingdom", 10), // express-ish
                new Step("BUS", "Magic Kingdom", "Disney's Pop Century Resort", 25)
            ));
        }

        // 3) TTC -> Animal Kingdom (time dependent)
        //    before 10:00 => walk or monorail to Poly, then bus to AK
        //    after 10:00  => monorail to MK, then bus to AK
        if (start.equals(TTC) && dest.equals(AK)) {
            boolean afterTen = isAfterTen(timeParam);

            if (afterTen) {
                out.add(List.of(
                    new Step("MONORAIL", "Transportation & Ticket Center (TTC)", "Magic Kingdom", 10),
                    new Step("BUS", "Magic Kingdom", "Disney's Animal Kingdom", 25)
                ));
            } else {
                out.add(List.of(
                    new Step("WALK", "Transportation & Ticket Center (TTC)", "Disney's Polynesian Village Resort", 5),
                    new Step("BUS", "Disney's Polynesian Village Resort", "Disney's Animal Kingdom", 25)
                ));
                out.add(List.of(
                    new Step("MONORAIL", "Transportation & Ticket Center (TTC)", "Disney's Polynesian Village Resort", 10),
                    new Step("BUS", "Disney's Polynesian Village Resort", "Disney's Animal Kingdom", 25)
                ));
            }
        }

        // 4) Pop Century -> Hollywood Studios (Skyliner via CBR)
        if (start.equals(POP) && dest.equals(HS)) {
            out.add(List.of(
                new Step("SKYLINER", "Disney's Pop Century Resort", "Caribbean Beach Skyliner Hub", 8),
                new Step("SKYLINER", "Caribbean Beach Skyliner Hub", "Disney's Hollywood Studios", 9)
            ));
        }

        // 5) Animal Kingdom -> EPCOT (bus)
        if (start.equals(AK) && dest.equals(EPCOT)) {
            out.add(List.of(
                new Step("BUS", "Disney's Animal Kingdom", "EPCOT", 25)
            ));
        }

        // 6) Wilderness Lodge -> Magic Kingdom (boat or bus)
        if (start.equals(WL) && dest.equals(MK)) {
            out.add(List.of(
                new Step("FERRY", "Disney's Wilderness Lodge", "Magic Kingdom", 12)  // boat
            ));
            out.add(List.of(
                new Step("BUS", "Disney's Wilderness Lodge", "Magic Kingdom", 15)
            ));
        }

        // 7) Hollywood Studios -> EPCOT via Skyliner (ends at IG)
        if (start.equals(HS) && dest.equals(EPCOT)) {
            out.add(List.of(
                new Step("SKYLINER", "Disney's Hollywood Studios", "Caribbean Beach Skyliner Hub", 12),
                new Step("SKYLINER", "Caribbean Beach Skyliner Hub", "EPCOT (International Gateway)", 12)
            ));
        }

        return out;
    }

    // time check: true if time >= 10:00
    private static boolean isAfterTen(String timeHHMM) {
        if (timeHHMM == null || !timeHHMM.matches("\\d{1,2}:\\d{2}")) return false;
        try {
            String[] p = timeHHMM.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            if (h > 10) return true;
            if (h < 10) return false;
            return m >= 0; // 10:00 or later counts as "after" for this toy rule
        } catch (Exception e) {
            return false;
        }
    }

    // quick synonym mapper so users can type short names
    private static String applySynonyms(String raw) {
        String s = raw.trim();
        String low = s.toLowerCase();

        if (low.equals("poly")) return "Disney's Polynesian Village Resort";
        if (low.equals("ttc") || low.contains("ticket center")) return "Transportation & Ticket Center (TTC)";
        if (low.equals("mk") || low.contains("magic kingdom")) return "Magic Kingdom";
        if (low.equals("epcot")) return "EPCOT";
        if (low.startsWith("pop")) return "Disney's Pop Century Resort";
        if (low.equals("hs") || low.contains("hollywood")) return "Disney's Hollywood Studios";
        if (low.equals("ak") || low.contains("animal")) return "Disney's Animal Kingdom";
        if (low.contains("wilderness")) return "Disney's Wilderness Lodge";
        if (low.contains("caribbean beach")) return "Caribbean Beach Skyliner Hub";
        if (low.contains("international gateway")) return "EPCOT (International Gateway)";

        return s;
    }

    private static String normalize(String s) { return s.trim().toLowerCase(); }

    private static String join(List<String> words, int i, int j) {
        // join words[i..j-1] as one string
        if (j <= i) return words.get(0);
        StringBuilder sb = new StringBuilder();
        for (int k = i; k < j; k++) {
            if (k > i) sb.append(" ");
            sb.append(words.get(k));
        }
        return sb.toString();
    }

    private static List<String> samplePairs() {
        return List.of(
            "Disney's Polynesian Village Resort -> EPCOT",
            "Transportation & Ticket Center (TTC) -> Disney's Pop Century Resort",
            "Transportation & Ticket Center (TTC) -> Disney's Animal Kingdom   (try time=11:00)",
            "Disney's Pop Century Resort -> Disney's Hollywood Studios",
            "Disney's Animal Kingdom -> EPCOT",
            "Disney's Wilderness Lodge -> Magic Kingdom"
        );
    }

    private static String label(String mode) {
        switch (mode.toUpperCase()) {
            case "WALK": return "Walk";
            case "MONORAIL": return "Monorail";
            case "SKYLINER": return "Skyliner";
            case "FERRY": return "Boat";
            case "BUS": return "Bus";
            default: return mode;
        }
    }
}
