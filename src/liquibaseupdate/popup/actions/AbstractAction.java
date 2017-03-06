package liquibaseupdate.popup.actions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import liquibaseupdate.popup.actions.update.Update;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public abstract class AbstractAction implements IObjectActionDelegate{

	private IWorkbenchPart targetPart;

	public abstract ConnectionsEnum getConnection();

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	@Override
	public void run(IAction action) {
		try {
			Properties prop = getProperties();
			Update update = new Update(targetPart, prop);
			update.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	public Properties getProperties() throws Exception {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(getConnection().getPropertyFilePath());
			prop.load(input);
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return prop;
	}

}