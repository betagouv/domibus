package pages.errorLog;

import ddsl.dobjects.DatePicker;
import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.grid.DGrid;
import ddsl.dobjects.DButton;
import ddsl.dobjects.DInput;
import ddsl.dobjects.DLink;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;


/**
 * @author Catalin Comanici

 * @version 4.1
 */


public class ErrorLogPage extends DomibusPage {
	public ErrorLogPage(WebDriver driver) {
		super(driver);
		PageFactory.initElements(new AjaxElementLocatorFactory(driver, data.getTIMEOUT()), this);
	}

	ErrFilters filters = new ErrFilters(driver);

	public ErrFilters getFilters() {
		return filters;
	}

	@FindBy(id = "errorLogTable")
	private WebElement errorLogTableContainer;

	@FindBy(id = "signalmessageid_id")
	WebElement signalMessIDInput;


	@FindBy(id = "messageid_id")
	WebElement messageIDInput;

	@FindBy(id = "searchbutton_id")
	WebElement searchButton;

	@FindBy(id = "advancedlink_id")
	WebElement advancedLink;

	@FindBy(id = "fromtimestamp_id")
	WebElement errFromContainer;

	@FindBy(id = "totimestamp_id")
	WebElement errToContainer;

	public DGrid grid() {
		return new DGrid(driver, errorLogTableContainer);
	}

	public DInput getSignalMessIDInput() {
		return new DInput(driver, signalMessIDInput);
	}

	public DInput getMessageIDInput() {
		return new DInput(driver, messageIDInput);
	}

	public DButton getSearchButton() {
		return new DButton(driver, searchButton);
	}

	public DLink getAdvancedLink() {
		return new DLink(driver, advancedLink);
	}

	public DatePicker getErrFrom() {
		return new DatePicker(driver, errFromContainer);
	}

	public DatePicker getErrTo() {
		return new DatePicker(driver, errToContainer);
	}


	public void basicSearch(String signalMessID, String messageID, String fromDate, String toDate) throws Exception {
		log.debug("submit basic search");

		getSignalMessIDInput().fill(signalMessID);
		getMessageIDInput().fill(messageID);

		getErrFrom().selectDate(fromDate);
		getErrTo().selectDate(toDate);

		getSearchButton().click();

		PageFactory.initElements(driver, this);


	}


	public boolean isLoaded() throws Exception {
		return (getSearchButton().isEnabled()
				&& getMessageIDInput().isEnabled()
				&& getSignalMessIDInput().isEnabled());
	}
}
