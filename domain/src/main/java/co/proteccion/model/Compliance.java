package co.proteccion.model;

public record Compliance(
        Boolean isOnRestrictedList,
        Boolean sourceOfFundsDeclared,
        Boolean taxDeclarationAccepted
) {
}
