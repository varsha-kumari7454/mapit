package db.migration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

public class V2__Populate_IntermediateDate_Table implements JdbcMigration {
   
		private final String BASE_PATH = "/opt/dbmigrationdata/";
		private final String MAPIT = "mapitdata2.csv";
		private final String MATCH = "Users.csv";
   
		private final String MAPIT_DELIMITER = ",";
		private final String MATCH_DELIMITER = "\\$\\$\\$";

		private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

		@Override
		public void migrate(Connection connection) throws Exception {
			
			try {
				System.out.println("Starting the process\n");
			
				createMapitTableAndInsertData(connection);
			
				createMatchTableAndInsertData(connection);
			
				joinAndInsertDataInTempTable(connection);
			
				System.out.println("Successful...");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void createMapitTableAndInsertData(Connection connection) throws Exception {

			PreparedStatement dropStmt = connection.prepareStatement("DROP TABLE IF EXISTS mapitusers;");
			dropStmt.execute();
			dropStmt.close();

			PreparedStatement createStmt = connection.prepareStatement("CREATE TABLE mapitusers(" + 
					"email VARCHAR(255) NOT NULL," + 
					"passwordhash VARCHAR(255) NOT NULL," + 
					"name text," + 
					"contactnumber VARCHAR(255)," + 
					"role VARCHAR(31) NOT NULL," + 
					"active BOOLEAN NOT NULL," + 
					"registeredat BIGINT" + 
				")");

			createStmt.execute();
			createStmt.close();
			System.out.println("Created mapit table\n");

			insertIntoMapitTable(connection, BASE_PATH + MAPIT);
		}
		
		private void insertIntoMapitTable(Connection connection, String file) throws Exception {
			
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				
				System.out.println("Inserting into mapit table from file " + file + "\n");
				
				int count = 0;
				PreparedStatement insertStmt = connection.prepareStatement(
						"INSERT INTO mapitusers(email, passwordhash, name, contactnumber, role, active, registeredat) VALUES(?,?,?,?,?,?,?)");
				//String line = reader.readLine(); // Header
				String line;
				while ((line = reader.readLine()) != null) {
					String[] cols = line.split(MAPIT_DELIMITER);
					insertStmt.setString(1, cols[0]);		//email
					insertStmt.setString(2, cols[1]);	//passwordhash
					insertStmt.setString(3, cols[2]);		//name
					cols[3] = cols[3].replaceAll("[\"\']", "");
					insertStmt.setString(4, cols[3].length()==0 ? null : cols[3]);		//contactnumber
					insertStmt.setString(5, cols[4]);		//role
					insertStmt.setBoolean(6, cols[5].equals("t") ? true : false);	//active
					insertStmt.setLong(7, Long.parseLong(cols[6]));		//registeredon
					insertStmt.addBatch();
					++count;
					if (count % 1000 == 0) {
						insertStmt.executeBatch();
					}
					System.out.println("Successfully inserted " + count + " records in mapit table\n");
				}
				insertStmt.executeBatch();
				insertStmt.close();
				
				System.out.println("Successfully inserted " + count + " records in mapit table\n");
			}
		}

		private void createMatchTableAndInsertData(Connection connection) throws Exception {

			PreparedStatement dropStmt = connection.prepareStatement("DROP TABLE IF EXISTS matchusers;");
			dropStmt.execute();
			dropStmt.close();

			PreparedStatement createStmt = connection.prepareStatement("CREATE TABLE matchusers(" + 
												"email VARCHAR(255) NOT NULL," + 
												"salt VARCHAR(255) NOT NULL," + 
												"passwordhash VARCHAR(255) NOT NULL," + 
												"name VARCHAR(255) NOT NULL," + 
												"role VARCHAR(31) NOT NULL," + 
												"active BOOLEAN NOT NULL," + 
												"registeredat BIGINT" + 
											")");

			createStmt.execute();
			createStmt.close();

			System.out.println("Created match table\n");

			insertIntoMatchTable(connection, BASE_PATH + MATCH);
			
		}
		
		private void insertIntoMatchTable(Connection connection, String file) throws Exception {

			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				
				System.out.println("Inserting into match table from file " + file + "\n");
				
				int count = 0;
				PreparedStatement insertStmt = connection.prepareStatement(
						"INSERT INTO matchusers(email, salt, passwordhash, name, role, active, registeredat) VALUES(?,?,?,?,?,?,?)");
				String line = reader.readLine(); // Header
				while ((line = reader.readLine()) != null) {
					String[] cols = line.split(MATCH_DELIMITER);
					insertStmt.setString(1, cols[1].substring(1, cols[1].length() - 1));	//email
					insertStmt.setString(2, cols[2].substring(1, cols[2].length() - 1));	//salt
					insertStmt.setString(3, cols[3].substring(1, cols[3].length() - 1));	//passwordhash
					insertStmt.setString(4, cols[4].substring(1, cols[4].length() - 1));	//name
					insertStmt.setString(5, cols[5].substring(1, cols[5].length() - 1));	//role
					insertStmt.setBoolean(6, cols[6].equalsIgnoreCase("TRUE") ? true : false);	//active
					insertStmt.setLong(7, getTimeInMillis(cols[0].replaceAll("[ISODate()\"]", "")));	//registeredon
					insertStmt.addBatch();
					++count;
					if (count % 1000 == 0) {
						insertStmt.executeBatch();
					}
					System.out.println("Successfully inserted " + count + " in match table \n");
				}
				insertStmt.executeBatch();
				insertStmt.close();
				
				System.out.println("Successfully inserted " + count + " in match table \n");
			}
		}

