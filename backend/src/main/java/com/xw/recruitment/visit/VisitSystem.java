package com.xw.recruitment.visit;

public enum VisitSystem {
    RECRUITMENT("recruitment"), WALLETCHECK("walletcheck");

    private final String code;

    VisitSystem(String code) { this.code = code; }

    public String code() { return code; }

    public static VisitSystem fromCode(String value) {
        for (VisitSystem system : values()) if (system.code.equals(value)) return system;
        throw new IllegalArgumentException("Invalid visit system.");
    }
}
