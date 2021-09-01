package jcolonia.daw2020.quinielabd;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

/**
 * Utilidades de acceso a una base de datos SQLite para gestión de
 * {@link ElementoPartido1X2 resultados de quiniela tipo 1-X-2}.
 * 
 * @versión 2021.8.1
 * @author <a href="dmartin.jcolonia@gmail.com">David H. Martín</a>
 */
public class AccesoBD implements AutoCloseable {
	/**
	 * Sentencia SQL para crear la tabla «Resultados» –vacía– si no existe.
	 */
	private static final String SQL_CREAR_TABLA = "CREATE TABLE IF NOT EXISTS Resultados (nombre_local TEXT NOT NULL, nombre_visitante TEXT NOT NULL, resultado TEXT NOT NULL)";

	/**
	 * Prototipo de sentenciaSQL preparada para insertar resultados.
	 */
	private static final String SQL_INSERTAR_CONTACTO = "INSERT INTO Resultados VALUES (?, ?, ?)";

	/**
	 * Sentencia SQL para obtener un volcado completo de los resultados.
	 */
	private static final String SQL_LISTADO_COMPLETO = "SELECT * FROM Resultados";

	/**
	 * Sentencia SQL para vaciar los resultados.
	 */
	private static final String SQL_VACIAR_TABLA = "DELETE FROM Resultados";

	/**
	 * Sentencia SQL para compactar espacio en el archivo de la base de datos.
	 */
	private static final String SQL_COMPACTAR_ESPACIO = "VACUUM";
	/**
	 * Nombre predeterminado del archivo de configuración del acceso a la base de
	 * datos.
	 */
	public static final String ARCHIVO_CONFIG_PREDETERMINADO = "config.xml";

	/**
	 * Nombre predeterminado del archivo de la base de datos.
	 */
	public static final String ARCHIVO_BD_PREDETERMINADO = "quiniela.db";

	/**
	 * Configuración del acceso a la base de datos.
	 */
	private Properties configuración;

	/**
	 * Conexión a la base de datos.
	 */
	private Connection conexión;

	/**
	 * Sentencia general SQL.
	 */
	private Statement sentenciaGeneralSQL;

	/**
	 * Sentencia preparada SQL, para inserciones en la base de datos.
	 * 
	 * @see #SQL_INSERTAR_CONTACTO
	 */
	private PreparedStatement preInserciónSQL;

	/**
	 * Carga la configuración desde el archivo de configuración predeterminado.
	 * 
	 * @see #ARCHIVO_CONFIG_PREDETERMINADO
	 */
	public AccesoBD() {
		this(ARCHIVO_CONFIG_PREDETERMINADO, ARCHIVO_BD_PREDETERMINADO);
	}

	/**
	 * Carga la configuración desde un archivo de configuración. En caso de error
	 * genera uno nuevo.
	 * 
	 * @param archivoConfiguración la ruta y nombre del archivo
	 * @param archivoBD
	 * @param archivoBD
	 */
	public AccesoBD(String archivoConfiguración, String archivoBD) {
		try {
			configuración = cargarConfiguración(archivoConfiguración);
		} catch (AccesoBDException e) {
			if (archivoBD == null || archivoBD.isEmpty()) {
				archivoBD = ARCHIVO_BD_PREDETERMINADO;
			}
			System.err.printf("Error cargando configuración de «%s»: %s%n", archivoConfiguración, e.getMessage());
			configuración = crearConfiguración(archivoConfiguración, archivoBD);
		}
	}

	/**
	 * Lee un archivo de configuración.
	 * 
	 * @param archivoConfiguración la ruta del archivo
	 * @return la configuración leída
	 * @throws AccesoBDException si no existe el archivo o se produce alguna
	 *                           incidencia durante la lectura
	 */
	public static Properties cargarConfiguración(String archivoConfiguración) throws AccesoBDException {
		Path rutaConfig;
		rutaConfig = Path.of(archivoConfiguración);

		if (!existeArchivo(rutaConfig)) {
			String mensaje;
			mensaje = String.format("No existe el archivo «%s»", rutaConfig.getFileName());
			throw new AccesoBDException(mensaje);
		}

		Properties configuración = new Properties();
		try (FileInputStream in = new FileInputStream(rutaConfig.toFile())) {
			configuración.loadFromXML(in);
		} catch (IOException e) {
			throw new AccesoBDException("Error al cargar configuración", e);
		}

		return configuración;
	}

