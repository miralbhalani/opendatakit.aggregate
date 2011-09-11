package org.opendatakit.aggregate.client.widgets;

import com.google.gwt.user.client.ui.Image;

public final class HelpDialogsToggleButton extends AggregateImageToggleButton {

  private static final Image HELP_DIALOG_ICON = new Image("images/help_dialog.jpg");
  private static final String TOOLTIP_TXT = "Help Balloons";
  private static final String HELP_BALLOON_TXT = "This will display a more detailed help balloon when"
      + "you hover over an icon.";

  public HelpDialogsToggleButton() {
    super(HELP_DIALOG_ICON, TOOLTIP_TXT, HELP_BALLOON_TXT);
  }

}