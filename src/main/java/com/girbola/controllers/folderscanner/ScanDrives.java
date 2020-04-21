package com.girbola.controllers.folderscanner;

import static com.girbola.messages.Messages.sprintf;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.girbola.Main;
import com.girbola.controllers.main.Model_main;
import com.girbola.drive.DriveInfo;
import com.girbola.drive.DrivesListHandler;
import com.girbola.filelisting.ValidatePathUtils;
import com.girbola.messages.Messages;
import com.girbola.misc.Misc;

import common.utils.OSHI_Utils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.util.Duration;

public class ScanDrives {

	private CheckBoxTreeItem rootItem;
//	private DriveInfo driveInfo;
	private ObservableList<Path> drivesList_selected_obs;
	private Model_folderScanner model_folderScanner;
	private Set<DriveInfo> rootDrives = new HashSet<>();
	private int i = 0;
	private int rootCount = 0;
	private DrivesListHandler drivesListHandler;
	private final static String ERROR = ScanDrives.class.getSimpleName();
	private Model_main model_Main;

	public ScanDrives(Model_main aModel_main, CheckBoxTreeItem aRootItem, ObservableList<Path> aDrivesList_selected_obs,
			DrivesListHandler aDrivesListHandler, Model_folderScanner aModel_folderScanner) {
		this.model_Main = aModel_main;
		this.rootItem = aRootItem;
		this.drivesList_selected_obs = aDrivesList_selected_obs;
		this.drivesListHandler = aDrivesListHandler;
		this.model_folderScanner = aModel_folderScanner;
		scanner.setPeriod(Duration.seconds(10));
	}

	public void restart() {
		scanner.restart();
	}

	public void stop() {
		scanner.cancel();
		sprintf("Scanning cancelled? " + scanner.isRunning());
	}