	/**
	 * Crea un archivo de configuración con los datos de acceso a la base de datos
	 * en formato SQLite. El único aspecto relevante que contiene es el nombre del
	 * archivo.
	 * 
	 * @param archivoConfiguración el nombre, ruta del archivo de configuración
	 * @param archivoBD            el nombre, ruta del archivo de la base de datos
	 * @return la configuración creada
	 */
	public static Properties crearConfiguración(String archivoConfiguración, String archivoBD) {
		Path rutaConfig;
		rutaConfig = Path.of(archivoConfiguración);

		Properties configuración = new Properties();
		configuración.setProperty("jdbc.url", "jdbc:sqlite:" + archivoBD);
		configuración.setProperty("jdbc.user", "");
		configuración.setProperty("jdbc.password", "");
		configuración.setProperty("jdbc.codificación", "UTF-8");

		try (FileOutputStream out = new FileOutputStream(rutaConfig.toFile())) {
			configuración.storeToXML(out, "Configuración BD", "UTF-8");
			System.err.printf("Creado nuevo archivo de configuración «%s» para «%s»%n", archivoConfiguración,
					archivoBD);
		} catch (IOException e) {
			System.err.printf("Error al guardar configuración en «%s»: %s%n", archivoConfiguración,
					e.getLocalizedMessage());
		}
		return configuración;
	}

	/**
	 * Comprueba la existencia de un archivo.
	 * 
	 * @param ruta el nombre, ruta del archivo a comprobar
	 * @return si existe o no
	 * @throws AccesoBDException si el archivo existe pero no se puede leer
	 */
	private static boolean existeArchivo(Path ruta) throws AccesoBDException {
		boolean existe;

		existe = Files.exists(ruta);

		if (existe && !Files.isReadable(ruta)) {
			String mensaje;
			mensaje = String.format("No se puede leer el archivo «%s»", ruta.getFileName());
			throw new AccesoBDException(mensaje);
		}
		return existe;
	}

	/**
	 * Abre la conexión a la base de datos si no ha sido abierta previamente. Crea
	 * también una sentencia SQL genérica –disponible para ejecutar consultas no
	 * preparadas– y la tabla principal en caso de no existir.
	 * 
	 * @return la conexión existente o creada
	 * @throws AccesoBDException si no se completa o se produce alguna incidencia
	 *                           durante la conexión
	 */
	public Connection abrirConexión() throws AccesoBDException {
		if (conexión == null) {
			String jdbcURL = configuración.getProperty("jdbc.url");
			String jdbcUser = configuración.getProperty("jdbc.user");
			String jdbcPassword = configuración.getProperty("jdbc.password");

			try {
				conexión = DriverManager.getConnection(jdbcURL, jdbcUser, jdbcPassword);

				if (conexión == null) { // Conexión fallida
					String mensaje = String.format("%s — Conexión fallida 😕%n", jdbcURL);
					throw new AccesoBDException(mensaje);
				}

				sentenciaGeneralSQL = conexión.createStatement();
				sentenciaGeneralSQL.setQueryTimeout(5);
				sentenciaGeneralSQL.execute(SQL_CREAR_TABLA);
			} catch (SQLException e) {
				String mensaje = String.format("%s — Conexión fallida: %s", jdbcURL, e.getLocalizedMessage());
				throw new AccesoBDException(mensaje, e);
			}
		}
		return conexión;
	}

	/**
	 * Lee el contenido completo de la base de datos y crea todos los partidos. Los
	 * datos creados se depositan en la lista VACÍA facilitada. En caso de que la
	 * lista no esté vacía se borrará su contenido.
	 * 
	 * @param lista una lista de resultados vacía
	 * @return el número de resultados
	 * @throws AccesoBDException si se produce alguna incidencia
	 */
	public int leer(Vector<ElementoPartido1X2> lista) throws AccesoBDException {
		ResultSet resultado;
		ElementoPartido1X2 nuevoPartido;

		String nombreLocal, nombreVisitante, resultadoPartido;

		if (lista == null) {
			throw new AccesoBDException("Lista nula");
		}

		lista.clear();

		try {
			resultado = sentenciaGeneralSQL.executeQuery(SQL_LISTADO_COMPLETO);
			while (resultado.next()) {
				nombreLocal = resultado.getString("nombre_local");
				nombreVisitante = resultado.getString("nombre_visitante");
				resultadoPartido = resultado.getString("resultado");

				nuevoPartido = ElementoPartido1X2.of(nombreLocal, nombreVisitante, resultadoPartido);
				lista.add(nuevoPartido);
			}
		} catch (SQLException | DatoPartido1X2Exception e) {
			String mensaje = String.format("Error al leer contactos: %s", e.getLocalizedMessage());
			throw new AccesoBDException(mensaje, e);
		}

		return lista.size();
	}

	/**
	 * Inserta un resultado en la base de datos. En caso de no existir la sentencia
	 * preparada se crea -permitiendo así que se pueda compartir en caso de realizar
	 * varias inserciones consecutivas-.
	 * 
	 * @param partido el resultado a grabar
	 * @return el número de filas afectadas –cero o una…–
	 * @throws AccesoBDException si se produce alguna incidencia
	 */
	public int insertar(ElementoPartido1X2 partido) throws AccesoBDException {
		int númFilas = 0;
		try {
			if (preInserciónSQL == null) {
				preInserciónSQL = conexión.prepareStatement(SQL_INSERTAR_CONTACTO);
				preInserciónSQL.setQueryTimeout(5);
			}

			preInserciónSQL.setString(1, partido.getEquipoLocal());
			preInserciónSQL.setString(2, partido.getEquipoVisitante());
			preInserciónSQL.setString(3, partido.getResultado().toString());
			númFilas = preInserciónSQL.executeUpdate();
		} catch (SQLException e) {
			String mensaje = String.format("Error al insertar contacto: %s", e.getLocalizedMessage());
			throw new AccesoBDException(mensaje, e);
		}
		return númFilas;
	}

