package jcolonia.daw2020.quinielabd;

/**
 * Excepción usada generalmente en la aplicación «Quiniela 1X2» cuando un
 * partido no se encuentra operativo.
 * 
 * @versión 2021.8.1
 * @author <a href="dmartin.jcolonia@gmail.com">David H. Martín</a>
 */
public class Partido1X2Exception extends RuntimeException {
	/**
	 * Número de serie, asociado a la versión de la clase.
	 */
	private static final long serialVersionUID = 20210825001L;

	/**
	 * Crea una excepción sin ninguna información adicional.
	 */
	public Partido1X2Exception() {
		super();
	}

	/**
	 * Crea una excepción con un texto descriptivo.
	 * 
	 * @param mensaje el texto correspondiente
	 */
	public Partido1X2Exception(String mensaje) {
		super(mensaje);
	}

	/**
	 * Crea una excepción secundaria almacenando otra excepción de referencia.
	 * 
	 * @param causa la excepción –o {@link Throwable}– correspondiente
	 */
	public Partido1X2Exception(Throwable causa) {
		super(causa);
	}

	/**
	 * Crea una excepción secundaria almacenando otra excepción de referencia y un
	 * texto descriptivo.
	 * 
	 * @param mensaje el texto correspondiente
	 * @param causa   la excepción –o {@link Throwable}– correspondiente
	 */
	public Partido1X2Exception(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
