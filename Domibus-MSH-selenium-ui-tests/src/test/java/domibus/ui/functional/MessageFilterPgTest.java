package domibus.ui.functional;

import io.qameta.allure.*;
import ddsl.dcomponents.grid.DGrid;
import ddsl.dcomponents.popups.Dialog;
import ddsl.enums.DMessages;
import ddsl.enums.PAGES;
import domibus.ui.SeleniumTest;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.msgFilter.MessageFilterGrid;
import pages.msgFilter.MessageFilterModal;
import pages.msgFilter.MessageFilterPage;
import rest.RestServicePaths;
import utils.DFileUtils;
import utils.Gen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


@Epic("Message Filter")
@Feature("Functional")
public class MessageFilterPgTest extends SeleniumTest {

	private MessageFilterPage navigateToPage() throws Exception {
		MessageFilterPage page = new MessageFilterPage(driver);
		if (page.getTitle().contains("Filter")) {
			page.refreshPage();
		} else {
			page.getSidebar().goToPage(PAGES.MESSAGE_FILTER);
		}
		page.grid().waitForRowsToLoad();
		return page;
	}

	/* Login as super admin and open Messages Filter page */
	/*  MSGF-1 - Login as super admin and open Messages Filter page  */
	@Description("MSGF-1 - Login as super admin and open Messages Filter page")
	@Link(name = "EDELIVERY-5078", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5078")
	@AllureId("MSGF-1")
	@Test(description = "MSGF-1", groups = {"multiTenancy", "singleTenancy"})
	public void openMessagesFilterPage() throws Exception {
		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

		Allure.step("checking that all expected elements appear");
		log.info("checking that all expected elements appear");
		soft.assertTrue(page.isLoaded(), "All elements are loaded");
		soft.assertAll();

	}

	/* Create new filter and press Cancel */
	/*  MSGF-3 - Create new filter and press Cancel  */
	@Description("MSGF-3 - Create new filter and press Cancel")
	@Link(name = "EDELIVERY-5079", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5079")
	@AllureId("MSGF-3")
	@Test(description = "MSGF-3", groups = {"multiTenancy", "singleTenancy"})
	public void newFilterCancel() throws Exception {
		String actionName = Gen.randomAlphaNumeric(5);
		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

		page.getNewBtn().click();
		MessageFilterModal popup = new MessageFilterModal(driver);
		popup.getPluginSelect().selectOptionByIndex(0);
		popup.actionInput.sendKeys(actionName);
		popup.clickOK();
		Allure.step("created new filter with action " + actionName);
		log.info("created new filter with action " + actionName);

		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is active after new Message Filter was created");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is active after new Message Filter was created");

		page.cancelChangesAndConfirm();
		Allure.step("canceled the changes");
		log.info("canceled the changes");

		soft.assertTrue(page.grid().scrollTo("Action", actionName) == -1, "New filter is NOT present in the grid");
		soft.assertTrue(page.getSaveBtn().isDisabled(), "Save button is disabled after changes are canceled");
		soft.assertTrue(page.getCancelBtn().isDisabled(), "Cancel button is disabled after changes are canceled");

		soft.assertAll();
	}

	/*User creates new filter and presses Save*/
	/*  MSGF-4 - User creates new filter and presses Save  */
	@Description("MSGF-4 - User creates new filter and presses Save")
	@Link(name = "EDELIVERY-5080", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5080")
	@AllureId("MSGF-4")
	@Test(description = "MSGF-4", groups = {"multiTenancy", "singleTenancy"})
	public void newFilterSave() throws Exception {
		String actionName = Gen.randomAlphaNumeric(5);
		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

		page.getNewBtn().click();
		MessageFilterModal popup = new MessageFilterModal(driver);
		popup.getPluginSelect().selectOptionByIndex(0);
		popup.actionInput.sendKeys(actionName);
		popup.clickOK();
		Allure.step("created new filter with action " + actionName);
		log.info("created new filter with action " + actionName);

		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is active after new Message Filter was created");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is active after new Message Filter was created");

		page.saveAndConfirmChanges();
		Allure.step("saved the changes");
		log.info("saved the changes");

		soft.assertTrue(page.grid().scrollTo("Action", actionName) > -1, "New filter is present in the grid");
		soft.assertTrue(page.getSaveBtn().isDisabled(), "Save button is disabled after changes are saved");
		soft.assertTrue(page.getCancelBtn().isDisabled(), "Cancel button is disabled after changes are saved");

		soft.assertAll();
	}

	/*User shuffles filters using Move Up and Move Down buttons and presses Cancel*/
	/*  MSGF-5 - User shuffles filters using Move Up and Move Down buttons and presses Cancel  */
	@Description("MSGF-5 - User shuffles filters using Move Up and Move Down buttons and presses Cancel")
	@Link(name = "EDELIVERY-5081", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5081")
	@AllureId("MSGF-5")
	@Test(description = "MSGF-5", groups = {"multiTenancy", "singleTenancy"})
	public void shuffleAndCancel() throws Exception {
		SoftAssert soft = new SoftAssert();

		List<String> actionNames = new ArrayList<>();
		Allure.step("create 5 filters for the shuffle");
		log.info("create 5 filters for the shuffle");
		for (int i = 0; i < 5; i++) {
			String actionName = Gen.randomAlphaNumeric(5);
			rest.messFilters().createMessageFilter(actionName, null);
			actionNames.add(actionName);
		}


		MessageFilterPage page = navigateToPage();

		Allure.step("Switch row 0 and row 1");
		log.info("Switch row 0 and row 1");
		page.grid().selectRow(1);
		HashMap<String, String> row1 = page.grid().getRowInfo(1);
		HashMap<String, String> row0 = page.grid().getRowInfo(0);
		page.getMoveUpBtn().click();
		HashMap<String, String> newRow0 = page.grid().getRowInfo(0);
		soft.assertEquals(row1.get("Action"), newRow0.get("Action"), "The row that was previously on position 1 is now on first position");

		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is enabled");

		Allure.step("Cancel changes");
		log.info("Cancel changes");
		page.cancelChangesAndConfirm();
		page.grid().waitForRowsToLoad();

		HashMap<String, String> oldRow0 = page.grid().getRowInfo(0);

		soft.assertEquals(row0.get("Action"), oldRow0.get("Action"),
				"The row that was previously on position 0 is now on first position again after Cancel");

		Allure.step("Delete the created filters");
		log.info("Delete the created filters");
		for (int i = 0; i < actionNames.size(); i++) {
			rest.messFilters().deleteMessageFilter(actionNames.get(i), null);
		}
		soft.assertAll();
	}

	/* User selects first row */
	/*  MSGF-7 - User selects first row  */
	@Description("MSGF-7 - User selects first row")
	@Link(name = "EDELIVERY-5082", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5082")
	@AllureId("MSGF-7")
	@Test(description = "MSGF-7", groups = {"multiTenancy", "singleTenancy"})
	public void selectFirstRow() throws Exception {
		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

		Allure.step("selecting row 0");
		log.info("selecting row 0");
		page.grid().selectRow(0);

		Allure.step("checking buttons after row select");
		log.info("checking buttons after row select");
		soft.assertTrue(!page.getMoveUpBtn().isEnabled(), "Move up button is NOT enabled for the first row");
		soft.assertTrue(page.getMoveDownBtn().isEnabled(), "Move down button is enabled for the first row");

		soft.assertTrue(page.getEditBtn().isEnabled(), "Edit button is enabled for the first row");
		soft.assertTrue(page.getDeleteBtn().isEnabled(), "Delete button is enabled for the first row");

		soft.assertAll();
	}

	/* User selects last row */
	/*  MSGF-8 - User selects last row  */
	@Description("MSGF-8 - User selects last row")
	@Link(name = "EDELIVERY-5083", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5083")
	@AllureId("MSGF-8")
	@Test(description = "MSGF-8", groups = {"multiTenancy", "singleTenancy"})
	public void selectLastRow() throws Exception {
		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

		Allure.step("selecting last row");
		log.info("selecting last row");
		int lastRowIndex = page.grid().getRowsNo() - 1;
		page.grid().selectRow(lastRowIndex);

		Allure.step("checking buttons after row select");
		log.info("checking buttons after row select");
		soft.assertTrue(page.getMoveUpBtn().isEnabled(), "Move up button is NOT enabled for the last row");
		soft.assertTrue(!page.getMoveDownBtn().isEnabled(), "Move down button is enabled for the last row");

		soft.assertTrue(page.getEditBtn().isEnabled(), "Edit button is enabled for the last row");
		soft.assertTrue(page.getDeleteBtn().isEnabled(), "Delete button is enabled for the last row");

		soft.assertAll();
	}

	/* User selects row other than first and last */
	/*  MSGF-9 - User selects row other than first and last  */
	@Description("MSGF-9 - User selects row other than first and last")
	@Link(name = "EDELIVERY-5084", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5084")
	@AllureId("MSGF-9")
	@Test(description = "MSGF-9", groups = {"multiTenancy", "singleTenancy"})
	public void selectMiddleRow() throws Exception {
		SoftAssert soft = new SoftAssert();

		List<String> actionNames = new ArrayList<>();
		Allure.step("create 5 filters");
		log.info("create 5 filters");
		for (int i = 0; i < 5; i++) {
			String actionName = Gen.randomAlphaNumeric(5);
			rest.messFilters().createMessageFilter(actionName, null);
			actionNames.add(actionName);
		}


		MessageFilterPage page = navigateToPage();

		Allure.step("selecting middle row");
		log.info("selecting middle row");
		int rowIndex = page.grid().getRowsNo() / 2;
		page.grid().selectRow(rowIndex);

		Allure.step("checking buttons after row select");
		log.info("checking buttons after row select");
		soft.assertTrue(page.getMoveUpBtn().isEnabled(), "Move up button is enabled for the middle row");
		soft.assertTrue(page.getMoveDownBtn().isEnabled(), "Move down button is enabled for the middle row");

		soft.assertTrue(page.getEditBtn().isEnabled(), "Edit button is enabled for the middle row");
		soft.assertTrue(page.getDeleteBtn().isEnabled(), "Delete button is enabled for middle last row");

		Allure.step("Delete the created filters");
		log.info("Delete the created filters");
		for (int i = 0; i < actionNames.size(); i++) {
			rest.messFilters().deleteMessageFilter(actionNames.get(i), null);
		}

		soft.assertAll();
	}

	/* User click on Move up for any row other than first */
	/*  MSGF-10 - User click on Move up for lastany row other than first  */
	@Description("MSGF-10 - User click on Move up for lastany row other than first")
	@Link(name = "EDELIVERY-5085", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5085")
	@AllureId("MSGF-10")
	@Test(description = "MSGF-10", groups = {"multiTenancy", "singleTenancy"})
	public void moveUpAndDown() throws Exception {
		List<String> actionNames = new ArrayList<>();
		Allure.step("create 5 filters for the shuffle");
		log.info("create 5 filters for the shuffle");
		for (int i = 0; i < 5; i++) {
			String actionName = Gen.randomAlphaNumeric(5);
			rest.messFilters().createMessageFilter(actionName, null);
			actionNames.add(actionName);
		}

		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

//		move up
		int index = page.grid().getRowsNo() / 2;
		HashMap<String, String> prevInfo = page.grid().getRowInfo(index - 1);
		HashMap<String, String> rowInfo = page.grid().getRowInfo(index);
		HashMap<String, String> nextInfo = page.grid().getRowInfo(index + 1);

		Allure.step("moving filter up");
		log.info("moving filter up");
		page.grid().selectRow(index);
		page.getMoveUpBtn().click();

		HashMap<String, String> newPrevInfo = page.grid().getRowInfo(index - 1);
		HashMap<String, String> newRowInfo = page.grid().getRowInfo(index);
		HashMap<String, String> newNextInfo = page.grid().getRowInfo(index + 1);

		Allure.step("checking that rows have swapped position as expected");
		log.info("checking that rows have swapped position as expected");
		soft.assertEquals(newPrevInfo, rowInfo, "Selected row has moved up one position");
		soft.assertEquals(newNextInfo, nextInfo, "Row bellow was not affected");
		soft.assertEquals(newRowInfo, prevInfo, "Row above has moved down one position");

		Allure.step("checking the row is still selected");
		log.info("checking the row is still selected");
		soft.assertTrue(page.grid().getSelectedRowIndex() == index - 1, "Row is still selected at new position");
		Allure.step("checking buttons after row select");
		log.info("checking buttons after row select");
		soft.assertTrue(page.getMoveUpBtn().isEnabled(), "Move up button is enabled for the middle row");
		soft.assertTrue(page.getMoveDownBtn().isEnabled(), "Move down button is enabled for the middle row");

		soft.assertTrue(page.getEditBtn().isEnabled(), "Edit button is enabled for the middle row");
		soft.assertTrue(page.getDeleteBtn().isEnabled(), "Delete button is enabled for middle last row");

//		move down
		prevInfo = page.grid().getRowInfo(index - 1);
		rowInfo = page.grid().getRowInfo(index);
		nextInfo = page.grid().getRowInfo(index + 1);

		Allure.step("moving filter down");
		log.info("moving filter down");
		page.grid().selectRow(index);
		page.getMoveDownBtn().click();

		newPrevInfo = page.grid().getRowInfo(index - 1);
		newRowInfo = page.grid().getRowInfo(index);
		newNextInfo = page.grid().getRowInfo(index + 1);

		Allure.step("checking that rows have swapped position as expected");
		log.info("checking that rows have swapped position as expected");
		soft.assertEquals(newNextInfo, rowInfo, "Selected row has moved down one position");
		soft.assertEquals(prevInfo, newPrevInfo, "Row above was not affected");
		soft.assertEquals(nextInfo, newRowInfo, "Row below has moved up one position");

		Allure.step("checking the row is still selected");
		log.info("checking the row is still selected");
		soft.assertTrue(page.grid().getSelectedRowIndex() == index + 1, "Row is still selected at new position");
		Allure.step("checking buttons after row select");
		log.info("checking buttons after row select");
		soft.assertTrue(page.getMoveUpBtn().isEnabled(), "Move up button is enabled for the middle row");
		soft.assertTrue(page.getMoveDownBtn().isEnabled(), "Move down button is enabled for the middle row");

		soft.assertTrue(page.getEditBtn().isEnabled(), "Edit button is enabled for the middle row");
		soft.assertTrue(page.getDeleteBtn().isEnabled(), "Delete button is enabled for middle last row");

		for (int i = 0; i < actionNames.size(); i++) {
			rest.messFilters().deleteMessageFilter(actionNames.get(i), null);
		}
		Allure.step("deleted the created filters");
		log.info("deleted the created filters");

		soft.assertAll();
	}

	/* User reshuffles filters using Move Up and Move Down buttons and presses Save */
	/*  MSGF-11 - User reshuffles filters using Move Up and Move Down buttons and presses Save  */
	@Description("MSGF-11 - User reshuffles filters using Move Up and Move Down buttons and presses Save")
	@Link(name = "EDELIVERY-5086", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5086")
	@AllureId("MSGF-11")
	@Test(description = "MSGF-11", groups = {"multiTenancy", "singleTenancy"})
	public void shuffleAndSave() throws Exception {
		SoftAssert soft = new SoftAssert();

		List<String> actionNames = new ArrayList<>();
		Allure.step("create 5 filters for the shuffle");
		log.info("create 5 filters for the shuffle");
		for (int i = 0; i < 5; i++) {
			String actionName = Gen.randomAlphaNumeric(5);
			rest.messFilters().createMessageFilter(actionName, null);
			actionNames.add(actionName);
		}

		MessageFilterPage page = navigateToPage();

		int index = page.grid().scrollTo("Action", actionNames.get(0));

		if (index <= 0 || index == page.grid().getRowsNo()) {
			index = page.grid().scrollTo("Action", actionNames.get(1));
		}

		page.grid().selectRow(index);
		Allure.step("selected row with index " + index);
		log.info("selected row with index " + index);

		HashMap<String, String> row = page.grid().getRowInfo(index);

		Allure.step("pressing moveUP button");
		log.info("pressing moveUP button");
		page.getMoveUpBtn().click();

		Allure.step("check filter in position index-1");
		log.info("check filter in position index-1");
		HashMap<String, String> newRow = page.grid().getRowInfo(index - 1);
		soft.assertEquals(row.get("Action"), newRow.get("Action"), "The row was moved up by one position");
		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is enabled");

		Allure.step("Saving");
		log.info("Saving");
		page.saveAndConfirmChanges();

		Allure.step("check filter in position index-1");
		log.info("check filter in position index-1");
		HashMap<String, String> rowAfterSave = page.grid().getRowInfo(index - 1);
		soft.assertEquals(rowAfterSave.get("Action"), row.get("Action"),
				"After the save the row is still on one position higher than before");

		for (int i = 0; i < actionNames.size(); i++) {
			rest.messFilters().deleteMessageFilter(actionNames.get(i), null);
		}
		Allure.step("deleted the created filters");
		log.info("deleted the created filters");

		soft.assertAll();
	}

	/*User selects a filter and chooses to edit it then presses Cancel*/
	/*  MSGF-12 - User selects a filter and chooses to edit it then presses Cancel  */
	@Description("MSGF-12 - User selects a filter and chooses to edit it then presses Cancel")
	@Link(name = "EDELIVERY-5087", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5087")
	@AllureId("MSGF-12")
	@Test(description = "MSGF-12", groups = {"multiTenancy", "singleTenancy"})
	public void editAndCancel() throws Exception {
		Allure.step("Create a filter to edit");
		log.info("Create a filter to edit");
		String actionName = Gen.randomAlphaNumeric(5);
		rest.messFilters().createMessageFilter(actionName, null);

		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();


		MessageFilterGrid grid = page.grid();
		int index = grid.scrollTo("Action", actionName);
		HashMap<String, String> rowInfo = grid.getRowInfo(index);
		page.grid().selectRow(index);

		Allure.step("editing row");
		log.info("editing row");
		page.getEditBtn().click();
		MessageFilterModal modal = new MessageFilterModal(driver);
		Allure.step("editing action value");
		log.info("editing action value");
		modal.getActionInput().fill("newActionValue");
		modal.clickOK();

		Allure.step("canceling changes");
		log.info("canceling changes");
		page.cancelChangesAndConfirm();

		Allure.step("action value after cancel is the same as the one before editing");
		log.info("action value after cancel is the same as the one before editing");
		HashMap<String, String> newRowInfo = grid.getRowInfo(index);
		soft.assertEquals(rowInfo.get("Action"), newRowInfo.get("Action"), "Edited values are reset after canceling changes");

//		Delete created filter
		Allure.step("delete created filter");
		log.info("delete created filter");
		rest.messFilters().deleteMessageFilter(actionName, null);

		soft.assertAll();
	}

	/* User selects a filter and chooses to edit it then press save */
	/*  MSGF-14 - User selects a filter and chooses to edit it then press save  */
	@Description("MSGF-14 - User selects a filter and chooses to edit it then press save")
	@Link(name = "EDELIVERY-5088", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5088")
	@AllureId("MSGF-14")
	@Test(description = "MSGF-14", groups = {"multiTenancy", "singleTenancy"})
	public void editAndSave() throws Exception {
		SoftAssert soft = new SoftAssert();

		Allure.step("create a filter to edit");
		log.info("create a filter to edit");
		String actionName = Gen.randomAlphaNumeric(5);
		String newActionValue = Gen.randomAlphaNumeric(5);
		rest.messFilters().createMessageFilter(actionName, null);

		MessageFilterPage page = navigateToPage();

		Allure.step("editing filter");
		log.info("editing filter");
		int index = page.grid().scrollTo("Action", actionName);
		page.grid().selectRow(index);
		page.getEditBtn().click();

		Allure.step("changing action value");
		log.info("changing action value");
		MessageFilterModal modal = new MessageFilterModal(driver);
		modal.getActionInput().fill(newActionValue);

//		necessary because somehow typing doesn't finish the word otherwise
		modal.wait.forXMillis(200);

		modal.clickOK();
		Allure.step("saving changes");
		log.info("saving changes");
		page.saveAndConfirmChanges();

		HashMap<String, String> row = page.grid().getRowInfo(index);
		soft.assertEquals(row.get("Action"), newActionValue, "Edited values are saved");

//		Delete created filter
		Allure.step("delete created filter");
		log.info("delete created filter");
		rest.messFilters().deleteMessageFilter(newActionValue, null);

		soft.assertAll();
	}

	/* User chooses to delete a filter and presses Cancel */
	/*  MSGF-15 - User chooses to delete a filter and presses Cancel  */
	@Description("MSGF-15 - User chooses to delete a filter and presses Cancel")
	@Link(name = "EDELIVERY-5089", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5089")
	@AllureId("MSGF-15")
	@Test(description = "MSGF-15", groups = {"multiTenancy", "singleTenancy"})
	public void deleteAndCancel() throws Exception {
		SoftAssert soft = new SoftAssert();

		Allure.step("Create a filter to delete");
		log.info("Create a filter to delete");
		String actionName = Gen.randomAlphaNumeric(5);
		rest.messFilters().createMessageFilter(actionName, null);

		MessageFilterPage page = navigateToPage();


		int index = page.grid().scrollTo("Action", actionName);
		if (index < 0) {
			throw new SkipException("Could not find created filter");
		}

		Allure.step("deleting filter");
		log.info("deleting filter");
		page.grid().selectRow(index);
		page.getDeleteBtn().click();

		Allure.step("checking the row was deleted from the grid");
		log.info("checking the row was deleted from the grid");
		index = page.grid().scrollTo("Action", actionName);
		soft.assertTrue(index == -1, "Filter not found in grid after delete");

		Allure.step("canceling changes");
		log.info("canceling changes");
		page.cancelChangesAndConfirm();

		Allure.step("checking the row was restored in the grid");
		log.info("checking the row was restored in the grid");
		index = page.grid().scrollTo("Action", actionName);
		soft.assertTrue(index > -1, "Filter found in grid after Cancel");

//		Delete created filter
		Allure.step("cleanup the filter");
		log.info("cleanup the filter");
		rest.messFilters().deleteMessageFilter(actionName, null);

		soft.assertAll();
	}

	/* User chooses to delete a filter and presses Save */
	/*  MSGF-17 - User chooses to delete a filter and presses Save  */
	@Description("MSGF-17 - User chooses to delete a filter and presses Save")
	@Link(name = "EDELIVERY-5090", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5090")
	@AllureId("MSGF-17")
	@Test(description = "MSGF-17", groups = {"multiTenancy", "singleTenancy"})
	public void deleteAndSave() throws Exception {
		SoftAssert soft = new SoftAssert();

		Allure.step("Create a filter to edit");
		log.info("Create a filter to edit");
		String actionName = Gen.randomAlphaNumeric(5);
		rest.messFilters().createMessageFilter(actionName, null);


		MessageFilterPage page = navigateToPage();

		int index = page.grid().scrollTo("Action", actionName);
		if (index < 0) {
			throw new SkipException("Could not find created filter");
		}

		Allure.step("deleting filter");
		log.info("deleting filter");
		page.grid().selectRow(index);
		page.getDeleteBtn().click();

		Allure.step("checking the row was removed from the grid");
		log.info("checking the row was removed from the grid");
		index = page.grid().scrollTo("Action", actionName);
		soft.assertTrue(index == -1, "Filter not found in grid after delete");

		Allure.step("saving changes");
		log.info("saving changes");
		page.saveAndConfirmChanges();

		Allure.step("checking that row is still removed from the list after save");
		log.info("checking that row is still removed from the list after save");
		index = page.grid().scrollTo("Action", actionName);
		soft.assertTrue(index == -1, "Filter found in grid after Save");

		soft.assertAll();

	}

	/* Create new filter on default domain and change domains */
	/*  MSGF-18 - Create new filter on default domain and change domains  */
	@Description("MSGF-18 - Create new filter on default domain and change domains")
	@Link(name = "EDELIVERY-5091", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5091")
	@AllureId("MSGF-18")
	@Test(description = "MSGF-18", groups = {"multiTenancy"})
	public void newFilterAndChangeDomains() throws Exception {
		SoftAssert soft = new SoftAssert();

		String actionName = Gen.randomAlphaNumeric(5);

		MessageFilterPage page = navigateToPage();

		page.getNewBtn().click();
		MessageFilterModal popup = new MessageFilterModal(driver);
//		popup.getPluginSelect().selectOptionByIndex(0);
		popup.actionInput.sendKeys(actionName);
		popup.serviceInput.sendKeys(actionName + ":" + actionName);
		popup.clickOK();
		Allure.step("created new filter with action" + actionName);
		log.info("created new filter with action" + actionName);

		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is active after new Message Filter was created");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is active after new Message Filter was created");

		page.saveAndConfirmChanges();
		Allure.step("saved the changes");
		log.info("saved the changes");
		soft.assertFalse(page.getAlertArea().isError(), "Success message is shown!!");

		Allure.step("check if filter is present");
		log.info("check if filter is present");
		soft.assertTrue(page.grid().scrollTo("Action", actionName) > -1, "New filter is present in the grid");

		Allure.step("changing domain to 1");
		log.info("changing domain to 1");
		page.getDomainSelector().selectOptionByText(rest.getNonDefaultDomain());
		page.getDomainSelector().selectOptionByText(rest.getNonDefaultDomain());
		page.grid().waitForRowsToLoad();

		Allure.step("check if filter is NOT present");
		log.info("check if filter is NOT present");
		soft.assertTrue(page.grid().scrollTo("Action", actionName) == -1, "New filter is NOT present in the grid on other domains then default");

		Allure.step("delete created filter");
		log.info("delete created filter");
		rest.messFilters().deleteMessageFilter(actionName, null);
		soft.assertAll();
	}

	/* Operate a change in the list of filters and don't press Save or Cancel Change domain */
	/*  MSGF-19 - Operate a change in the list of filters and dont press Save or Cancel and Change domain  */
	@Description("MSGF-19 - Operate a change in the list of filters and dont press Save or Cancel and Change domain")
	@Link(name = "EDELIVERY-5092", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5092")
	@AllureId("MSGF-19")
	@Test(description = "MSGF-19", groups = {"multiTenancy"})
	public void editAndChangeDomain() throws Exception {
		SoftAssert soft = new SoftAssert();

		Allure.step("Create a filter to edit");
		log.info("Create a filter to edit");
		String actionName = Gen.randomAlphaNumeric(5);
		String anotherActionName = Gen.randomAlphaNumeric(5) + "mod";
		rest.messFilters().createMessageFilter(actionName, null);
		Allure.step("filter with action " + actionName + " created");
		log.debug("filter with action " + actionName + " created");
		String domainName = rest.getNonDefaultDomain();

		MessageFilterPage page = navigateToPage();
		String defaultDomainName = page.getDomainFromTitle();

		int index = page.grid().scrollTo("Action", actionName);
		page.grid().selectRow(index);
		page.getEditBtn().click();

		Allure.step("editing the message filter");
		log.info("editing the message filter");
		MessageFilterModal modal = new MessageFilterModal(driver);
		modal.getActionInput().fill(anotherActionName);
		modal.wait.forXMillis(150);
		modal.clickOK();
		modal.wait.forXMillis(150);

		Allure.step("check that new value is present in grid");
		log.info("check that new value is present in grid");
		String listedAction = page.grid().getRowInfo(index).get("Action");
		soft.assertEquals(listedAction, anotherActionName, "Action is changed after edit form is closed");

		try {
			Allure.step("changing domain");
			log.info("changing domain");
			page.getDomainSelector().selectOptionByText(domainName);
		} catch (Exception e) {
			soft.assertEquals(page.getDomainFromTitle().toLowerCase(), defaultDomainName, "Domain has not changed while changes are not saved");
		}

		Allure.step("check that cancel all changes dialog appears");
		log.info("check that cancel all changes dialog appears");
		Dialog dialog = new Dialog(driver);
		soft.assertTrue(dialog.isLoaded(), "Dialog is shown");
		soft.assertEquals(dialog.getMessage(), DMessages.DIALOG_CANCEL_ALL, "Dialog shows correct message");
		Allure.step("confirm cancel all changes");
		log.info("confirm cancel all changes");
		dialog.confirm();

		soft.assertEquals(page.getDomainSelector().getSelectedValue(), domainName, "Domain was changed");

		try {
			Allure.step("change domain back to default");
			log.info("change domain back to default");
			page.getDomainSelector().selectOptionByText(defaultDomainName);
		} catch (Exception e) {
			soft.assertEquals(page.getDomainFromTitle().toLowerCase(), defaultDomainName, "Domain has not changed while changes are not saved");
		}

		Allure.step("check that changes were canceled");
		log.info("check that changes were canceled");
		listedAction = page.grid().getRowInfo(index).get("Action");
		soft.assertEquals(listedAction, actionName, "Action is not changed after the user presses OK in the dialog");
		soft.assertTrue(page.getSaveBtn().isDisabled(), "Changes are canceled and save button is disabled");
		soft.assertTrue(page.getCancelBtn().isDisabled(), "Changes are canceled and cancel button is disabled");


		page.grid().selectRow(index);
		page.getEditBtn().click();
		Allure.step("edit the same filter again");
		log.info("edit the same filter again");
		modal = new MessageFilterModal(driver);
		modal.getActionInput().fill(anotherActionName);
		modal.wait.forXMillis(150);
		modal.clickOK();
		modal.wait.forXMillis(150);

		try {
			Allure.step("changing domain");
			log.info("changing domain");
			page.getDomainSelector().selectOptionByText(domainName);
		} catch (Exception e) {
			soft.assertEquals(page.getDomainFromTitle().toLowerCase(), defaultDomainName, "Domain has not changed while changes are not saved");
		}

		Allure.step("check that cancel all changes dialog appears");
		log.info("check that cancel all changes dialog appears");
		dialog = new Dialog(driver);
		soft.assertTrue(dialog.isLoaded(), "Dialog is shown");
		soft.assertEquals(dialog.getMessage(), DMessages.DIALOG_CANCEL_ALL, "Dialog shows correct message");

		Allure.step("Press cancel in the dialog");
		log.info("Press cancel in the dialog");
		dialog.cancel();
		Allure.step("check that the domain is not changed");
		log.info("check that the domain is not changed");
		soft.assertEquals(page.getDomainSelector().getSelectedValue(), defaultDomainName, "Domain was NOT changed");

		Allure.step("check info for filter is still updated");
		log.info("check info for filter is still updated");
		listedAction = page.grid().getRowInfo(index).get("Action");
		soft.assertEquals(listedAction, anotherActionName, "Action is still changed after the user presses Cancel in the dialog");
		soft.assertTrue(page.getSaveBtn().isEnabled(), "Changes are NOT canceled and save button is enabled");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Changes are NOT canceled and cancel button is enabled");

//		Delete created filter
		Allure.step("cleanup the filter");
		log.info("cleanup the filter");
		rest.messFilters().deleteMessageFilter(actionName, null);

		soft.assertAll();
	}

	/* Download list of messages filters */
	/*  MSGF-20 - Download list of messages filters  */
	@Description("MSGF-20 - Download list of messages filters")
	@Link(name = "EDELIVERY-5093", url = "https://ec.europa.eu/cefdigital/tracker/browse/EDELIVERY-5093")
	@AllureId("MSGF-20")
	@Test(description = "MSGF-20", groups = {"multiTenancy", "singleTenancy"})
	public void csvFileDownload() throws Exception {
		SoftAssert soft = new SoftAssert();

		MessageFilterPage page = navigateToPage();

		String fileName = rest.csv().downloadGrid(RestServicePaths.MESSAGE_FILTERS_CSV, null, null);
		Allure.step("downloaded file " + fileName);
		log.info("downloaded file " + fileName);
		page.grid().checkCSVvsGridInfo(fileName, soft);

		soft.assertAll();
	}


}
