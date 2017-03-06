package liquibaseupdate.popup.actions.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Scanner;

import javax.inject.Inject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class Update {

	private Shell shell;
	private IFile selectedFile;
	@Inject
	UISynchronize sync;
	private Color white;
	private Color black;
	private Properties prop;
	private static final int CLEAR_MD5 = 0;
	private static final int EXECUTE = 1;
	private static final int CANCEL = 2;

	/**
	 * @param shell
	 * @param selectedFile
	 * @param white
	 * @param black
	 */
	public Update(IWorkbenchPart targetPart, Properties prop) {
		Object obj = ((IStructuredSelection) targetPart.getSite().getSelectionProvider().getSelection()).getFirstElement();
		selectedFile = (IFile) Platform.getAdapterManager().getAdapter(obj, IFile.class);
		shell = targetPart.getSite().getShell();
		white = shell.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		black = shell.getDisplay().getSystemColor(SWT.COLOR_BLACK);
		this.prop = prop;
	}

	public void run() {
		try {
			File file = selectedFile.getLocation().toFile();
			final String fileAbsolutePath = file.toString();
			StringBuilder msg = new StringBuilder();
			msg.append("Você deseja realmente rodar o Liquibase com as configurações abaixo?");
			msg.append("\n\n\n");
			msg.append("Conexão: ").append(prop.getProperty("name"));
			msg.append("\n");
			msg.append("URL: ").append(prop.getProperty("url"));
			msg.append("\n");
			msg.append("User: ").append(prop.getProperty("username"));
			msg.append("\n");
			msg.append("Arquivo: ").append(getResumedPathFile(fileAbsolutePath));
			if (MessageDialog.openQuestion(shell, "Liquibase Update", msg.toString())) {

				JDBCConnection conn = new JDBCConnection();
				if (conn.isLocked(prop, shell)) {
					throw new RuntimeException();
				}
				if (conn.fileExistsInDatabase(getResumedPathFile(fileAbsolutePath), prop)) {
					MessageDialog dialog = new MessageDialog(shell, "Confirmação", null,
							"Esse arquivo já faz parte dos controles do Liquibase\n"
									+ "Deseja limpar o MD5 antes de executar o arquivo?\n\n"
									+ "Obs.: Limpar o MD5 é ideal para os casos onde foram adicionadas intruções no liquibase após o mesmo ter sido executado.",
							MessageDialog.QUESTION, new String[] { "Limpar MD5 e executar", "Apenas executar", "Cancelar" }, 0);
					switch (dialog.open()) {
					case CLEAR_MD5:
						conn.clearMD5(getResumedPathFile(fileAbsolutePath), prop);
						break;
					case EXECUTE:
						break;
					case CANCEL:
						throw new RuntimeException();
					default:
						break;
					}
				}

				String content = new Scanner(new File(fileAbsolutePath)).useDelimiter("\\Z").next();
				if (fileAbsolutePath.endsWith(".xml") && content.contains("http://www.liquibase.org/xml/ns/dbchangelog")) {
					Job job = new Job("Liquibase Update") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								runLiquibase(fileAbsolutePath, prop);
							} catch (Exception e) {
								showMsg(e.getMessage());
							}
							return Status.OK_STATUS;
						}
					};

					// Start the Job
					job.schedule();
				} else {
					showMsg("Selecione um xml do liquibase!");
				}
			}
		} catch (RuntimeException e) {

		} catch (Exception e) {
			showMsg("Parece que você esta sem acesso à pasta da rede \"Utilidades\" [U:]!");
		}
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				((MessageConsole) existing[i]).destroy();
			}
		}

		final MessageConsole myConsole = new MessageConsole(name, null);
		myConsole.setBackground(black);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	private void runLiquibase(String fileAbsolutePath, Properties prop) throws Exception {
		ProcessBuilder builder = new ProcessBuilder(
				"cmd.exe",
				"cmd /k \"cd /d " + fileAbsolutePath.substring(0, fileAbsolutePath.indexOf("\\liquibase"))
						+ " && java -jar " + prop.getProperty("liquibase")
						+ "	--driver=" + prop.getProperty("driver")
						+ "	--classpath=" + prop.getProperty("jdbc")
						+ "	--changeLogFile=" + getResumedPathFile(fileAbsolutePath)
						+ "	--url=" + prop.getProperty("url")
						+ "	--logLevel=" + prop.getProperty("logLevel")
						+ "	--username=" + prop.getProperty("username")
						+ "	--password=" + prop.getProperty("password")
						+ "	update\"");
		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		MessageConsole myConsole = findConsole("Liquibase Console");
		final MessageConsoleStream out = myConsole.newMessageStream();
		out.setColor(white);
		out.println("Iniciando Liquibase Update...");
		if (1 == Integer.valueOf(prop.getProperty("showCommand"))) {
			out.println("Comandos executados:");
			for (String command : builder.command()) {
				out.println("\t>>" + command.replace("	", " ").replace("&&", "^\n\t\t\t&&").replace("--", "^\n\t\t\t--").replace(" update", "^\n\t\t\tupdate"));
			}
		}
		while ((line = br.readLine()) != null) {
			out.println(line);
			if (line.isEmpty()) {
				break;
			}
		}
		p.destroy();
	}

	/**
	 * @param fileAbsolutePath
	 * @return
	 */
	private String getResumedPathFile(String fileAbsolutePath) {
		return fileAbsolutePath.substring(fileAbsolutePath.indexOf("\\liquibase") + 1);
	}

	public void showMsg(final String msg) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(shell, "LiquibaseMenu", msg);
			}
		});
	}

}
