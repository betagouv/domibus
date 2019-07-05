package ddsl.dcomponents;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


public class SandwichMenu extends DComponent {


	private WebDriverWait localWait = wait.defaultWait;

	public SandwichMenu(WebDriver driver) {
		super(driver);
		log.info("sandwich menu init");
	}

	By expandButton = By.id("settingsmenu_id");

	By menuContainer = By.cssSelector("div > div.mat-menu-content.ng-trigger.ng-trigger-fadeInItems");

	By currentUserID = By.cssSelector("button[role=\"menuitem\"]:nth-of-type(1) span");

	By logoutLnk = By.id("logout_id");


	public String getCurrentUserID() throws Exception{
		expandMenu();
		localWait.until(ExpectedConditions.presenceOfElementLocated(currentUserID));
		return driver.findElement(currentUserID).getText().trim();
	}

	private boolean isMenuExpanded() throws Exception {
		try {
			driver.findElement(menuContainer);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	private void expandMenu() throws Exception {
		if (isMenuExpanded()) return;
		driver.findElement(expandButton).click();
		try {
			wait.defaultWait.until(ExpectedConditions.visibilityOfElementLocated(menuContainer));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void contractMenu() throws Exception {
		if (!isMenuExpanded()) return;
		clickVoidSpace();
	}

	public boolean isLoggedIn() throws Exception {
		expandMenu();
		String userIDStr = driver.findElement(currentUserID).getText();
		boolean toReturn = !StringUtils.equalsIgnoreCase(userIDStr, "Not logged in");
		log.info("User login status is: " + toReturn);

		contractMenu();
		return toReturn;
	}

	public void logout() throws Exception {

		clickVoidSpace();

		expandMenu();
		log.info("Logging out...");
		driver.findElement(logoutLnk).click();
		contractMenu();
		wait.defaultWait.until(ExpectedConditions.visibilityOfElementLocated(expandButton));
	}


}
