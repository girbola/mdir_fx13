package com.girbola.fxml.possiblefolderchooser;

import java.nio.file.Path;

import com.girbola.messages.Messages;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class PossibleFolderChooserController {
	private SimpleStringProperty path = new SimpleStringProperty();

	@FXML
	private ListView<Path> possibleFoldersList;

	@FXML
	private Button ok_btn;

	@FXML
	private Button cancel_btn;

	@FXML
	void cancel_btn_action(ActionEvent event) {
		Messages.sprintf("Cancelled");
	}

	@FXML
	void ok_btn_action(ActionEvent event) {
		Messages.sprintf("Selected: " + possibleFoldersList.getSelectionModel().getSelectedItem());
	}

	public void init(SimpleStringProperty path) {
		this.path = path;
	}

}
