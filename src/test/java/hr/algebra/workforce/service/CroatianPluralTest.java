package hr.algebra.workforce.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CroatianPluralTest {

    @Test
    void singularIsUsedForOneAndNumbersEndingInOne() {
        assertThat(CroatianPlural.workingDays(1)).isEqualTo("1 radni dan");
        assertThat(CroatianPlural.workingDays(21)).isEqualTo("21 radni dan");
        assertThat(CroatianPlural.workingDays(101)).isEqualTo("101 radni dan");
    }

    @Test
    void dualIsUsedForNumbersEndingInTwoToFour() {
        assertThat(CroatianPlural.workingDays(2)).isEqualTo("2 radna dana");
        assertThat(CroatianPlural.workingDays(3)).isEqualTo("3 radna dana");
        assertThat(CroatianPlural.workingDays(24)).isEqualTo("24 radna dana");
    }

    @Test
    void pluralIsUsedForFiveAndAboveAndForTeens() {
        assertThat(CroatianPlural.workingDays(5)).isEqualTo("5 radnih dana");
        assertThat(CroatianPlural.workingDays(11)).isEqualTo("11 radnih dana");
        assertThat(CroatianPlural.workingDays(12)).isEqualTo("12 radnih dana");
        assertThat(CroatianPlural.workingDays(14)).isEqualTo("14 radnih dana");
        assertThat(CroatianPlural.workingDays(0)).isEqualTo("0 radnih dana");
    }
}
