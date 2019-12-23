/*
 @(#)Copyright:  Copyright (c) 2012-2019 All right reserved.
 @(#)Author:     Marko Lokka
 @(#)Product:    Image and Video Files Organizer Tool
 @(#)Purpose:    To help to organize images and video files in your harddrive with less pain
 */
package com.girbola.drive;

import java.io.File;
import java.sql.Connection;

import com.girbola.Main;
import com.girbola.controllers.folderscanner.Model_folderScanner;
import com.girbola.messages.Messages;
import com.girbola.sql.SQL_Utils;
import com.girbola.sql.SqliteConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Drive {

	private final String ERROR = Drive.class.getSimpleName();

	private ObservableList<DriveInfo> drivesList = FXCollections.observableArrayList();

	public boolean exists(DriveInfo drive) {
		for (DriveInfo di : drivesList) {
			if (di.getDrivePath().equals(drive.getDrivePath())) {
				return true;
			}
		}
		return false;
	}

	public boolean loadList(Model_folderScanner model_folderScanner) {
		Connection connection = SqliteConnection.connector(Main.conf.getAppDataPath(), Main.conf.getFolderInfo_db_fileName());
		boolean driveInfoLoaded = SQL_Utils.loadDriveInfo(connection, model_folderScanner);
		if (driveInfoLoaded) {
			return true;
		} else {
			return false;
		}
	}

	public ObservableList<DriveInfo> getDrivesList() {
		return this.drivesList;
	}

	public void saveList() {
		Connection connection = SqliteConnection.connector(Main.conf.getAppDataPath(), Main.conf.getFolderInfo_db_fileName());
		if(!SQL_Utils.isDbConnected(connection)) {
			SQL_Utils.createFolderInfoDatabase(connection);
			Messages.sprintf("createFolderInfoDatabase created");
		}
		SQL_Utils.addDriveInfo_list(connection, drivesList);
		try {
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean hasDrive(String drive) {
		for (DriveInfo di : drivesList) {
			if (di.getDrivePath().equals(drive)) {
				return true;
			}
		}
		return false;
	}

	public void createDriveInfo(String path, boolean selected) {
		boolean found = false;
		File file = new File(path);
		if (!drivesList.isEmpty()) {
			for (DriveInfo di : drivesList) {
				if (di.getDrivePath().equals(file.toString())) {
					di.setSelected(selected);
					found = true;
					break;
				} else {
					found = false;
				}
			}
			if (!found) {
				Messages.sprintf("2createDriveInfo path: " + path);
				drivesList.add(new DriveInfo(file.toString(), file.getTotalSpace(), file.exists(), selected, ""));
			}
		} else {
			Messages.sprintf("3createDriveInfo path: " + path);

			drivesList.add(new DriveInfo(file.toString(), file.getTotalSpace(), file.exists(), selected, ""));
			Messages.sprintf("drivesList size: " + drivesList.size());

		}
	}
}
