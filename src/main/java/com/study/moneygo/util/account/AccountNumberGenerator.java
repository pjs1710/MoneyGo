package com.study.moneygo.util.account;

import java.util.Random;

public class AccountNumberGenerator {

    private static final String PREFIX = "1001";
    private static final Random random = new Random();

    public static String generate() {
        // 1001-XXXX-XXXX 형식으로
        String part1 = PREFIX;
        String part2 = generateRandomDigits(4);
        String part3 = generateRandomDigits(4);

        return String.format("%s-%s-%s", part1, part2, part3);
    }

    private static String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }
}
