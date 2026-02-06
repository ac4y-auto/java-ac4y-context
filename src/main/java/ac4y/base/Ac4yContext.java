package ac4y.base;

import java.io.IOException;
import java.sql.SQLException;

import ac4y.base.database.DBConnection;

public class Ac4yContext {

	public DBConnection getDBConnection(String aModul, String aClassName) throws ClassNotFoundException, SQLException, IOException, Ac4yException {
		return new DBConnection(aModul+".properties");
	}
	
}