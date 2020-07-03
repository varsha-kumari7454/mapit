package dto;

public class EmailUpdateAccessDto {

	public boolean view;
	public boolean Edit;
	public boolean fullControl;

	public boolean isView() {
		return view;
	}

	public void setView(boolean view) {
		this.view = view;
	}

	public boolean isEdit() {
		return Edit;
	}

	public void setEdit(boolean edit) {
		Edit = edit;
	}

	public boolean isFullControl() {
		return fullControl;
	}

	public void setFullControl(boolean fullControl) {
		this.fullControl = fullControl;
	}

}
