/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.client.popups;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.form.KmlSettings;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.aggregate.client.widgets.EnumListBox;
import org.opendatakit.aggregate.client.widgets.KmlSettingListBox;
import org.opendatakit.aggregate.constants.common.ExportType;
import org.opendatakit.aggregate.constants.common.SubTabs;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;

public final class ExportPopup extends AbstractPopupBase {

  private static final String FILE_TYPE_TOOLTIP = "Type of File to Generate";
  private static final String GEOPOINT_TOOLTIP = "Geopoint field to map";
  private static final String BINARY_TOOLTIP = "Binary field to display";
  private static final String TITLE_TOOLTIP = "Field to use as Title";

  private static final String CREATE_BUTTON_TXT = "<img src=\"images/green_right_arrow.png\" /> Publish";
  private static final String CREATE_BUTTON_TOOLTIP = "Create Export File";
  private static final String CREATE_BUTTON_HELP_BALLON = "This creates either a CSV or KML file of your data.";

  private static final String EXPORT_ERROR_MSG = "Either the Geopoint field or your Title field were invalid";

  private boolean gotKmlOptions = false;
  private FlexTable layout;
  private EnumListBox<ExportType> fileType;

  private String formId;

  private KmlSettingListBox geoPointsDropDown;
  private KmlSettingListBox titleFieldsDropDown;
  private KmlSettingListBox binaryFieldsDropDown;

  private AggregateButton exportButton;

  public ExportPopup(String formid) {
    super();
    this.formId = formid;

    layout = new FlexTable();

    geoPointsDropDown = new KmlSettingListBox(GEOPOINT_TOOLTIP);
    titleFieldsDropDown = new KmlSettingListBox(TITLE_TOOLTIP);
    binaryFieldsDropDown = new KmlSettingListBox(BINARY_TOOLTIP);

    exportButton = new AggregateButton(CREATE_BUTTON_TXT, CREATE_BUTTON_TOOLTIP,
        CREATE_BUTTON_HELP_BALLON);
    exportButton.addClickHandler(new CreateExportHandler()); 
    
    fileType = new EnumListBox<ExportType>(ExportType.values(), FILE_TYPE_TOOLTIP);
    fileType.addChangeHandler(new ExportTypeChangeHandler());

    SecureGWT.getFormService().getPossibleKmlSettings(formId, new KmlSettingsCallback());

    layout.setWidget(0, 0, new ClosePopupButton(this));
    layout.setWidget(0, 1, new HTML("<h3>Form:<h3>"));
    layout.setWidget(0, 2, new HTML(formId));

    layout.setWidget(0, 3, new HTML("<h3>Type:<h3>"));
    layout.setWidget(0, 4, fileType);
    layout.setWidget(0, 6, exportButton);

    layout.setWidget(1, 1, new HTML("<h4>Geopoint:<h4>"));
    layout.setWidget(1, 2, geoPointsDropDown);

    layout.setWidget(1, 3, new HTML("<h4>Title:<h4>"));
    layout.setWidget(1, 4, titleFieldsDropDown);

    layout.setWidget(1, 5, new HTML("<h4>Picture:<h4>"));
    layout.setWidget(1, 6, binaryFieldsDropDown);

    updateUIOptions();

    setWidget(layout);
  }

  private void enableKmlOptions() {
    geoPointsDropDown.setEnabled(true);
    titleFieldsDropDown.setEnabled(true);
    binaryFieldsDropDown.setEnabled(true);
    layout.getRowFormatter().setStyleName(1, "enabledTableRow");
  }

  private void disableKmlOptions() {
    geoPointsDropDown.setEnabled(false);
    titleFieldsDropDown.setEnabled(false);
    binaryFieldsDropDown.setEnabled(false);
    layout.getRowFormatter().setStyleName(1, "disabledTableRow");
  }

  public void updateUIOptions() {
    ExportType type = fileType.getSelectedValue();

    if (type == null) {
      exportButton.setEnabled(false);
      disableKmlOptions();
      return;
    }

    switch (type) {
    case KML:
      if (gotKmlOptions) {
        exportButton.setEnabled(true);
      } else {
        exportButton.setEnabled(false);
      }
      enableKmlOptions();
      break;
    case CSV:
      exportButton.setEnabled(true);
      disableKmlOptions();
      break;
    default: // unknown type
      exportButton.setEnabled(false);
      disableKmlOptions();
      break;
    }
  }


  private class CreateExportCallback implements AsyncCallback<Boolean> {

    @Override
    public void onFailure(Throwable caught) {
      AggregateUI.getUI().reportError(caught);
    }

    @Override
    public void onSuccess(Boolean result) {
      if (result) {
        AggregateUI.getUI().redirectToSubTab(SubTabs.EXPORT);
      } else {
        Window.alert(EXPORT_ERROR_MSG);
      }

      hide();
    }
  }

  private class KmlSettingsCallback implements AsyncCallback<KmlSettings> {
    @Override
    public void onFailure(Throwable caught) {
      AggregateUI.getUI().reportError(caught);
    }

    @Override
    public void onSuccess(KmlSettings result) {
      gotKmlOptions = true;
      geoPointsDropDown.updateValues(result.getGeopointNodes());
      titleFieldsDropDown.updateValues(result.getTitleNodes());
      binaryFieldsDropDown.updateValues(result.getBinaryNodes());
    }
  }

  private class ExportTypeChangeHandler implements ChangeHandler {
    @Override
    public void onChange(ChangeEvent event) {
      updateUIOptions();
    }
  }
  
  private class CreateExportHandler implements ClickHandler {
    @Override
    public void onClick(ClickEvent event) {
      ExportType type = ExportType.valueOf(fileType.getValue(fileType.getSelectedIndex()));

      if (type.equals(ExportType.CSV)) {
        SecureGWT.getFormService().createCsv(formId, null, new CreateExportCallback());
      } else { // .equals(ExportType.KML.toString())
        String geoPointValue = geoPointsDropDown.getElementKey();
        String titleValue = titleFieldsDropDown.getElementKey();
        String binaryValue = binaryFieldsDropDown.getElementKey();

        SecureGWT.getFormService().createKml(formId, geoPointValue, titleValue, binaryValue,
            new CreateExportCallback());
      }

    }
  }
}