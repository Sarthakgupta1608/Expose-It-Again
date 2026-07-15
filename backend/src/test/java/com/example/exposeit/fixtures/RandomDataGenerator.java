package com.example.exposeit.fixtures;

import com.example.exposeit.Post.Entity.PostCategory;
import java.util.Random;
import java.util.UUID;

/**
 * RandomDataGenerator
 *
 * Util class to generate randomized data fields (strings, usernames, emails, coords, UUIDs)
 * for test fixtures, ensuring unique and isolated data sets for each test execution.
 */
public class RandomDataGenerator {
    private static final Random random = new Random();

    public static String randomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String randomEmail() {
        return randomString(10) + "@example.com";
    }

    public static String randomUsername() {
        return "user_" + randomString(8);
    }

    public static String randomPassword() {
        return "Password_" + randomString(8) + "!";
    }

    public static double randomLatitude() {
        return -90.0 + (180.0 * random.nextDouble());
    }

    public static double randomLongitude() {
        return -180.0 + (360.0 * random.nextDouble());
    }

    public static PostCategory randomCategory() {
        PostCategory[] categories = PostCategory.values();
        return categories[random.nextInt(categories.length)];
    }

    public static UUID randomUuid() {
        return UUID.randomUUID();
    }
}
