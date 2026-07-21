package com.valliento.session;

import java.util.Set;

/**
 * Defines which sidebar modules each role is allowed to see/use.
 *
 * Administrator - full access to everything.
 * Manager       - add/update/delete products, manage inventory, purchase,
 *                 suppliers, employees, expenses, reports, settings.
 * Cashier       - billing / invoice generation, customer lookup, daily closing.
 * Waiter        - mark tables and manage orders, generate KOT.
 *
 * Keys correspond to the module keys used in MainController.
 */
public class RolePermissions {

    public static final String ADMINISTRATOR = "Administrator";
    public static final String MANAGER = "Manager";
    public static final String CASHIER = "Cashier";
    public static final String WAITER = "Waiter";

    private static final Set<String> MANAGER_MODULES = Set.of(
        "dashboard", "products", "inventory", "purchase",
        "customers", "suppliers", "expenses", "employees",
        "reports", "dailyClosing", "settings"
    );

    private static final Set<String> CASHIER_MODULES = Set.of(
        "dashboard", "billing", "customers", "dailyClosing"
    );

    private static final Set<String> WAITER_MODULES = Set.of(
        "tableManagement", "kot"
    );

    /** The module a role should land on right after logging in. */
    public static String defaultModule(String role) {
        if (role == null) return "dashboard";
        return switch (role) {
            case MANAGER -> "dashboard";
            case CASHIER -> "billing";
            case WAITER -> "tableManagement";
            default -> "dashboard"; // Administrator and anything unrecognized
        };
    }

    /** Whether the given role is allowed to open the given module. */
    public static boolean canAccess(String role, String moduleKey) {
        if (role == null) return false;
        if (ADMINISTRATOR.equalsIgnoreCase(role)) return true;

        return switch (role) {
            case MANAGER -> MANAGER_MODULES.contains(moduleKey);
            case CASHIER -> CASHIER_MODULES.contains(moduleKey);
            case WAITER -> WAITER_MODULES.contains(moduleKey);
            default -> false;
        };
    }
}