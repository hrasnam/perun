package cz.metacentrum.perun.webgui.json.attributesManager;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import cz.metacentrum.perun.webgui.client.PerunWebSession;
import cz.metacentrum.perun.webgui.client.UiElements;
import cz.metacentrum.perun.webgui.client.resources.TableSorter;
import cz.metacentrum.perun.webgui.json.*;
import cz.metacentrum.perun.webgui.json.keyproviders.GeneralKeyProvider;
import cz.metacentrum.perun.webgui.model.Attribute;
import cz.metacentrum.perun.webgui.model.GeneralObject;
import cz.metacentrum.perun.webgui.model.PerunError;
import cz.metacentrum.perun.webgui.widgets.AjaxLoaderImage;
import cz.metacentrum.perun.webgui.widgets.PerunTable;
import cz.metacentrum.perun.webgui.widgets.cells.PerunCheckboxCell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Ajax query to get all required attributes for any entity and specified service
 * 
 * @author Pavel Zlamal <256627@mail.muni.cz>
 */
public class GetRequiredAttributes implements JsonCallback, JsonCallbackTable<Attribute> {

	// Perun session
	private PerunWebSession session = PerunWebSession.getInstance();
	// JSON URL
	private static final String JSON_URL = "attributesManager/getRequiredAttributes";
	// IDs
	private Map<String, Integer> ids = new HashMap<String, Integer>();
	// Selection model
	final MultiSelectionModel<Attribute> selectionModel = new MultiSelectionModel<Attribute>(new GeneralKeyProvider<Attribute>());
	// Table data provider
	private ListDataProvider<Attribute> dataProvider = new ListDataProvider<Attribute>();
	// Table
	private PerunTable<Attribute> table;
	// List of attributes
	private ArrayList<Attribute> list = new ArrayList<Attribute>();
	// FIELD UPDATER - when user clicks on a row
	private FieldUpdater<Attribute, String> tableFieldUpdater;
	// loader image
	private AjaxLoaderImage loaderImage = new AjaxLoaderImage();
	// Json callback events
	private JsonCallbackEvents events = new JsonCallbackEvents();

	/**
	 * Creates a new callback
	 *
     */
	public GetRequiredAttributes() {}

	/**
	 * Creates a new callback
	 *
     * @param ids IDS of entities which we want attributes for
     */
	public GetRequiredAttributes(Map<String, Integer> ids) {
		this.ids = ids;
	}

	/**
	 * Creates a new callback
	 *
     * @param ids IDS of entities which we want attributes for
     * @param events external events
     */
	public GetRequiredAttributes(Map<String, Integer> ids, JsonCallbackEvents events) {
		this.events = events;
		this.ids = ids;
	}

	/**
	 * Retrieves data from the RPC
	 */
	public void retrieveData() {
		String params = "";
		// serialize parameters
		for (Map.Entry<String, Integer> attr : this.ids.entrySet()) {
			params += attr.getKey() + "=" + attr.getValue() + "&";
		}
		JsonClient js = new JsonClient();
		js.retrieveData(GetRequiredAttributes.JSON_URL, params, this);
	}

	/**
	 * Returns the table widget with attributes and custom field updater
	 * 
	 * @param fu custom field updater
	 * @return table widget
	 */
	public CellTable<Attribute> getTable(FieldUpdater<Attribute, String> fu) {
		this.tableFieldUpdater = fu;
		return this.getTable();
	}

	/**
	 * Returns table widget with attributes
	 * 
	 * @return table widget
	 */
	public CellTable<Attribute> getTable() {
		// create table
		CellTable<Attribute> table = getEmptyTable();
		// retrieve data
		retrieveData();
		return table;
	}

