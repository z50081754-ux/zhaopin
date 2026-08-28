package com.xw.recruitment.visit;

public enum VisitSystem {
    RECRUITMENT("recruitment", 15),
    WALLETCHECK("walletcheck", 15),
    RESEARCH("research", 5);

    private final String code;
    private final int qualificationSeconds;

    VisitSystem(String code, int qualificationSeconds) {
        this.code = code;
        this.qualificationSeconds = qualificationSeconds;
    }

    public String code() { return code; }
    public int qualificationSeconds() { return qualificationSeconds; }

    public static VisitSystem fromCode(String value) {
        for (VisitSystem system : values()) if (system.code.equals(value)) return system;
        throw new IllegalArgumentException("Invalid visit system.");
    }
}
