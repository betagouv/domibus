package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.UserManagementException;
import eu.domibus.api.user.UserState;
import eu.domibus.api.user.plugin.AuthenticationEntity;
import eu.domibus.api.user.plugin.PluginUserService;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.user.plugin.PluginUserMapper;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.ErrorRO;
import eu.domibus.web.rest.ro.PluginUserFilterRequestRO;
import eu.domibus.web.rest.ro.PluginUserRO;
import eu.domibus.web.rest.ro.PluginUserResultRO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
@RestController
@RequestMapping(value = "/rest/plugin")
@Validated
public class PluginUserResource extends BaseResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserResource.class);

    @Autowired
    private PluginUserService pluginUserService;

    @Autowired
    private PluginUserMapper pluginUserMapper;

    @Autowired
    private AuthCoreMapper authCoreMapper;

    @Autowired
    private ErrorHandlerService errorHandlerService;

    @ExceptionHandler({UserManagementException.class})
    public ResponseEntity<ErrorRO> handleUserManagementException(UserManagementException ex) {
        return errorHandlerService.createResponse(ex, HttpStatus.CONFLICT);
    }

    @GetMapping(value = {"/users"})
    public PluginUserResultRO findUsers(PluginUserFilterRequestRO request) {
        PluginUserResultRO result = retrieveAndPackageUsers(request);
        Long count = pluginUserService.countUsers(request.getAuthType(), request.getAuthRole(), request.getOriginalUser(), request.getUserName());
        result.setCount(count);
        return result;
    }

    @PutMapping(value = {"/users"})
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateUsers(@RequestBody @Valid List<PluginUserRO> userROs) {
        LOG.debug("Update plugin users was called: {}", userROs);

        List<PluginUserRO> addedUsersRO = userROs.stream().filter(u -> UserState.NEW.name().equals(u.getStatus())).collect(Collectors.toList());
        List<PluginUserRO> updatedUsersRO = userROs.stream().filter(u -> UserState.UPDATED.name().equals(u.getStatus())).collect(Collectors.toList());
        List<PluginUserRO> removedUsersRO = userROs.stream().filter(u -> UserState.REMOVED.name().equals(u.getStatus())).collect(Collectors.toList());

        List<AuthenticationEntity> addedUsers = authCoreMapper.pluginUserROListToAuthenticationEntityList(addedUsersRO);
        List<AuthenticationEntity> updatedUsers = authCoreMapper.pluginUserROListToAuthenticationEntityList(updatedUsersRO);
        List<AuthenticationEntity> removedUsers = authCoreMapper.pluginUserROListToAuthenticationEntityList(removedUsersRO);

        pluginUserService.updateUsers(addedUsers, updatedUsers, removedUsers);
    }

    /**
     * This method returns a CSV file with the contents of Plugin User table
     *
     * @return CSV file with the contents of Plugin User table
     */
    @GetMapping(path = "/csv")
    public ResponseEntity<String> getCsv(PluginUserFilterRequestRO request) {
        request.setPageStart(0);
        request.setPageSize(getCsvService().getPageSizeForExport());
        final PluginUserResultRO result = retrieveAndPackageUsers(request);
        AuthType authType = request.getAuthType();
        getCsvService().validateMaxRows(result.getEntries().size(),
                () -> pluginUserService.countUsers(authType, request.getAuthRole(), request.getOriginalUser(), request.getUserName()));

        return exportToCSV(result.getEntries(), PluginUserRO.class, getCustomColumnNames(authType), getExcludedColumns(authType), "pluginusers");
    }

    protected List<String> getExcludedColumns(AuthType type) {
        List<String> excludedColumns = new ArrayList<>();
        excludedColumns.addAll(Arrays.asList("authenticationType", "entityId", "status", "password", "domain"));
        if (type == AuthType.BASIC) {
            excludedColumns.add("certificateId");
        } else {
            excludedColumns.addAll(Arrays.asList("userName", "expirationDate", "active", "suspended"));
        }
        return excludedColumns;
    }

    protected Map<String, String> getCustomColumnNames(AuthType type) {
        Map<String, String> customColumnNames = new HashMap<>();
        customColumnNames.put("authRoles".toUpperCase(), "Role");
        if (type == AuthType.BASIC) {
            customColumnNames.put("UserName".toUpperCase(), "User Name");
        }
        return customColumnNames;
    }

    protected PluginUserResultRO retrieveAndPackageUsers(PluginUserFilterRequestRO request) {
        LOG.debug("Retrieving plugin users.");
        List<AuthenticationEntity> users = pluginUserService.findUsers(request.getAuthType(), request.getAuthRole(), request.getOriginalUser(), request.getUserName(),
                request.getPageStart(), request.getPageSize());

        List<PluginUserRO> pluginUserROList = pluginUserMapper.convertAndPrepareUsers(users);

        PluginUserResultRO result = new PluginUserResultRO();
        result.setEntries(pluginUserROList);
        result.setPage(request.getPageStart());
        result.setPageSize(request.getPageSize());

        return result;
    }

}
