/*
 @(#)Copyright:  Copyright (c) 2012-2020 All right reserved.
 @(#)Author:     Marko Lokka
 @(#)Product:    Image and Video Files Organizer Tool
 @(#)Purpose:    To help to organize images and video files in your harddrive with less pain
 */
package com.girbola.controllers.main;

import static com.girbola.Main.bundle;
import static com.girbola.fileinfo.FileInfo_Utils.createFileInfo_list;
import static com.girbola.messages.Messages.sprintf;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.girbola.Main;
import com.girbola.concurrency.ConcurrencyUtils;
import com.girbola.configuration.Configuration_SQL_Utils;
import com.girbola.controllers.loading.LoadingProcess_Task;
import com.girbola.controllers.main.tables.FolderInfo;
import com.girbola.controllers.main.tables.TableUtils;
import com.girbola.controllers.main.tables.cell.TableCell_Connected;
import com.girbola.controllers.main.tables.cell.TableCell_Copied;
import com.girbola.controllers.main.tables.cell.TableCell_DateDifference_Status;
import com.girbola.controllers.main.tables.cell.TableCell_DateFixer;
import com.girbola.controllers.main.tables.cell.TableCell_ProgressBar;
import com.girbola.controllers.main.tables.cell.TableCell_Status;
import com.girbola.fileinfo.FileInfo;
import com.girbola.messages.Messages;
import com.girbola.misc.Misc;
import com.girbola.sql.SqliteConnection;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.StageStyle;
import javafx.util.Callback;

/**
 *
 * @author Marko Lokka
 */
public class Tables {

	private final String ERROR = Tables.class.getSimpleName();

	private Model_main model_Main;

	private HideButtons hideButtons;
	private Sorter sorter;

	private TableView<FolderInfo> sortIt_table;
	private TableView<FolderInfo> sorted_table;
	private TableView<FolderInfo> asitis_table;

	private HBox tables_container;

	boolean isSameTable = false;

	protected Tables(Model_main aModel) {
		this.model_Main = aModel;
		hideButtons = new HideButtons(this.model_Main);
		sprintf("Tables instantiated...");
	}

	private FolderInfo findTableValues(TableView<FolderInfo> table, File f) {
		for (FolderInfo tv : table.getItems()) {
			if (tv.getFolderPath().equals(f.toString())) {
				return tv;
			}
		}
		sprintf("findTableValues is NULL");
		return null;
	}

