package domibus.ui.ux;

import ddsl.dcomponents.grid.DGrid;
import ddsl.enums.DMessages;
import ddsl.enums.PAGES;
import domibus.ui.SeleniumTest;
import org.apache.commons.collections4.ListUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.tlsTrustStore.TlsTrustStorePage;
import rest.RestServicePaths;
import utils.TestUtils;

import java.util.List;

/**
 * @author Rupam
 * @version 5.0
 */

public class TlsTruststoreUXTest extends SeleniumTest {

    JSONObject descriptorObj = TestUtils.getPageDescriptorObject(PAGES.TRUSTSTORES_TLS);


    /* This method will verify page navigation and components when tls config is not done */
    @Test(description = "TLS-1", groups = {"multiTenancy", "singleTenancy", "NoTlsConfig"})
    public void openTlsTrustorePg() throws Exception {
        SoftAssert soft = new SoftAssert();

        log.info("Login into application and navigate to TlsTruststore page");

        selectRandomDomain();

        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);

        soft.assertTrue(page.getAlertArea().isError(), "Error message appears");
        String currentDomain = page.getDomainFromTitle();
        if (!data.isMultiDomain()) {
            currentDomain = "default";
        }
        soft.assertTrue(page.getAlertArea().getAlertMessage().equals(String.format(DMessages.TlsTruststore.TLS_TRUSTSTORE_NOCONFIG, currentDomain)), "same");
        soft.assertTrue(page.getUploadButton().isEnabled(), "Upload button is enabled");
        soft.assertTrue(page.getDownloadButton().isDisabled(), "Download button is disabled");
        soft.assertTrue(page.getAddCertButton().isDisabled(), "Add Certificate button is disabled");
        soft.assertTrue(page.getRemoveCertButton().isDisabled(), "Remove Certificate button is disabled");
        soft.assertTrue(page.grid().getPagination().getTotalItems() == 0, "Grid is empty");
        soft.assertTrue(page.getGridctrls().getShowHideCtrlLnk().isPresent(), "Show column link is present");

