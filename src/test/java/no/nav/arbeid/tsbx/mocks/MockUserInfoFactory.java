package no.nav.arbeid.tsbx.mocks;

import no.nav.arbeid.tsbx.auth.UserInfo;

import java.time.Month;
import java.time.Year;

public class MockUserInfoFactory {

    public static UserInfo generateRandomUser() {
        final String firstName = new String[]{"Ola", "Fredrik", "Jane", "Lisa"}[(int)(Math.random()*4)];
        final String lastName = new String[]{"Fredriksen", "Olsen", "Berntsen", "Hansen"}[(int)(Math.random()*4)];
        final String pid = generateValidPid();

        return new UserInfo(firstName + " " + lastName, pid);
    }

    public static String generateValidPid() {
        final Year year = Year.of(1980 + (int)(Math.random()*20));
        final Month month = Month.of((int)(Math.random()*12) + 1);
        final int day = (int)(Math.random()*month.length(year.isLeap())) + 1;

        final int d1 = day / 10,
                d2 = day % 10,
                m1 = month.getValue() / 10,
                m2 = month.getValue() % 10,
                y1 = (year.getValue() / 10) % 10,
                y2 = year.getValue() % 10;

        int k1, k2, i1, i2, i3;
        do {
            final int idigits = (int) (Math.random() * 500);
            i1 = idigits / 100;
            i2 = (idigits / 10) % 10;
            i3 = idigits % 10;

            k1 = 11 - ((3 * d1 + 7 * d2 + 6 * m1 + 1 * m2 + 8 * y1 + 9 * y2 + 4 * i1 + 5 * i2 + 2 * i3) % 11);
            if (k1 == 11) {
                k1 = 0;
            }

            k2 = 11 - ((5 * d1 + 4 * d2 + 3 * m1 + 2 * m2 + 7 * y1 + 6 * y2 + 5 * i1 + 4 * i2 + 3 * i3 + 2 * k1) % 11);
            if (k2 == 11) {
                k2 = 0;
            }
        } while (k1 == 10 || k2 == 10);

        return String.format("%d%d%d%d%d%d%d%d%d%d%d", d1, d2, m1, m2, y1, y2, i1, i2, i3, k1, k2);
    }

}