	void setDrag(TableView<FolderInfo> table) {

		table.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getClickCount() == 2) {
					if (event.getTarget() instanceof TableColumn) {
						sprintf("setOnMouseClicked is tablerow");
					} else {
						sprintf("is not tablerow. it is:  " + event.getTarget().toString());
					}
				}
			}
		});
		table.setOnDragDetected(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (event.getTarget() instanceof Rectangle) {
					return;
				}
				if (event.getTarget().equals(event.getSource())) {
					return;
				}
				sprintf("setOnDragDetected:  " + event.getTarget() + " source: " + event.getSource());

				ObservableList<FolderInfo> selected = table.getSelectionModel().getSelectedItems();
				List<File> selectedFiles = getSelectedFilesFromTable(selected);
				if (selectedFiles.isEmpty()) {
					return;
				}
				Dragboard db = table.startDragAndDrop(TransferMode.MOVE);
				final ClipboardContent content = new ClipboardContent();
				content.putFiles(selectedFiles);
				db.setContent(content);

				event.consume();
			}

		});

		table.setOnDragDropped((DragEvent event) -> {
			isSameTable = false;
			sprintf("setOnDragDropped set on drag dropped: " + event.getGestureTarget() + " source gesture: "
					+ event.getGestureSource());

			if (event.getGestureSource() != event.getGestureTarget()) {
				@SuppressWarnings("unchecked")
				TableView<FolderInfo> source = (TableView<FolderInfo>) event.getGestureSource();
				@SuppressWarnings("unchecked")
				TableView<FolderInfo> target = (TableView<FolderInfo>) event.getGestureTarget();

				Messages.sprintf("target= " + target.getId() + " source= " + source.getId());
				// TableView<FolderInfo> tableTarget = (TableView<FolderInfo>)
				// event.getGestureTarget();
//				if (target == null) {
//					sprintf("tableTarget is null");
//					return;
//				}
				Dragboard db = event.getDragboard();
				List<File> folder = db.getFiles();
				if (folder.isEmpty()) {
					// sprintf("db getFiles were empty!");
					return;
				}
				for (File f : folder) {
					Messages.sprintf("TEH file?: " + f);
					FolderInfo tableValue = findTableValues(source, f);
					if (tableValue != null) {
						target.getItems().add(tableValue);
					} else {
						break;
					}
				}

			} else {
				isSameTable = true;
			}
		});

		table.setOnDragOver((DragEvent event) -> {
			/* show to the user that it is an actual gesture target */
			/*
			 * if (event.getGestureSource() != target && event.getDragboard().hasString()) {
			 * // allow for both copying and moving, whatever user chooses
			 * event.acceptTransferModes(TransferMode.COPY_OR_MOVE); }
			 */
			if (event.getGestureSource() != table && event.getDragboard().hasFiles()) {
				// sprintf("Event drag over has files: " +
				// event.getGestureSource() + " source:
				// " + event.getSource());
				event.acceptTransferModes(TransferMode.MOVE);
				isSameTable = false;
			} else {
				isSameTable = true;
			}
			event.consume();
		});
		table.setOnDragExited((DragEvent event) -> {
			sprintf("drag exited");
			event.consume();
		});

		table.setOnDragEntered((DragEvent event) -> {
			sprintf(" drag entered " + event.getGestureTarget());
			if (event.getGestureTarget() == null) {
				isSameTable = true;
			} else {
				isSameTable = false;
			}
		});
		table.setOnDragDone((DragEvent event) -> {
			sprintf("draggingg is done" + event.getGestureSource() + " target: " + event.getGestureTarget());

			if (!isSameTable) {
				@SuppressWarnings("unchecked")
				TableView<FolderInfo> tableView = (TableView<FolderInfo>) event.getGestureSource();
				ObservableList<FolderInfo> selectedToBeRemoved = tableView.getSelectionModel().getSelectedItems();
				tableView.getItems().removeAll(selectedToBeRemoved);
			}
			isSameTable = false;
			sortIt_table.getSelectionModel().clearSelection();
			sorted_table.getSelectionModel().clearSelection();
			asitis_table.getSelectionModel().clearSelection();

		});
	}

	public void setDeleteKeyPressed(TableView<FolderInfo> table) {
		table.setOnKeyPressed((KeyEvent event) -> {
			if (event.getCode() == (KeyCode.DELETE)) {
				ObservableList<FolderInfo> table_row_list = table.getSelectionModel().getSelectedItems();
				Dialog<ButtonType> dialog = new Dialog<>();
				dialog.initStyle(StageStyle.UNDECORATED);

				DialogPane dialogPane = new DialogPane();
				dialogPane.setPrefHeight(Math.floor(200 / 1.5));
				dialogPane.setContentText(bundle.getString("removePermanently"));
				dialog.setDialogPane(dialogPane);

				ButtonType yes = new ButtonType(bundle.getString("yes"), ButtonBar.ButtonData.YES);
				ButtonType no = new ButtonType(bundle.getString("no"), ButtonBar.ButtonData.NO);
				ButtonType cancel = new ButtonType(bundle.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

				dialog.getDialogPane().getButtonTypes().addAll(yes, no, cancel);

				Optional<ButtonType> result = dialog.showAndWait();
				if (result.get().getButtonData().equals(ButtonBar.ButtonData.YES)) {

					ArrayList<FolderInfo> listToRemove = new ArrayList<>();
					Connection connection = SqliteConnection.connector(Main.conf.getAppDataPath(),
							Main.conf.getFolderInfo_db_fileName());

					for (FolderInfo folderInfo : table_row_list) {
						if (!folderInfo.getFolderPath()
								.equals(System.getProperty("user.home") + File.separator + "Pictures")) {
							listToRemove.add(folderInfo);
						} else {
							Messages.warningText(bundle.getString("folderIsUserHomeDirectory") + " Folder name is: "
									+ folderInfo.getFolderPath());
							listToRemove.add(folderInfo);
						}
					}
					for (FolderInfo folderInfo : listToRemove) {
						table.getItems().remove(folderInfo);
					}
					Configuration_SQL_Utils.insert_IgnoredList(listToRemove);
					listToRemove.clear();
					try {
						connection.close();
					} catch (Exception e) {
						// TODO: handle exception
					}
				} else if (result.get().getButtonData().equals(ButtonBar.ButtonData.NO)) {
					ArrayList<FolderInfo> listToRemove = new ArrayList<>();

					for (FolderInfo folderInfo : table_row_list) {
						// if
						// (!folderInfo.getFolderPath().contains(System.getProperty("user.home")))
						// {
						//
						// }
						listToRemove.add(folderInfo);
					}
					table.getItems().removeAll(listToRemove);

					listToRemove.clear();
				} else if (result.get().getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) {
					Messages.sprintf("Cancelled removing files");
				} else {
					Messages.errorSmth(ERROR, "", null, Misc.getLineNumber(), false);
				}

				// table.getSelectionModel().clearSelection();
			}
		});
	}

	public boolean checkIfExist(TableView<FolderInfo> tableTarget, String f) {
		for (FolderInfo folder : tableTarget.getItems()) {
			if (folder.getFolderPath().equals(f)) {
				// System.out.println("folder listing: " + folder);
				return true;
			}
		}
		return false;
	}

	private List<File> getSelectedFilesFromTable(ObservableList<FolderInfo> list) {
		List<File> newList = new ArrayList<>();

		for (FolderInfo listing : list) {
			newList.add(new File(listing.getFolderPath()));
		}
		return newList;
	}

	public Sorter getSorter() {
		return this.sorter;
	}

	public TableView<FolderInfo> getSortIt_table() {
		return this.sortIt_table;
	}

	public TableView<FolderInfo> getSorted_table() {
		return this.sorted_table;
	}

	public TableView<FolderInfo> getAsItIs_table() {
		return this.asitis_table;
	}

	public void setSortIt_table(TableView<FolderInfo> sortIt_table) {
		sprintf("setSortIt_table setted");
		if (sortIt_table == null) {
			Messages.sprintf("table was null!");
		}
		this.sortIt_table = sortIt_table;
		Messages.sprintf("table was: " + sortIt_table);

	}

	public void setSorted_table(TableView<FolderInfo> sorted_table) {
		if (sorted_table == null) {
			Messages.sprintf("table was null!");
		}
		this.sorted_table = sorted_table;
	}

	public void setAsItIs_table(TableView<FolderInfo> asitis_table) {
		if (asitis_table == null) {
			Messages.sprintf("table was null!");
		}
		this.asitis_table = asitis_table;
	}

	void setTables_Container(HBox tables_container) {
		this.tables_container = tables_container;
		for (Node n : tables_container.getChildren()) {
			if (n instanceof VBox) {

				for (Node vbox : ((VBox) n).getChildren()) {
					if (vbox instanceof TableView) {
						sprintf("TableView found!: " + vbox);
						HBox.setHgrow(vbox, Priority.ALWAYS);
						HBox.setHgrow(vbox, Priority.ALWAYS);
						HBox.setHgrow(vbox, Priority.ALWAYS);

						VBox.setVgrow(vbox, Priority.ALWAYS);
						VBox.setVgrow(vbox, Priority.ALWAYS);
						VBox.setVgrow(vbox, Priority.ALWAYS);

						HBox.setHgrow(n, Priority.ALWAYS);
						HBox.setHgrow(n, Priority.ALWAYS);
						HBox.setHgrow(n, Priority.ALWAYS);

						VBox.setVgrow(n, Priority.ALWAYS);
						VBox.setVgrow(n, Priority.ALWAYS);
						VBox.setVgrow(n, Priority.ALWAYS);

					}
				}

			}
		}
	}

	HBox getTables_Container() {
		return this.tables_container;
	}

	public Callback<TableColumn<FolderInfo, Boolean>, TableCell<FolderInfo, Boolean>> connected_cellFactory = new Callback<TableColumn<FolderInfo, Boolean>, TableCell<FolderInfo, Boolean>>() {
		@Override
		public TableCell<FolderInfo, Boolean> call(TableColumn<FolderInfo, Boolean> p) {
			return new TableCell_Connected(model_Main);
		}
	};

	public Callback<TableColumn<FolderInfo, Integer>, TableCell<FolderInfo, Integer>> progressBar_cellFactory = new Callback<TableColumn<FolderInfo, Integer>, TableCell<FolderInfo, Integer>>() {
		@Override
		public TableCell<FolderInfo, Integer> call(TableColumn<FolderInfo, Integer> p) {
			return new TableCell_ProgressBar();
		}
	};

	public Callback<TableColumn<FolderInfo, String>, TableCell<FolderInfo, String>> dateFixer_cellFactory = new Callback<TableColumn<FolderInfo, String>, TableCell<FolderInfo, String>>() {
		@Override
		public TableCell<FolderInfo, String> call(TableColumn<FolderInfo, String> p) {
			return new TableCell_DateFixer(model_Main);
		}
	};
	public Callback<TableColumn<FolderInfo, Double>, TableCell<FolderInfo, Double>> dateDifference_Status_cellFactory = new Callback<TableColumn<FolderInfo, Double>, TableCell<FolderInfo, Double>>() {
		@Override
		public TableCell<FolderInfo, Double> call(TableColumn<FolderInfo, Double> p) {
			return new TableCell_DateDifference_Status(model_Main);
		}
	};
	public Callback<TableColumn<FolderInfo, Integer>, TableCell<FolderInfo, Integer>> cell_Status_cellFactory = new Callback<TableColumn<FolderInfo, Integer>, TableCell<FolderInfo, Integer>>() {
		@Override
		public TableCell<FolderInfo, Integer> call(TableColumn<FolderInfo, Integer> p) {
			return new TableCell_Status(model_Main);
		}
	};

	public Callback<TableColumn<FolderInfo, Integer>, TableCell<FolderInfo, Integer>> copied_cellFactory = new Callback<TableColumn<FolderInfo, Integer>, TableCell<FolderInfo, Integer>>() {
		@Override
		public TableCell<FolderInfo, Integer> call(TableColumn<FolderInfo, Integer> p) {
			return new TableCell_Copied();
		}
	};

	public void refreshAllTables() {
		Platform.runLater(() -> {
			if (!getAsItIs_table().getItems().isEmpty()
					|| (TableColumn<?, ?>) getAsItIs_table().getColumns().get(0) != null) {
				((TableColumn<?, ?>) getAsItIs_table().getColumns().get(0)).setVisible(false);
			}
			((TableColumn<?, ?>) getAsItIs_table().getColumns().get(0)).setVisible(true);
		});
		Platform.runLater(() -> {
			if (!getSortIt_table().getItems().isEmpty()
					|| (TableColumn<?, ?>) getSortIt_table().getColumns().get(0) != null) {
				((TableColumn<?, ?>) getSortIt_table().getColumns().get(0)).setVisible(false);
			}
			((TableColumn<?, ?>) getSortIt_table().getColumns().get(0)).setVisible(true);
		});
		Platform.runLater(() -> {
			if (!getSorted_table().getItems().isEmpty()
					|| (TableColumn<?, ?>) getSorted_table().getColumns().get(0) != null) {
				((TableColumn<?, ?>) getSorted_table().getColumns().get(0)).setVisible(false);
			}
			((TableColumn<?, ?>) getSorted_table().getColumns().get(0)).setVisible(true);
		});
	}

	void updateFolderInfoFileInfo_(TableView<FolderInfo> table) {
		ConcurrencyUtils.initExecutionService();

//		Stage stage = (Stage) table.getScene().getWindow();
		AtomicInteger ai = new AtomicInteger(0);
		Task<Void> task = new Task<Void>() {
			@SuppressWarnings("deprecation")
			@Override
			protected Void call() throws Exception {
				updateProgress(ai.get(), table.getSelectionModel().getSelectedItems().size());
				sprintf("Update Progress: " + ai.get() + " max " + table.getSelectionModel().getSelectedItems().size());
				for (FolderInfo folderInfo : table.getSelectionModel().getSelectedItems()) {
					sprintf("Reloading: " + folderInfo.getFolderPath());
					List<FileInfo> list = createFileInfo_list(folderInfo);
					if (list != null) {
						folderInfo.setFileInfoList(list);
						TableUtils.updateFolderInfos_FileInfo(folderInfo);
						TableUtils.refreshTableContent(table);
						updateProgress(ai.incrementAndGet(), table.getSelectionModel().getSelectedItems().size());
						sprintf("Update Progress: " + ai.get() + " max "
								+ table.getSelectionModel().getSelectedItems().size());
						Main.setChanged(true);
					} else {
						Messages.errorSmth(ERROR, "", null, Misc.getLineNumber(), true);
					}
				}
				return null;
			}
		};
		LoadingProcess_Task lpt = new LoadingProcess_Task();
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				sprintf("updateTableValuesFileInfo done successfully");
				lpt.closeStage();
			}
		});

		task.setOnCancelled(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				lpt.closeStage();
				Messages.warningText("Creating file info cancelled");
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				lpt.closeStage();
				Messages.errorSmth(ERROR, "", null, Misc.getLineNumber(), true);
			}
		});
		lpt.setTask(task);
		ConcurrencyUtils.exec[ConcurrencyUtils.getExecCounter()].submit(task);

	}

	public void registerTableView_obs_listener() {
		getSorted_table().getItems().addListener(listener);
		getSortIt_table().getItems().addListener(listener);
		getAsItIs_table().getItems().addListener(listener);
	}

	ListChangeListener<FolderInfo> listener = new ListChangeListener<FolderInfo>() {

		@Override
		public void onChanged(Change<? extends FolderInfo> c) {
			Main.setChanged(true);
			sprintf("listener changed");
		}

	};

	public HideButtons getHideButtons() {
		return this.hideButtons;
	}

	public void unRegister_obs_listener() {
		getSorted_table().getItems().removeListener(listener);
		getSortIt_table().getItems().removeListener(listener);
		getAsItIs_table().getItems().removeListener(listener);
	}

}
