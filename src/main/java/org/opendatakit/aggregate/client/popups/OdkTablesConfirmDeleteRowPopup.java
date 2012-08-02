package org.opendatakit.aggregate.client.popups;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;

public class OdkTablesConfirmDeleteRowPopup extends AbstractPopupBase {

	  private static final String BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Delete Row";
	  private static final String TOOLTIP_TXT = "Delete this row";
	  private static final String HELP_BALLOON_TXT = "Delete this row.";

	  // the table to delete from
	  private final String tableId;
	  // the row you're deleting
	  private final String rowId;

	  public OdkTablesConfirmDeleteRowPopup(String tableId, String rowId) {
	    super();
	    this.tableId = tableId;
	    this.rowId = rowId;

	    AggregateButton deleteButton = new AggregateButton(BUTTON_TXT, TOOLTIP_TXT,
	        HELP_BALLOON_TXT);
	    deleteButton.addClickHandler(new ExecuteDelete());

	    FlexTable layout = new FlexTable();

	    HTML message = new HTML("Are you sure you want to delete this row?");
	    layout.setWidget(0, 0, message);
	    layout.setWidget(0, 1, deleteButton);
	    layout.setWidget(0, 2, new ClosePopupButton(this));

	    setWidget(layout);
	  }

	  private class ExecuteDelete implements ClickHandler {

	    @Override
	    public void onClick(ClickEvent event) {
	      // Set up the callback object.
	      AsyncCallback<Void> callback = new AsyncCallback<Void>() {
	        @Override
	        public void onFailure(Throwable caught) {
	          AggregateUI.getUI().reportError(caught);
	        }

	        @Override
	        public void onSuccess(Void v) {
	          AggregateUI.getUI().clearError();
	          
	          AggregateUI.getUI().getTimer().refreshNow();
	        }
	      };
	      // Make the call to the form service.
	      SecureGWT.getServerDataService().deleteRow(tableId, rowId, callback);
	      hide();
	    }
	  }
}