        soft.assertAll();
    }

    /* This method will verfiy page navigation with components when tls configuration is done */
    @Test(description = "TLS-2", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void openPage() throws Exception {
        SoftAssert soft = new SoftAssert();

        log.info("Login into application and navigate to TlsTruststore page");
        selectRandomDomain();

        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);
        soft.assertTrue(!page.getAlertArea().isShown(), "Alert area with error/success message is not shown on page landing");
        soft.assertTrue(page.getUploadButton().isEnabled(), "Upload button is enabled");
        soft.assertTrue(page.getDownloadButton().isEnabled(), "Download button is enabled");
        soft.assertTrue(page.getAddCertButton().isEnabled(), "Add Certificate button is enabled");
        soft.assertTrue(page.getRemoveCertButton().isDisabled(), "Remove Certificate button is disabled");
        soft.assertTrue(page.getGridctrls().getShowHideCtrlLnk().isPresent(), "Show column link is present");

        soft.assertAll();
    }

    /* This method will verify download csv feature */
    @Test(description = "TLS-11", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void downloadCSV() throws Exception {
        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);

        String fileName = rest.csv().downloadGrid(RestServicePaths.TLS_TRUSTSTORE_CSV, null, null);
        log.info("downloaded rows to file " + fileName);
        page.grid().checkCSVvsGridInfo(fileName, soft);

        soft.assertAll();
    }

    /* This method will verify grid data by changing rows */
    @Test(description = "TLS-12", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void changeRowCount() throws Exception {

        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);

        DGrid grid = page.grid();
        grid.checkChangeNumberOfRows(soft);
        soft.assertAll();
    }

    /* Check/Uncheck of fields on Show links */
    @Test(description = "TLS-15", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void changeVisibleColumns() throws Exception {

        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);

        DGrid grid = page.grid();
        grid.waitForRowsToLoad();
        grid.checkModifyVisibleColumns(soft);

        soft.assertAll();
    }

    /* This method will verify hide show link functionality */
    @Test(description = "TLS-17", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void hideWithoutSelection() throws Exception {
        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);

        DGrid grid = page.grid();
        List<String> columnsPre = grid.getColumnNames();

        soft.assertTrue(!grid.getGridCtrl().areCheckboxesVisible(), "Before clicking Show link,checkboxes are not visible");

        grid.getGridCtrl().showCtrls();
        soft.assertTrue(grid.getGridCtrl().areCheckboxesVisible(), "After clicking Show link,checkboxes are visible");

        grid.getGridCtrl().hideCtrls();
        soft.assertTrue(!grid.getGridCtrl().areCheckboxesVisible(), "After clicking Hide link,checkboxes are not visible");

        List<String> columnsPost = grid.getColumnNames();
        soft.assertTrue(ListUtils.isEqualList(columnsPre, columnsPost), "List of columns before and after event are same");

        soft.assertAll();
    }

    /* This test case will verify sorting on the basis of all sortable columns */
    @Test(description = "TLS-13", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void verifySorting() throws Exception {
        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);

        page.grid().getPagination().getPageSizeSelect().selectOptionByText("100");

        JSONArray colDescs = descriptorObj.getJSONObject("grid").getJSONArray("columns");
        for (int i = 0; i < colDescs.length(); i++) {
            JSONObject colDesc = colDescs.getJSONObject(i);
            if (page.grid().getColumnNames().contains(colDesc.getString("name"))) {
                TestUtils.testSortingForColumn(soft, page.grid(), colDesc);
            }
        }

        soft.assertAll();
    }

    /* This method will verify show column link presence along with all column checkboxes*/
    @Test(description = "TLS-14", groups = {"multiTenancy", "singleTenancy", "TlsConfig"})
    public void verifyShowLinkFeature() throws Exception {
        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);
        page.getGridctrls().showCtrls();
        soft.assertTrue(page.getGridctrls().areCheckboxesVisible(), "All check boxes are visible");
        soft.assertTrue(page.getGridctrls().getAllLnk().isPresent() && page.getGridctrls().getAllLnk().isEnabled(), "All link is present & enabled");
        soft.assertTrue(page.getGridctrls().getNoneLnk().isPresent() && page.getGridctrls().getNoneLnk().isEnabled(), "None link is present & enabled");

        soft.assertAll();

    }

    /* This method will verify Page navigation and default element present on both domains when tls config is done*/
    @Test(description = "TLS-18", groups = {"multiTenancy", "TlsConfig"})
    public void openPageForSuperAdmin() throws Exception {
        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);
        int domainCount = rest.getDomainNames().size();
        for (int i = 0; i <= domainCount - 1; i++) {
            page.getDomainSelector().selectOptionByIndex(i);
            page.grid().waitForRowsToLoad();
            Boolean isDefaultElmPresent = !page.getAlertArea().isShown() && page.getUploadButton().isEnabled() &&
                    page.getDownloadButton().isEnabled() && page.getAddCertButton().isEnabled() &&
                    page.getRemoveCertButton().isDisabled() && page.getGridctrls().getShowHideCtrlLnk().isPresent();
            soft.assertTrue(isDefaultElmPresent("yes",page,soft));
           // soft.assertTrue(isDefaultElmPresent, "All default elements are present properly for domain " + page.getDomainFromTitle());
        }
        soft.assertAll();
    }

    /* This test case will verify Page navigation and element on both domains when no tls config is done*/
    @Test(description = "TLS-19", groups = {"multiTenancy", "NoTlsConfig"})
    public void openPageSuperAdmin() throws Exception {
        SoftAssert soft = new SoftAssert();
        TlsTrustStorePage page = new TlsTrustStorePage(driver);
        page.getSidebar().goToPage(PAGES.TRUSTSTORES_TLS);
        int domainCount = rest.getDomainNames().size();
        for (int i = 0; i <= domainCount - 1; i++) {
            soft.assertTrue(page.getAlertArea().isShown(), "Error message is shown");
            page.getDomainSelector().selectOptionByIndex(i);
            page.grid().waitForRowsToLoad();
            isDefaultElmPresent("no",page,soft);
        }
        soft.assertAll();
    }

    public Boolean isDefaultElmPresent(String tlsConfig, TlsTrustStorePage page, SoftAssert soft) throws Exception {

        if (tlsConfig.equals("yes")) {
            Boolean isElmPresent = !page.getAlertArea().isShown() && page.getUploadButton().isEnabled() &&
                    page.getDownloadButton().isEnabled() && page.getAddCertButton().isEnabled() &&
                    page.getRemoveCertButton().isDisabled() && page.getGridctrls().getShowHideCtrlLnk().isPresent();
            return isElmPresent;
        } else {
            Boolean isElmPresent =  page.getUploadButton().isEnabled() && page.getAddCertButton().isDisabled()
             && page.getRemoveCertButton().isDisabled()  && page.getGridctrls().getShowHideCtrlLnk().isPresent();
           return isElmPresent;
        }

    }
}

