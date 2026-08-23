package com.xw.recruitment.application;

import java.util.Set;

public final class ApplicationStage {
    private static final Set<String> ALLOWED =
        Set.of("new", "screening", "interview", "offer", "hired", "rejected");

    private ApplicationStage() {}

    public static boolean isAllowed(String value) {
        return value != null && ALLOWED.contains(value);
    }
}
