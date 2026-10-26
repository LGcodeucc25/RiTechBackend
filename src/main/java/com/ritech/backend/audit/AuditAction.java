package com.ritech.backend.audit;

public final class AuditAction {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String SIMULATION_CREATED = "SIMULATION_CREATED";
    public static final String REPORT_VIEWED = "REPORT_VIEWED";
    public static final String PASSWORD_RECOVERY_REQUESTED = "PASSWORD_RECOVERY_REQUESTED";

    private AuditAction() {
    }
}