	ScheduledService<Void> scanner = new ScheduledService<Void>() {

		@Override
		protected Task createTask() {
			return new Task<Integer>() {
				@Override
				protected Integer call() throws Exception {

					File[] listOfRoots = null;
					if (com.sun.jna.Platform.isWindows()) {
						listOfRoots = File.listRoots();
					} else if (com.sun.jna.Platform.isMac()) {
						File media = new File(File.separator + "media");
						listOfRoots = media.listFiles();
					} else if (com.sun.jna.Platform.isLinux()) {
						File media = new File(File.separator + "media");
						listOfRoots = media.listFiles();
					}

					if (listOfRoots != null) {
						if (updateRootDrives(listOfRoots)) {

							Messages.sprintf("Updating root drives: " + rootDrives.size());
							rootItem.getChildren().clear();
							rootCount = listOfRoots.length;

							redrawRootFolders();

						}
					} else {
						Messages.errorSmth(ERROR, "Listing Drives list were null", null, Misc.getLineNumber(), true);
					}
					return null;
				}

				private void redrawRootFolders() throws IOException {
					for (DriveInfo driveInfo : rootDrives) {

						Messages.sprintf("Iterating root drives: " + driveInfo.getDrivePath() + " drive serial: "
								+ driveInfo.getIdentifier());

//					for (i = 0; i < listOfRoots.length; i++) {
						CheckBoxTreeItem<String> checkBoxTreeItem = createBranch(driveInfo.getDrivePath());
						DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(driveInfo.getDrivePath()));
						for (Path f : stream) {
							if (ValidatePathUtils.validFolder(f)) {
								Messages.sprintf("==== validfolderstream file: " + f);
								if (Main.conf.getUserHome().contains(f.toString())) {
									if (Files
											.exists(Paths.get(Main.conf.getUserHome() + File.separator + "Pictures"))) {
										CheckBoxTreeItem<String> checkBoxTreeItem2 = createBranch(
												Main.conf.getUserHome() + File.separator + "Pictures");
										checkBoxTreeItem2.setSelected(true);
										checkBoxTreeItem.getChildren().add(checkBoxTreeItem2);
									}
									if (Files.exists(Paths.get(Main.conf.getUserHome() + File.separator + "Videos"))) {
										CheckBoxTreeItem<String> checkBoxTreeItem3 = createBranch(
												Main.conf.getUserHome() + File.separator + "Videos");
										checkBoxTreeItem3.setSelected(true);
										checkBoxTreeItem.getChildren().add(checkBoxTreeItem3);
									}
								} else {
									CheckBoxTreeItem<String> checkBoxTreeItem2 = createBranch(f.toString());
									checkBoxTreeItem.getChildren().add(checkBoxTreeItem2);
								}
							}
						}
						rootItem.getChildren().add(checkBoxTreeItem);
					}

				}

				private boolean updateRootDrives(File[] listOfRoots) {
					Set<DriveInfo> setOfRootDrives = new HashSet<>();

					for (int i = 0; i < listOfRoots.length; i++) {
						String serial = OSHI_Utils.getDriveSerialNumber(listOfRoots[i].toString());
						Messages.sprintf("seriallllllll: " + serial + " drive: " + listOfRoots[i].toString());
						if (serial == null) {
							Main.setProcessCancelled(true);
							Messages.errorSmth(ERROR, "Can't read drives serialnumber with OSHI", null,
									Misc.getLineNumber(), true);
						}
						setOfRootDrives.add(new DriveInfo(listOfRoots[i].toString(), listOfRoots[i].getTotalSpace(),
								listOfRoots[i].exists(), false, serial));
					}

					for (DriveInfo driveInfo : setOfRootDrives) {
						if (!findDuplicateDrive(driveInfo)) {
							Messages.sprintf("Adding all to root Drives. DriveInfo: " + driveInfo.getDrivePath()
									+ " serial: " + driveInfo.getIdentifier() + " setOfRootDrives size: "
									+ setOfRootDrives.size());
							rootDrives.clear();
							rootDrives.addAll(setOfRootDrives);
							return true;
						}
					}
					return false;
				}

				private boolean findDuplicateDrive(DriveInfo driveInfoToSearch) {
					for (DriveInfo driveInfo : rootDrives) {
						Messages.sprintf(
								"222driveInfo: " + driveInfo.getDrivePath() + " serial: " + driveInfo.getIdentifier());

						if (driveInfo.getIdentifier().equals(driveInfoToSearch.getIdentifier())) {
							Messages.sprintf("Right identifier found!" + driveInfo.getDrivePath());
							return true;
						}
					}
					Messages.sprintf("Right identifier were NOT found!");
					return false;
				}

				private CheckBoxTreeItem<String> createBranch(String string) {
					CheckBoxTreeItem<String> cb = new CheckBoxTreeItem<>(string);
					cb.setExpanded(true);
					cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
						@Override
						public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
								Boolean newValue) {
							sprintf("Path selection: " + cb.getValue());
							if (newValue == true) {
								if (Main.conf.getWorkDir().contains(cb.getValue())) {
									Platform.runLater(() -> {
										cb.setSelected(false);
										Messages.warningText(Main.bundle.getString("workDirConflict"));
									});
									drivesList_selected_obs.remove(Paths.get(cb.getValue()));
								} else {
									sprintf("Path is: " + cb.getValue());
									if (Files.exists(Paths.get(cb.getValue()))) {
										if (!selectedFolderHasValue(
												model_Main.getSelectedFolders().getSelectedFolderScanner_obs(),
												Paths.get(cb.getValue()))) {
											model_Main.getSelectedFolders().getSelectedFolderScanner_obs()
													.add(new SelectedFolder(true, cb.getValue()));
										}
									}
									model_folderScanner.getSelectedDrivesFoldersList_obs()
											.add(Paths.get(cb.getValue()));
									sprintf("drive selected: " + cb.getValue());
								}
							} else {
								sprintf("drive de-selected: " + cb.getValue());
								remove(cb.getValue());

								drivesList_selected_obs.remove(Paths.get(cb.getValue()));
							}
							drivesListHandler.createDriveInfo(cb.getValue(), newValue);
						}

						private void remove(String value) {
							Iterator<SelectedFolder> it = model_Main.getSelectedFolders().getSelectedFolderScanner_obs()
									.iterator();
							while (it.hasNext()) {
								SelectedFolder selectedFolder = it.next();
								if (selectedFolder.getFolder().equals(value)) {
									it.remove();
									break;
								}
							}

						}

						private boolean selectedFolderHasValue(
								ObservableList<SelectedFolder> selectedFolderScanner_list, Path path) {
							for (SelectedFolder sf : selectedFolderScanner_list) {
								if (Paths.get(sf.getFolder()).equals(path)) {
									return true;
								}
							}
							return false;
						}
					});
					if (drivesListHandler.isDriveAlreadyInRegister(string)) {
						for (DriveInfo driveInfo : drivesListHandler.getDrivesList_obs()) {
							if (driveInfo.getDrivePath().equals(string)) {
								cb.setSelected(driveInfo.getSelected());
							}
						}
					} else {
						Messages.sprintf("has no drive in list");
						drivesListHandler.createDriveInfo(cb.getValue(), false);
					}
					return cb;
				}
			};
		}
	};

}