	/**
	 * Returns empty table widget with attributes
	 * 
	 * @return table widget
	 */
	public CellTable<Attribute> getEmptyTable(){

		// Table data provider.
		dataProvider = new ListDataProvider<Attribute>(list);

		// Cell table
		table = new PerunTable<Attribute>(list);
		table.removeRowCountChangeHandler(); // remove row count change handler

		// Connect the table to the data provider.
		dataProvider.addDataDisplay(table);

		// Sorting
		ListHandler<Attribute> columnSortHandler = new ListHandler<Attribute>(dataProvider.getList());
		table.addColumnSortHandler(columnSortHandler);

		// table selection
		table.setSelectionModel(selectionModel, DefaultSelectionEventManager.<Attribute> createCheckboxManager());

		// set empty content & loader
		table.setEmptyTableWidget(loaderImage);

		// because of tab index
		table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

		// checkbox column
        Column<Attribute, Attribute> checkBoxColumn = new Column<Attribute, Attribute>(
                new PerunCheckboxCell<Attribute>(true, false, false)) {
            @Override
            public Attribute getValue(Attribute object) {
                // Get the value from the selection model.
                GeneralObject go = object.cast();
                go.setChecked(selectionModel.isSelected(object));
                return go.cast();
            }
        };

        // updates the columns size
        table.setColumnWidth(checkBoxColumn, 40.0, Unit.PX);

        // Add the columns

        // Checkbox column header
        CheckboxCell cb = new CheckboxCell();
        Header<Boolean> checkBoxHeader = new Header<Boolean>(cb) {
            public Boolean getValue() {
                return false;//return true to see a checked checkbox.
            }
        };
        checkBoxHeader.setUpdater(new ValueUpdater<Boolean>() {
            public void update(Boolean value) {
                // sets selected to all, if value = true, unselect otherwise
                for(Attribute obj : list){
                    if (obj.isWritable()) {
                        selectionModel.setSelected(obj, value);
                    }
                }
            }
        });

        table.addColumn(checkBoxColumn, checkBoxHeader);

		// Create ID column.
		table.addIdColumn("Attribute ID");

		// Name column
		Column<Attribute, String> nameColumn = JsonUtils.addColumn(
				new JsonUtils.GetValue<Attribute, String>() {
					public String getValue(Attribute attribute) {
						return attribute.getFriendlyName();
					}
				}, this.tableFieldUpdater);

		// Create ENTITY column
		Column<Attribute, String> entityColumn = JsonUtils.addColumn(
				new JsonUtils.GetValue<Attribute, String>() {
					public String getValue(Attribute g) {
						return g.getEntity();
					}
				}, this.tableFieldUpdater);

		// Create def type column
		Column<Attribute, String> defTypeColumn = JsonUtils.addColumn(
				new JsonUtils.GetValue<Attribute, String>() {
					public String getValue(Attribute g) {
						return g.getDefinition();
					}
				}, this.tableFieldUpdater);

		// Create type column.
		Column<Attribute, String> typeColumn = JsonUtils.addColumn(
				new JsonUtils.GetValue<Attribute, String>() {
					public String getValue(Attribute attribute) {
						return renameContent(attribute.getType());
					}
				}, this.tableFieldUpdater);

		// Create value column.
		Column<Attribute, String> valueColumn = JsonUtils.addColumn(
				new JsonUtils.GetValue<Attribute, String>() {
					public String getValue(Attribute attribute) {
						if (attribute.getValue() == null) return "";
						return attribute.getValue();
					}
				}, new FieldUpdater<Attribute, String>() {
					public void update(int index, Attribute object, String newText) {
						if (object.setValue(newText)) {
							selectionModel.setSelected(object, true);  
						} else {
							selectionModel.setSelected(object, false);
							UiElements.cantSaveAttributeValueDialogBox(object);
						}

					}
				});



		// Sorting name column
		nameColumn.setSortable(true);
		columnSortHandler.setComparator(nameColumn,
				new Comparator<Attribute>() {
			public int compare(Attribute o1, Attribute o2) {
				return o1.getFriendlyName().compareToIgnoreCase(
						o2.getFriendlyName());
			}
		});


		// Sorting type column
		typeColumn.setSortable(true);
		columnSortHandler.setComparator(typeColumn,
				new Comparator<Attribute>() {
			public int compare(Attribute o1, Attribute o2) {
				return o1.getType().compareToIgnoreCase(o2.getType());
			}
		});



		// Sorting value column
		valueColumn.setSortable(true);
		columnSortHandler.setComparator(valueColumn,
				new Comparator<Attribute>() {
			public int compare(Attribute o1, Attribute o2) {
				return o1.getValue().compareToIgnoreCase(o2.getValue());
			}
		});

		// Sorting value column
		entityColumn.setSortable(true);
		columnSortHandler.setComparator(entityColumn,
				new Comparator<Attribute>() {
			public int compare(Attribute o1, Attribute o2) {
				return o1.getEntity().compareToIgnoreCase(o2.getEntity());
			}
		});

		// Sorting value column
		defTypeColumn.setSortable(true);
		columnSortHandler.setComparator(defTypeColumn,
				new Comparator<Attribute>() {
			public int compare(Attribute o1, Attribute o2) {
				return o1.getDefinition().compareToIgnoreCase(o2.getDefinition());
			}
		});

		// Add the columns.
		this.table.addColumnSortHandler(columnSortHandler);

		// updates the columns size
		this.table.setColumnWidth(entityColumn, 100.0, Unit.PX);
		this.table.setColumnWidth(defTypeColumn, 110.0, Unit.PX);
		this.table.setColumnWidth(typeColumn, 100.0, Unit.PX);
		this.table.setColumnWidth(nameColumn, 200.0, Unit.PX);
		this.table.setColumnWidth(valueColumn, 200.0, Unit.PX);

		// Add the columns.
		this.table.addColumn(nameColumn, "Name");
		this.table.addColumn(entityColumn,"Entity");
		this.table.addColumn(defTypeColumn,"Definition");
		this.table.addColumn(typeColumn, "Value type");
		this.table.addColumn(valueColumn, "Value");
		this.table.addDescriptionColumn();

		return this.table;
	}

