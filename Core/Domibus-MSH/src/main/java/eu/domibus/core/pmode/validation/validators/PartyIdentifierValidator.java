package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.core.pmode.validation.PModeValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Soumya Chandran
 * @since 4.2
 * Validates Identifiers in the party.
 */
@Component
@Order(9)
public class PartyIdentifierValidator implements PModeValidator {

    final PModeValidationHelper pModeValidationHelper;
    final BusinessProcessValidator businessProcessValidator;

    public PartyIdentifierValidator(PModeValidationHelper pModeValidationHelper, BusinessProcessValidator businessProcessValidator) {
        this.pModeValidationHelper = pModeValidationHelper;
        this.businessProcessValidator = businessProcessValidator;
    }

    /**
     * Validate the Identifiers in the pmode
     *
     * @param pMode configuration of pmode
     * @return list of ValidationIssue
     */
    @Override
    public List<ValidationIssue> validate(Configuration pMode) {
        List<ValidationIssue> issues = new ArrayList<>();
        List<Party> allParties = pMode.getBusinessProcesses().getParties();
        allParties.forEach(party -> {
            issues.addAll(validateDuplicatePartyIdentifiers(party));
            validateForbiddenCharactersInParty(issues, party);
        });

        issues.addAll(validateDuplicateIdentifiersInAllParties(allParties));

        return issues;
    }

    protected void validateForbiddenCharactersInParty(List<ValidationIssue> issues, Party party) {
        businessProcessValidator.validateForbiddenCharacters(issues, party.getName(), "party name [" + party.getName() + "].");
        party.getIdentifiers().forEach(identifier -> {
            businessProcessValidator.validateForbiddenCharacters(issues, identifier.getPartyId(), "party identifier's partyId [" + identifier.getPartyId() + "].");
            if (identifier.getPartyIdType() != null) {
                businessProcessValidator.validateForbiddenCharacters(issues, identifier.getPartyIdType().getName(), "party identifier's partyId type name [" + identifier.getPartyIdType().getName() + "].");
                businessProcessValidator.validateForbiddenCharacters(issues, identifier.getPartyIdType().getValue(), "party identifier's partyId type value [" + identifier.getPartyIdType().getValue() + "].");
            }
        });
    }

    /**
     * check the duplicate identifiers of the parties
     *
     * @param party party with identifiers
     * @return list of ValidationIssue
     */
    protected List<ValidationIssue> validateDuplicatePartyIdentifiers(Party party) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<Identifier> duplicateIdentifiers = party.getIdentifiers().stream()
                .collect(Collectors.groupingBy(Identifier::getPartyId))
                .entrySet().stream()
                .filter(map -> map.getValue().size() > 1)
                .flatMap(map -> map.getValue().stream())
                .collect(Collectors.toSet());

        duplicateIdentifiers.forEach(identifier -> {
            issues.add(createIssue(identifier.getPartyId(), party.getName(), "Duplicate party identifier [%s] found for the party [%s]"));
        });

        return issues;
    }


    /**
     * check the duplicate identifiers in all the parties
     *
     * @param allParties list of all parties
     * @return list of ValidationIssue
     */
    protected List<ValidationIssue> validateDuplicateIdentifiersInAllParties(List<Party> allParties) {
        List<ValidationIssue> issues = new ArrayList<>();

        List<String> allIds = allParties.stream()
                .flatMap(party -> party.getIdentifiers().stream())
                .map(id -> id.getPartyId())
                .collect(Collectors.toList());
        Set<String> uniques = new HashSet<>();
        Set<String> duplicateIds = allIds.stream()
                .filter(e -> !uniques.add(e))
                .collect(Collectors.toSet());
        duplicateIds.forEach(id -> {
            issues.add(createIssue(id, id, "Duplicate party identifier [%s] found."));
        });

        return issues;
    }

    /**
     * Get the duplicate identifiers found in the party
     *
     * @return list of duplicate identifiers
     */
    protected List<Identifier> getDuplicateIdentifiers(Set<Identifier> identifierSet, Party party1) {
        List<Identifier> duplicateIdentifiers = new ArrayList<>();
        identifierSet.forEach(identifier -> party1.getIdentifiers().stream()
                .filter(party -> identifier.getPartyId().equalsIgnoreCase(party.getPartyId()))
                .forEach(duplicateIdentifiers::add));
        return duplicateIdentifiers;
    }

    /**
     * Creates pmode validation issue
     */
    protected ValidationIssue createIssue(String partyId, String name, String message) {
        return pModeValidationHelper.createValidationIssue(message, partyId, name);
    }

    protected List<ValidationIssue> validateDuplicatePartyNameInAllParties(Party party, List<Party> allParties) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (allParties.stream().anyMatch(otherParty -> otherParty != party && StringUtils.equalsIgnoreCase(party.getName(), otherParty.getName()))) {
            issues.add(pModeValidationHelper.createValidationIssue("Duplicate party name [%s] found in parties", party.getName()));
        }
        return issues;
    }
}
