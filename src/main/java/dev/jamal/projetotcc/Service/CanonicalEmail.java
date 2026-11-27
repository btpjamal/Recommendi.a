package dev.jamal.projetotcc.Service;

/** Keep in sync with V30: trim U+0020 and fold ASCII uppercase only. */
public final class CanonicalEmail {
    private CanonicalEmail() {}

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        int start = 0;
        int end = email.length();
        while (start < end && email.charAt(start) == ' ') {
            start++;
        }
        while (end > start && email.charAt(end - 1) == ' ') {
            end--;
        }
        StringBuilder canonical = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char ch = email.charAt(i);
            canonical.append(ch >= 'A' && ch <= 'Z' ? (char) (ch + ('a' - 'A')) : ch);
        }
        return canonical.toString();
    }
}
