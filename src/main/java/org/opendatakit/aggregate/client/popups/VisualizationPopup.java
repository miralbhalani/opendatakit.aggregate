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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendatakit.aggregate.client.AggregateUI;
import org.opendatakit.aggregate.client.FilterSubTab;
import org.opendatakit.aggregate.client.SecureGWT;
import org.opendatakit.aggregate.client.form.KmlSettings;
import org.opendatakit.aggregate.client.preferences.Preferences;
import org.opendatakit.aggregate.client.submission.Column;
import org.opendatakit.aggregate.client.submission.SubmissionUI;
import org.opendatakit.aggregate.client.widgets.AggregateButton;
import org.opendatakit.aggregate.client.widgets.ClosePopupButton;
import org.opendatakit.aggregate.client.widgets.ColumnListBox;
import org.opendatakit.aggregate.client.widgets.EnumListBox;
import org.opendatakit.aggregate.client.widgets.KmlSettingListBox;
import org.opendatakit.aggregate.constants.common.ChartType;
import org.opendatakit.aggregate.constants.common.GeoPointConsts;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.maps.client.InfoWindow;
import com.google.gwt.maps.client.InfoWindowContent;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.Table;
import com.google.gwt.visualization.client.visualizations.corechart.BarChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart;
import com.google.gwt.visualization.client.visualizations.corechart.PieChart.PieOptions;

public final class VisualizationPopup extends AbstractPopupBase {

  private static final String COLUMN_TXT = "<h4 id=\"form_name\"> Column to Visualize:</h4>";
  private static final String GPS_TXT = "<h4 id=\"form_name\"> GeoPoint to Map:</h4>";
  private static final String TABULATION_TXT = "<h4 id=\"form_name\">Tabulation:</h4>";
  private static final String TYPE_TXT = "<h4 id=\"form_name\">Visualization Type:</h4>";

  private static int VIZ_TYPE_TEXT = 0;
  private static int VIZ_TYPE_LIST = 1;
  private static int COLUMN_TEXT = 2;
  private static int COLUMN_LIST = 3;
  private static int VALUE_TEXT = 4;
  private static int VALUE_SELECTION = 5;
  private static int BUTTON = 6;
  private static int CLOSE = 7;

  private static final String RESIZE_UNITS = "px";

  private static final String VIZ_TYPE_TOOLTIP = "Type of Visualization";

  private final ArrayList<Column> headers;
  private final ArrayList<SubmissionUI> submissions;

  private final FlexTable dropDownsTable;
  private final EnumListBox<ChartType> chartType;

  private final ColumnListBox columnList;
  private final ColumnListBox dataList;
  private final KmlSettingListBox geoPoints;

  private boolean mapsApiLoaded;
  private boolean chartApiLoaded;
  private boolean tallyOccurances;

  private final String formId;

  private final AggregateButton executeButton;

  private final Label message;

  private final SimplePanel chartPanel;

