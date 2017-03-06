package liquibaseupdate.popup.actions.update;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.swt.widgets.Shell;

public class JDBCConnection {

	public boolean fileExistsInDatabase(String fileName, Properties prop) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean exists = false;
		try {
			Class.forName(prop.getProperty("driver"));

			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(prop.getProperty("url"), prop.getProperty("username"), prop.getProperty("password"));

			String sql = "SELECT 1 FROM DATABASECHANGELOG WHERE FILENAME = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, fileName.replace("\\", "/"));
			rs = ps.executeQuery();
			exists = rs.next();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			finallyClose(conn, ps, rs);
		}

		return exists;
	}

	public boolean isLocked(Properties prop, Shell shell) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isLocked = false;
		try {
			Class.forName(prop.getProperty("driver"));

			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(prop.getProperty("url"), prop.getProperty("username"), prop.getProperty("password"));

			String sql = " SELECT LOCKEDBY FROM DATABASECHANGELOGLOCK ";
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next()) {
				String userLocking = rs.getString("LOCKEDBY");
				if(userLocking != null ) {
					isLocked = true;
					StringBuilder msg = new StringBuilder();
					msg.append("Liquibase locked by ").append(userLocking);
					msg.append("\n\n");
					msg.append("Deseja remover o lock e continuar o processo?");
					if(MessageDialog.openQuestion(shell, "Liquibase Update", msg.toString())) {
						sql = " DELETE FROM DATABASECHANGELOGLOCK ";
						ps = conn.prepareStatement(sql);
						ps.executeQuery();
						isLocked = false;
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			finallyClose(conn, ps, rs);
		}

		return isLocked;
	}

	public void clearMD5(String fileName, Properties prop) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			Class.forName(prop.getProperty("driver"));

			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(prop.getProperty("url"), prop.getProperty("username"), prop.getProperty("password"));

			String sql = "UPDATE DATABASECHANGELOG SET MD5SUM = NULL WHERE FILENAME = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, fileName.replace("\\", "/"));
			ps.execute();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			finallyClose(conn, ps, null);
		}
	}

	private void finallyClose(Connection conn, PreparedStatement ps, ResultSet rs) {
		try {
			if (ps != null) {
				ps.close();
			}
			if (rs != null) {
				rs.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
		}
	}

}
