package com.claircore.billing.domain.model.valueobjects;

public enum PlanType {
    FREEMIUM(1, 1, 1, false, 10),
    PREMIUM(3, 5, 10, true, 100);

    private final int maxOrganizations;
    private final int maxSpaces;
    private final int maxDevices;
    private final boolean monthlyReports;
    private final int assistantDailyMessages;

    PlanType(int maxOrganizations, int maxSpaces, int maxDevices, boolean monthlyReports, int assistantDailyMessages) {
        this.maxOrganizations = maxOrganizations;
        this.maxSpaces = maxSpaces;
        this.maxDevices = maxDevices;
        this.monthlyReports = monthlyReports;
        this.assistantDailyMessages = assistantDailyMessages;
    }

    public boolean monthlyReports() {
        return monthlyReports;
    }

    public int maxOrganizations() {
        return maxOrganizations;
    }

    public int maxSpaces() {
        return maxSpaces;
    }

    public int maxDevices() {
        return maxDevices;
    }

    public int assistantDailyMessages() {
        return assistantDailyMessages;
    }
}