    /**
     * Sorts table by objects Name
     */
    public void sortTable() {
        list = new TableSorter<Attribute>().sortByFriendlyName(getList());
        dataProvider.flush();
        dataProvider.refresh();
    }

    /**
     * Add object as new row to table
     *
     * @param object Attribute to be added as new row
     */
    public void addToTable(Attribute object) {
        list.add(object);
        dataProvider.flush();
        dataProvider.refresh();
    }

    /**
     * Removes object as row from table
     *
     * @param object Attribute to be removed as row
     */
    public void removeFromTable(Attribute object) {
        list.remove(object);
        selectionModel.getSelectedSet().remove(object);
        dataProvider.flush();
        dataProvider.refresh();
    }

    /**
     * Clear all table content
     */
    public void clearTable(){
        loaderImage.loadingStart();
        list.clear();
        selectionModel.clear();
        dataProvider.flush();
        dataProvider.refresh();
    }

    /**
     * Clears list of selected items
     */
    public void clearTableSelectedSet(){
        selectionModel.clear();
    }

    /**
     * Return selected items from list
     *
     * @return return list of checked items
     */
    public ArrayList<Attribute> getTableSelectedList(){
        return JsonUtils.setToList(selectionModel.getSelectedSet());
    }

    /**
     * Called, when an error occurs
     */
    public void onError(PerunError error) {
        session.getUiElements().setLogErrorText("Error while loading required attributes.");
        loaderImage.loadingError(error);
        events.onError(error);
    }

    /**
     * Called, when loading starts
     */
    public void onLoadingStart() {
        session.getUiElements().setLogText("Loading required attributes started.");
        loaderImage.loadingStart();
        events.onLoadingStart();
    }

    /**
     * Called, when operation finishes successfully.
     */
    public void onFinished(JavaScriptObject jso) {
        clearTable();
        for (Attribute a : JsonUtils.<Attribute>jsoAsList(jso)) {
            if (!a.getDefinition().equals("core")) {
                addToTable(a);
            }
        }
        sortTable();
        loaderImage.loadingFinished();
        session.getUiElements().setLogText("Required attributes loaded: " + list.size());
        events.onFinished(jso);
    }

    public void insertToTable(int index, Attribute object) {
        list.add(index, object);
        dataProvider.flush();
        dataProvider.refresh();
    }

    public void setEditable(boolean editable) {
        //this.editable = editable;
    }

    public void setCheckable(boolean checkable) {
        //this.checkable = checkable;
    }

    public void setList(ArrayList<Attribute> list) {
        clearTable();
        this.list.addAll(list);
        dataProvider.flush();
        dataProvider.refresh();
    }

    public ArrayList<Attribute> getList() {
        return this.list;
    }

	/**
	 * Returns shorten string for ValueTypeColum
	 * 
	 * @return String shorten string
	 */
	private String renameContent(String oldString) {

		String newString = "";
		newString = oldString.substring(10);

		return newString;

	}

	/**
	 * Sets entities and their ids to this callback 
	 * 
	 * @param ids map of IDS
	 */
	public void setIds(Map<String, Integer> ids) {
		this.ids = ids;	
	}

	/**
	 * Returns map of entities and their ids currently "in use" for this callback
	 * 
	 * @return map of IDS
	 */
	public Map<String, Integer> getIds() {
		return this.ids;
	}

	/**
	 * Returns selection model of table
	 * 
	 * @return selection model
	 */
	public MultiSelectionModel<Attribute> getSelectionModel() {
		return selectionModel;
	}

}