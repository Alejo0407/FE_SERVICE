package com.pe.amd.modelo.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public final class ConnectionFactory {
	
	private static ConnectionFactory instance = new ConnectionFactory();
	
	protected ConnectionFactory() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Class.forName("org.postgresql.Driver");
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Class.forName("com.sybase.jdbc3.jdbc.SybDriver");
		}catch(Exception e) {}
	}
	
	public static Connection getConnection(String dbName) throws JDOMException, IOException, SQLException {
		if(instance == null)
			instance = new ConnectionFactory();
		
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build("conf/db_conf.xml");
		List<Element> db = doc.getRootElement().getChildren("db");
		
		Connection conn = null;
		for(Element e:db) {
			if(e.getChild("name").getText().toLowerCase().trim().equals(
					dbName.toLowerCase().trim()) ){
				String provider = e.getAttribute("provider").getValue();
				String ip = e.getChildText("ip");
				String port = e.getChildText("port");
				String user = e.getChildText("user");
				String pass = e.getChildText("pass");
				String url = "";
				if(provider.equalsIgnoreCase("mysql")) {
					url = "jdbc:mysql://"+ip+":"+port+"/"+e.getChildText("name");
				}
				
				conn = DriverManager.getConnection(url,user,pass);
				break;
			}
		}
		
		return conn;
	}
}
