package hr.algebra.workforce.service;

public final class CroatianPlural {

    private CroatianPlural() {
    }

    public static String workingDays(int count) {
        int lastDigit = count % 10;
        int lastTwoDigits = count % 100;
        if (lastDigit == 1 && lastTwoDigits != 11) {
            return count + " radni dan";
        }
        if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) {
            return count + " radna dana";
        }
        return count + " radnih dana";
    }
}