		private void joinAndInsertDataInTempTable(Connection connection) throws Exception {

			PreparedStatement dropStmt = connection.prepareStatement("DROP TABLE IF EXISTS intermediatedata;");
			dropStmt.execute();
			dropStmt.close();

			PreparedStatement createStmt = connection.prepareStatement("CREATE TABLE intermediatedata(" + 
												"id BIGSERIAL PRIMARY KEY," +
					
												"mapitemail VARCHAR(255)," + 
												"mapitpasswordhash VARCHAR(255)," + 
												"mapitname VARCHAR(255)," + 
												"mapitcontactnumber VARCHAR(255)," + 
												"mapitrole VARCHAR(31)," + 
												"mapitactive BOOLEAN," +
												"mapitregisteredat BIGINT," +

												"matchemail VARCHAR(255)," + 
												"matchpasswordhash VARCHAR(255)," + 
												"matchsalt VARCHAR(255)," + 
												"matchname VARCHAR(255)," + 
												"matchrole VARCHAR(31)," + 
												"matchactive BOOLEAN," +
												"matchregisteredat BIGINT," +

												"copiedtoregistereduser BOOLEAN DEFAULT FALSE," + 
												"passwordsetinregistereduser BOOLEAN DEFAULT FALSE" + 
											")");
			createStmt.execute();
			createStmt.close();
			System.out.println("Created intermediatedata table\n");

			PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO intermediatedata(" + 
							"mapitemail, mapitpasswordhash, mapitname, mapitcontactnumber, mapitrole, mapitactive, mapitregisteredat, " +
							"matchemail, matchpasswordhash, matchsalt, matchname, matchrole, matchactive, matchregisteredat) " +
							
							"SELECT p.email AS mapitemail, p.passwordhash AS mapitpasswordhash, p.name AS mapitname, p.contactnumber AS mapitcontactnumber," +
							"p.role AS mapitrole, p.active AS mapitactive, p.registeredat AS mapitregisteredat," +
							
							"t.email AS matchemail, t.passwordhash AS matchpasswordhash, t.salt AS matchsalt, t.name AS matchname," +
							"t.role AS matchrole, t.active AS matchactive, t.registeredat AS matchregisteredat " +
							
							"FROM mapitusers p FULL OUTER JOIN matchusers t ON p.email = t.email");
			insertStmt.execute();
			insertStmt.close();
			System.out.println("Inserted data into intermidatedata table\n");
		}

		private long getTimeInMillis(String time) throws Exception {
			return formatter.parse(time).getTime();
		}
}