  public VisualizationPopup(FilterSubTab filterSubTab) {
    super();

    formId = filterSubTab.getDisplayedFilterGroup().getFormId();
    headers = filterSubTab.getSubmissionTable().getHeaders();
    submissions = filterSubTab.getSubmissionTable().getSubmissions();

    chartType = new EnumListBox<ChartType>(ChartType.values(), VIZ_TYPE_TOOLTIP);
    chartType.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        updateUIoptions();
      }
    });

    columnList = new ColumnListBox(headers, "TOOLTIP", false);
    dataList = new ColumnListBox(headers, "TOOLTIP", false);
    geoPoints = new KmlSettingListBox("TOOLTIP");

    mapsApiLoaded = false;
    Maps.loadMapsApi(Preferences.getGoogleMapsApiKey(), "2", false, new Runnable() {
      public void run() {
        mapsApiLoaded = true;
      }
    });

    chartApiLoaded = false;
    VisualizationUtils.loadVisualizationApi(new Runnable() {
      public void run() {
        chartApiLoaded = true;
        updateUIoptions();
      }
    }, PieChart.PACKAGE, Table.PACKAGE);

    SecureGWT.getFormService().getGpsCoordnates(formId, new AsyncCallback<KmlSettings>() {
      public void onFailure(Throwable caught) {
        AggregateUI.getUI().reportError(caught);
      }

      public void onSuccess(KmlSettings result) {
        geoPoints.updateValues(result.getGeopointNodes());
      }
    });

    executeButton = new AggregateButton("", "TOOLTIP");
    executeButton.addClickHandler(new ExecuteVisualization());

    message = new Label();

    dropDownsTable = new FlexTable();
    dropDownsTable.addStyleName("popup_menu");

    dropDownsTable.setHTML(0, VIZ_TYPE_TEXT, TYPE_TXT);
    dropDownsTable.setWidget(0, VIZ_TYPE_LIST, chartType);
    dropDownsTable.setHTML(0, COLUMN_TEXT, COLUMN_TXT);
    dropDownsTable.setWidget(0, COLUMN_LIST, columnList);
    dropDownsTable.setHTML(0, VALUE_TEXT, TABULATION_TXT);
    dropDownsTable.setWidget(0, VALUE_SELECTION, dataList);
    dropDownsTable.setWidget(1, VALUE_SELECTION, dataList);
    dropDownsTable.setWidget(0, BUTTON, executeButton);
    dropDownsTable.setWidget(0, CLOSE, new ClosePopupButton(this));
    dropDownsTable.getCellFormatter().addStyleName(0, CLOSE, "popup_close_cell");

    // setup the window size
    chartPanel = new SimplePanel();
    Integer height = (Window.getClientHeight() * 5) / 6;
    Integer width = (Window.getClientWidth() * 5) / 6;
    chartPanel.setHeight(height.toString() + RESIZE_UNITS);
    chartPanel.setWidth(width.toString() + RESIZE_UNITS);

    VerticalPanel layoutPanel = new VerticalPanel();
    layoutPanel.add(dropDownsTable);
    layoutPanel.add(message);
    layoutPanel.add(chartPanel);

    setWidget(layoutPanel);
    chartType.setItemSelected(0, true);
    updateUIoptions();
  }

  private void updateUIoptions() {
    ChartType selected = chartType.getSelectedValue();
    executeButton.setHTML(selected.getButtonText());
    if (selected.equals(ChartType.MAP)) {
      dropDownsTable.setHTML(0, COLUMN_TEXT, GPS_TXT);
      dropDownsTable.setWidget(0, COLUMN_LIST, geoPoints);
    } else { // must be a chart if not MAP
      dropDownsTable.setHTML(0, COLUMN_TEXT, COLUMN_TXT);
      dropDownsTable.setWidget(0, COLUMN_LIST, columnList);

    }
    center();
    message.setText("UPDATED");

  }

  private DataTable createDataTable() {
    Column firstDataValue = columnList.getSelectedColumn();
    Column secondDataValue = dataList.getSelectedColumn();

    DataTable data = DataTable.create();
    data.addColumn(ColumnType.STRING, firstDataValue.getDisplayHeader());
    if (tallyOccurances) {
      data.addColumn(ColumnType.NUMBER, secondDataValue.getDisplayHeader());
    } else {
      data.addColumn(ColumnType.NUMBER, "Number of Ocurrences");
    }

    int firstIndex = 0;
    int secondIndex = 0;
    int index = 0;
    for (Column c : headers) {
      if (c.equals(firstDataValue))
        firstIndex = index;
      if (c.equals(secondDataValue))
        secondIndex = index;
      index++;
    }

    HashMap<String, Double> aggregation = new HashMap<String, Double>();
    for (SubmissionUI s : submissions) {
      String label = s.getValues().get(firstIndex);
     
      // determine submissions value
      double addend = 0;
      if (tallyOccurances) {
        addend = 1;
      } else {
        try {
          addend = Double.parseDouble(s.getValues().get(secondIndex));
        } catch(Exception e) {
          // move on because we couldn't parse the value
        }
      }
      
      // update running total
      if (aggregation.containsKey(label)) {
        aggregation.put(label, aggregation.get(label) + addend);
      } else {
        aggregation.put(label, addend);
      }
    }

    // output table
    int i = 0;
    for (String s : aggregation.keySet()) {
      data.addRow();
      data.setValue(i, 0, s);
      data.setValue(i, 1, aggregation.get(s));
      i++;
    }

    return data;
  }

  /**
   * Create pie chart
   * 
   * @return
   */
  private PieChart createPieChart() {
    DataTable data = createDataTable();
    PieOptions options = PieChart.createPieOptions();
    options.setWidth(chartPanel.getOffsetWidth());
    options.setHeight(chartPanel.getOffsetHeight());
    options.set3D(true);
    return new PieChart(data, options);
  }

  /**
   * Create bar chart
   * 
   * @return
   */
  private BarChart createBarChart() {
    DataTable data = createDataTable();
    Options options = Options.create();
    options.setWidth(chartPanel.getOffsetWidth());
    options.setHeight(chartPanel.getOffsetHeight());
    return new BarChart(data, options);
  }

  private int findGpsIndex(String columnElementKey, Integer columnCode) {
    int index = 0;
    Long columnNum = columnCode.longValue();
    for (Column col : headers) {
      if (col.getColumnEncoding().equals(columnElementKey)
          && col.getGeopointColumnCode().equals(columnNum)) {
        return index;
      }
      index++;
    }
    return -1;
  }

  private LatLng getLatLonFromSubmission(int latIndex, int lonIndex, SubmissionUI sub) {
    LatLng gpsPoint;
    List<String> values = sub.getValues();
    try {
      Double lat = Double.parseDouble(values.get(latIndex));
      Double lon = Double.parseDouble(values.get(lonIndex));
      gpsPoint = LatLng.newInstance(lat, lon);
    } catch (Exception e) {
      // just set the gps point to null, no need to figure out problem
      gpsPoint = null;
    }
    return gpsPoint;
  }

  private MapWidget createMap() {
    int latIndex = findGpsIndex(geoPoints.getElementKey(),
        GeoPointConsts.GEOPOINT_LATITUDE_ORDINAL_NUMBER);
    int lonIndex = findGpsIndex(geoPoints.getElementKey(),
        GeoPointConsts.GEOPOINT_LONGITUDE_ORDINAL_NUMBER);

    // check to see if we have lat & long, if not display erro
    if (latIndex < 0 || lonIndex < 0) {
      String error = "ERROR:";
      if(latIndex < 0) {
        error = error + " The Latitude Coordinate is NOT included in the Filter.";
      }
      if(lonIndex < 0) {
        error = error + " The Longitude Coordinate is NOT included in the Filter.";
      }

      message.setText(error);
      return null;
    }

    // create a center point, stop at the first gps point found
    LatLng center = LatLng.newInstance(0.0, 0.0);
    for (SubmissionUI sub : submissions) {
      LatLng gpsPoint = getLatLonFromSubmission(latIndex, lonIndex, sub);
      if (gpsPoint != null) {
        center = gpsPoint;
        break;
      }
    }

    // create mapping area
    final MapWidget map = new MapWidget(center, 3);
    map.setSize("100%", "100%");
    map.addControl(new LargeMapControl());

    // create the markers
    for (SubmissionUI sub : submissions) {
      LatLng gpsPoint = getLatLonFromSubmission(latIndex, lonIndex, sub);
      if (gpsPoint != null) {
        Marker marker = new Marker(gpsPoint);
        map.addOverlay(marker);

        // marker needs to be added to the map before calling
        // InfoWindow.open(marker, ...)
        marker.addMarkerClickHandler(new MarkerClickHandler() {
          public void onClick(MarkerClickEvent event) {
            InfoWindow info = map.getInfoWindow();
            info.open(event.getSender(), new InfoWindowContent("HELLO WORLD!"));
          }
        });
      }
    }
    return map;
  }

  private class ExecuteVisualization implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {

      // verify modules are loaded
      if (!mapsApiLoaded || !chartApiLoaded) {
        message.setText("Modules are not loaded yet, please try again!");
        return;
      }

      Widget chart;
      switch (chartType.getSelectedValue()) {
      case MAP:
        chart = createMap();
        break;
      case PIE_CHART:
        chart = createPieChart();
        break;
      case BAR_GRAPH:
        chart = createBarChart();
        break;
      default:
        chart = null;
      }
      chartPanel.clear();
      chartPanel.add(chart);
    }

  }
}