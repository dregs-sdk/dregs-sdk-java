package com.dregs.sdk.model;

/**
 * The four categories Dregs scores an identity in.
 *
 * <p>Each is scored from 0 (worst) to 100 (best) and moves independently: an account can look
 * entirely human and still score badly on authenticity.
 */
public enum Category {

    /** How likely it is that a person, rather than a script, is behind the account. */
    HUMANITY,

    /** How genuine the details on the account look. */
    AUTHENTICITY,

    /** How distinct the account is from others in the same tenant. */
    UNIQUENESS,

    /** How ordinary the account's activity looks. */
    BEHAVIOR;

    /**
     * Parses a category name from the API, tolerating one this release predates.
     *
     * <p>A category Dregs adds later comes back as {@code null} rather than breaking the response
     * around it. The name is still reachable through the enclosing model's raw payload.
     *
     * @param value the category name, such as {@code "HUMANITY"}
     * @return the matching category, or {@code null} when there is none
     */
    public static Category fromApi(String value) {
        if (value == null) {
            return null;
        }

        for (Category category : values()) {
            if (category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }

        return null;
    }
}
