package com.aidvps.schemakit.provider;

import com.aidvps.druid.differ.internal.model.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for custom schema provider implementations.
 *
 * <p>Validates that a custom provider implementation meets the contract: - Implements
 * SchemaProvider interface correctly - Has required methods - Returns valid schemas - Follows best
 * practices
 *
 * <p>Usage:
 *
 * <pre>
 * CustomProviderValidator validator = new CustomProviderValidator();
 * ValidationResult result = validator.validate(myProvider);
 * if (result.hasErrors()) {
 *     System.out.println("Validation failed: " + result.getErrors());
 * }
 * </pre>
 */
public class CustomProviderValidator {

    /** Validation result containing errors and warnings. */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        /** Add an error. */
        public void addError(String error) {
            errors.add(error);
        }

        /** Add a warning. */
        public void addWarning(String warning) {
            warnings.add(warning);
        }

        /** Check if there are any errors. */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /** Check if there are any warnings. */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /** Get all errors. */
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        /** Get all warnings. */
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        /** Get error count. */
        public int getErrorCount() {
            return errors.size();
        }

        /** Get warning count. */
        public int getWarningCount() {
            return warnings.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (!errors.isEmpty()) {
                sb.append("ERRORS (").append(errors.size()).append("):\n");
                for (String error : errors) {
                    sb.append("  - ").append(error).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                sb.append("WARNINGS (").append(warnings.size()).append("):\n");
                for (String warning : warnings) {
                    sb.append("  - ").append(warning).append("\n");
                }
            }

            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("No issues found - provider is valid!");
            }

            return sb.toString();
        }
    }

    /**
     * Validate a custom provider.
     *
     * @param provider Provider to validate
     * @return Validation result
     */
    public ValidationResult validate(SchemaProvider provider) {
        ValidationResult result = new ValidationResult();

        if (provider == null) {
            result.addError("Provider cannot be null");
            return result;
        }

        // Check interface implementation
        validateInterfaceImplementation(provider, result);

        // Check methods
        validateMethods(provider, result);

        // Check behavior
        validateBehavior(provider, result);

        return result;
    }

    private void validateInterfaceImplementation(SchemaProvider provider, ValidationResult result) {
        Class<?> providerClass = provider.getClass();

        // Check if implements SchemaProvider
        if (!SchemaProvider.class.isAssignableFrom(providerClass)) {
            result.addError("Class must implement SchemaProvider interface");
        }

        // Check if public class
        if (providerClass.isAnnotation()) {
            result.addError("Provider cannot be an annotation");
        }

        if (providerClass.isEnum()) {
            result.addError("Provider cannot be an enum");
        }

        // Check for required methods
        try {
            Method getProviderIdMethod = providerClass.getMethod("getProviderId");
            if (!getProviderIdMethod.getReturnType().equals(String.class)) {
                result.addError("getProviderId() must return String");
            }
        } catch (NoSuchMethodException e) {
            result.addError("Missing required method: getProviderId()");
        }

        try {
            Method getSchemaMethod =
                    providerClass.getMethod("getSchema", SchemaProviderConfig.class);
            if (!getSchemaMethod.getReturnType().equals(Schema.class)) {
                result.addError("getSchema() must return Schema");
            }
        } catch (NoSuchMethodException e) {
            result.addError("Missing required method: getSchema(SchemaProviderConfig)");
        }
    }

    private void validateMethods(SchemaProvider provider, ValidationResult result) {
        try {
            String providerId = provider.getProviderId();
            if (providerId == null || providerId.trim().isEmpty()) {
                result.addError("getProviderId() returned null or empty");
            }
        } catch (Exception e) {
            result.addError("getProviderId() threw exception: " + e.getMessage());
        }

        // Check getSchema with null config
        try {
            provider.getSchema(null);
            result.addError("getSchema(null) should throw SchemaProviderException");
        } catch (SchemaProviderException e) {
            // Expected
        } catch (NullPointerException e) {
            result.addError(
                    "getSchema(null) threw NullPointerException instead of SchemaProviderException");
        } catch (Exception e) {
            result.addError(
                    "getSchema(null) threw unexpected exception: " + e.getClass().getName());
        }
    }

    private void validateBehavior(SchemaProvider provider, ValidationResult result) {
        // Test with invalid config
        try {
            provider.getSchema(new InvalidConfig());
            // Should either throw or handle gracefully
        } catch (SchemaProviderException e) {
            // Expected - good error handling
        } catch (ClassCastException e) {
            result.addError(
                    "getSchema() should handle invalid config types gracefully, not throw ClassCastException");
        } catch (Exception e) {
            result.addWarning(
                    "getSchema() threw exception for invalid config: " + e.getClass().getName());
        }

        // Check if provider has validation method
        Class<?> providerClass = provider.getClass();
        boolean hasValidateConfig = false;
        try {
            providerClass.getMethod("validateConfig", SchemaProviderConfig.class);
            hasValidateConfig = true;
        } catch (NoSuchMethodException e) {
            // No validateConfig method - that's ok, default implementation exists
        }

        if (!hasValidateConfig) {
            result.addWarning("Consider overriding validateConfig() for better error handling");
        }

        // Check for common issues
        checkForCommonIssues(provider, result);
    }

    private void checkForCommonIssues(SchemaProvider provider, ValidationResult result) {
        Class<?> providerClass = provider.getClass();

        // Check for mutable static fields
        if (hasMutableStaticFields(providerClass)) {
            result.addWarning("Provider has mutable static fields - ensure thread safety");
        }

        // Check for deprecated methods
        Method[] methods = providerClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Deprecated.class)) {
                result.addWarning("Method " + method.getName() + " is deprecated");
            }
        }

        // Check method accessibility
        for (Method method : methods) {
            if (method.getDeclaringClass() == SchemaProvider.class) {
                continue; // Skip interface methods
            }
            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
                result.addWarning(
                        "Non-public method "
                                + method.getName()
                                + " should be private or protected");
            }
        }
    }

    private boolean hasMutableStaticFields(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                if (!Modifier.isFinal(field.getModifiers())) {
                    if (!field.getType().isPrimitive()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Quick validation check - returns true if provider is valid, false otherwise.
     *
     * @param provider Provider to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(SchemaProvider provider) {
        ValidationResult result = validate(provider);
        return !result.hasErrors();
    }

    /**
     * Get validation report as a string.
     *
     * @param provider Provider to validate
     * @return Validation report
     */
    public String getValidationReport(SchemaProvider provider) {
        ValidationResult result = validate(provider);
        return result.toString();
    }

    /** Invalid configuration for testing. */
    private static class InvalidConfig implements SchemaProviderConfig {
        @Override
        public java.util.Map<String, Object> toMap() {
            return new java.util.HashMap<>();
        }

        @Override
        public void validate() throws ConfigValidationException {
            throw new ConfigValidationException("Invalid config for testing");
        }
    }
}
