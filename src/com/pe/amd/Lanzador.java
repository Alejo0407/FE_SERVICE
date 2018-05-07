package com.pe.amd;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.pe.amd.modelo.app.Programa;
import com.pe.amd.modelo.beans.BeanManager;
import com.pe.amd.modelo.db.ConnectionFactory;

/**
 * Main class
 * @author Diego
 *
 */
public class Lanzador implements Runnable{
	
	public static boolean cerrar = false;
	public static boolean parar = false, respaldo = false;
	
	//@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		//Inicio el Hilo que dentendra el programa
		Thread hilo = new Thread(new Lanzador());
		hilo.start();
		
		//CONEXIONES
		System.out.println("Iniciando el Servicio....\n");
		Connection csunat = null,corigen = null;
		try {
			csunat = ConnectionFactory.getConnection("sunat");
			corigen = ConnectionFactory.getConnection("carbuss");
		}catch(Exception e) {
			System.out.println("Error en la conexión a la BD");
			System.exit(1);
		}
		//PRUEBAS
		//Programa programa = new Programa(new Date(2017-1900,5,12),csunat,corigen);
		//System.out.println("Fecha Actual: " + new Date(2017-1900,5,12).toString());
		Programa programa = null;
		try {
			programa = new Programa(csunat,corigen,false);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			System.err.println(e1.getMessage());
			e1.printStackTrace();
		}
		System.out.println("Fecha Actual: " + new Date(/*2017-1900,5,12*/).toString());
		
		//HORA DEL ENVIO DE DOCUMENTOS
		String hora = "19:00";
		SAXBuilder builder = null;
		Document doc = null;
		try {
			builder = new SAXBuilder();
			doc = builder.build(new File("conf/conf.xml"));
			hora = doc.getRootElement().getChildText("resumen");
		}catch(Exception e) {System.err.println(e.getMessage());hora = "19:00";}
		
		//REALIZO EL PROCESO DE VERIFICACION
		try {
			String decoded = (String)programa.getStatus(doc.getRootElement().getChildText("ticket"),
					doc.getRootElement().getChildText("fecha"),
					doc.getRootElement().getChildText("correlativo"),
					BeanManager.COD_RESUMEN_DIARIO,false)[0];
			System.out.println("\nEstado del Documento: "+ decoded);
			
		}catch(Exception e) {System.err.println(e.getMessage());}
		
		GregorianCalendar calendar = new GregorianCalendar();
		String anio = String.format("%04d", calendar.get(GregorianCalendar.YEAR)),
				mes = String.format("%02d", calendar.get(GregorianCalendar.MONTH)+1 ),
				dia = String.format("%02d", calendar.get(GregorianCalendar.DATE));
		int h = Integer.valueOf(hora.substring(0, 2)),
				m = Integer.valueOf(hora.substring(3));

		
		System.out.println("\n\n-------------------\nLa hora para el envio del Resumen sera: " + hora + "\n\n");
		System.out.println("Iniciando la generacion y migracion de documentos....\n");
		
		
		//INICIO DEL SERVICIO 
		Object[] respuesta;
		do {
			if(!Lanzador.parar) {
				try {
					
					programa.migrarFacturas(new Date(), false);
					programa.migrarBoletas(new Date(), false);
					
					
					respuesta = programa.generarFacturas(new Date());
					Lanzador.parar |= !((Boolean)respuesta[1]).booleanValue();
					respuesta = programa.generarBoletas(new Date());
					Lanzador.parar |= !((Boolean)respuesta[1]).booleanValue();
					
					/*
					programa.migrarFacturas(new Date(2017-1900,5,12), false);
					programa.migrarBoletas(new Date(2017-1900,5,12), false);
					
					
					respuesta = programa.generarFacturas(new Date(2017-1900,5,12));
					Lanzador.parar |= !((Boolean)respuesta[1]).booleanValue();
					respuesta = programa.generarBoletas(new Date(2017-1900,5,12));
					Lanzador.parar |= !((Boolean)respuesta[1]).booleanValue();
					*/
					System.gc();
					
					Lanzador.parar = Lanzador.respaldo;
					
					if(Lanzador.parar) 
						System.out.println("El programa se ha detenido... start para iniciarlo");
					
				}catch(Exception e) {
					System.err.println(e.getMessage());
					Lanzador.parar = true;
					if(Lanzador.parar) 
						System.out.println("El programa se ha detenido... escriba start para iniciarlo");
				} 
			}
			calendar = new GregorianCalendar();
			//calendar.setTime(new Date(2017-1900,5,12));
			System.gc();
			
			if(Lanzador.cerrar) {
				programa.close();
				System.exit(0);
			}
		}while(( calendar.get(GregorianCalendar.HOUR)+12 != h 
				|| calendar.get(GregorianCalendar.MINUTE) != m )
				|| calendar.get(GregorianCalendar.AM_PM) == calendar.get(GregorianCalendar.PM));
		
		
		System.out.println("\n\nFinalizo la generacion y migracion de documentos\n");
		
		//CIERRE CON ENVIO DE RESUMEN DIARIO
		try {
			System.out.println("Cierre Diario..");
			Object[] temp = programa.generarResumenDiario(anio+mes+dia);
			String ticket = (String)temp[0];
			if(ticket != null) {
				if(doc.getRootElement().getChild("ticket") == null) 
					doc.addContent(new Element("ticket"));
				if(doc.getRootElement().getChild("fecha") == null) 
					doc.addContent(new Element("fecha"));
				if(doc.getRootElement().getChild("correlativo") == null) 
					doc.addContent(new Element("correlativo"));
				
				doc.getRootElement().getChild("ticket").addContent(ticket);
				doc.getRootElement().getChild("fecha").addContent(anio+mes+dia);
				doc.getRootElement().getChild("correlativo").addContent((String)temp[3]);
			}
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter("conf/conf.xml"));
			
			System.out.println("Ticket generado: " + ticket);
		}catch(Exception e) {System.err.println(e.getMessage());};
		System.err.println("----FIN DEL PROGRAMA----");

		programa.close();
		
		Scanner s = new Scanner(System.in);
		s.next();
		s.close();
		
	}

	@Override
	public void run() {
		System.out.println("Escriba exit para salir: ");
		Scanner scanner = new Scanner(System.in);
		String ingreso;
		boolean exit = false;
		while( !exit ) {
			ingreso = scanner.nextLine().trim();
			if(ingreso.equalsIgnoreCase("exit")) {
				Lanzador.cerrar = true;
				exit = true;
			}
			else if(ingreso.equalsIgnoreCase("start")) {

				Lanzador.parar = false;
				Lanzador.respaldo = false;
				System.out.println("START :: INVOCADO");
			}
			else if(ingreso.equalsIgnoreCase("stop")) {
				Lanzador.parar = true;
				Lanzador.respaldo = true;
				System.out.println("STOP :: INVOCADO");
				
			}
		}
		scanner.close();
	}
}
