package co.proteccion.value_objects;

public record Compliance(
        boolean isOnRestrictedList,
        boolean sourceOfFundsDeclared,
        boolean taxDeclarationAccepted
) {
}