	/**
	 * Inserta una colección de resultados en la base de datos.
	 * 
	 * @param lista los resultados a grabar
	 * @return el número de filas afectadas, debería coincidir con el tamaño de la
	 *         colección original
	 * @throws AccesoBDException si se produce alguna incidencia
	 */
	public int escribir(Vector<ElementoPartido1X2> lista) throws AccesoBDException {
		if (lista == null) {
			throw new AccesoBDException("Lista nula");
		}

		int númFilas = 0;

		for (ElementoPartido1X2 partido : lista) {
			númFilas += insertar(partido);
		}

		return númFilas;
	}

	/**
	 * Descarta la sentencias SQL inicializadas y deja cerrada la conexión.
	 * 
	 * @throws AccesoBDException si se produce alguna incidencia
	 */
	@Override
	public void close() throws AccesoBDException {
		if (conexión != null) {
			try {
				Connection conexiónCerrada = conexión;
				conexión = null;
				sentenciaGeneralSQL = null;
				preInserciónSQL = null;
				conexiónCerrada.close();
			} catch (SQLException e) {
				String mensaje = String.format("Error en cierre de conexión: %s", e.getLocalizedMessage());
				throw new AccesoBDException(mensaje, e);
			}
		}
	}

	/**
	 * Inserta datos de ejemplo aleatorios en la base de datos. A partir de una
	 * lista de nombres de equipos los distribuye aleatoriamente en parejas y
	 * determina un resultado también aleatorio. Para el resultado se simula un dado
	 * con tres «1», dos «2» y una «X».
	 * 
	 * @throws AccesoBDException si se produce alguna incidencia al acceder a la
	 *                           base de datos
	 */
	private void generarBD() throws AccesoBDException {
		Vector<ElementoPartido1X2> lista;
		ElementoPartido1X2 nuevo;

		String nombreLocal, nombreVisitante, resultado;

		String[] equipos = { "At. Madrid", "R. Madrid", "FC Barcelona", "Sevilla FC", "RCD Espanyol", "Real Sociedad",
				"Getafe CF", "Real Betis", "Levante UD", "RC Celta", "CA Osasuna", "Rayo Vallecano", "Deportivo Alavés",
				"Elche CF", "Athletic Club", "Valencia CF", "RCD Mallorca", "Villarreal CF", "Cádiz CF", "Granada CF" };

		if (equipos.length % 2 != 0) { // Salir enseguida si no es par
			System.err.println("El número de equipos tiene que ser par para poder hacer parejas");
			System.exit(1);
		}

		Vector<String> listaEquipos = new Vector<String>(equipos.length);
		for (String equipo : equipos) {
			listaEquipos.add(equipo);
		}

		Random rnd = new Random();

		// 1 (3 caras → 50%), X (2 caras → 33,33%), 2 (1 cara → 16,66%)
		Resultado1X2[] dado1X2 = { Resultado1X2.Local1, Resultado1X2.Local1, Resultado1X2.Local1, Resultado1X2.EmpateX,
				Resultado1X2.EmpateX, Resultado1X2.Visitante2 };

		lista = new Vector<ElementoPartido1X2>();

		// Bolsa de equipos, sorteo por extracción aleatoria
		while (!listaEquipos.isEmpty()) {
			nombreLocal = listaEquipos.remove(rnd.nextInt(listaEquipos.size()));
			nombreVisitante = listaEquipos.remove(rnd.nextInt(listaEquipos.size()));
			resultado = dado1X2[rnd.nextInt(dado1X2.length)].toString();

			nuevo = new ElementoPartido1X2();
			try {
				nuevo.setDato(nombreLocal);
				nuevo.setDato(nombreVisitante);
				nuevo.setDato(resultado);
			} catch (DatoPartido1X2Exception e) {
				System.err.printf("Error de carga de datos: %s%n", e.getLocalizedMessage());
				System.exit(2);
			}

			lista.add(nuevo);
		}

		// Volcado previo a consola
		for (ElementoPartido1X2 partido : lista) {
			System.out.println(partido);
		}

		abrirConexión();
		escribir(lista);
	}

	public static void main(String[] args) {
		try (AccesoBD acceso = new AccesoBD()) {
			// Cierre implícito con close() –try_with_resources–
			acceso.generarBD();
		} catch (AccesoBDException e) {
			System.err.println(e.getLocalizedMessage());
		}
	}
}
