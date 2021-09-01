package jcolonia.daw2020.quinielabd;

/**
 * Enumeración con los tres resultados posibles en una «Quiniela 1X2».
 * 
 * @versión 2021.8.1
 * @author <a href="dmartin.jcolonia@gmail.com">David H. Martín</a>
 */
public enum Resultado1X2 {
	/**
	 * Opción de resultado tipo 1-X-2: victoria del equipo local «1».
	 */
	Local1("1"),
	/**
	 * Opción de resultado tipo 1-X-2: victoria del equipo visitante «2».
	 */
	Visitante2("2"),
	/**
	 * Opción de resultado tipo 1-X-2: empate «X».
	 */
	EmpateX("X");

	/**
	 * Carácter que representa el resultado: 1-X-2.
	 */
	private String descripción;

	/**
	 * Almacena el carácter asociado a cada resultado posible.
	 * 
	 * @param descripción el carácter correspondiente
	 */
	Resultado1X2(String descripción) {
		this.descripción = descripción;
	}

	/**
	 * Devuelve el carácter identificador del resultado.
	 * 
	 * @return el texto correspondiente
	 */
	@Override
	public String toString() {
		return descripción;
	}

	/**
	 * Devuelve un texto formateado como «[1][X][2]» mostrando el resultado.
	 * 
	 * @return el texto correspondiente
	 */
	public String to1X2String() {
		String textoResultado = null;
		switch (this) {
		case Local1:
			textoResultado = "[1][-][-]";
			break;
		case Visitante2:
			textoResultado = "[-][-][2]";
			break;
		case EmpateX:
			textoResultado = "[-][X][-]";
			break;
		}
		return textoResultado;
	}

	/**
	 * Devuelve un texto formateado como «[1][X][2]» pero vacío, sin ningún
	 * resultado. El texto es análogo al devuelto por {@link #to1X2String} para
	 * usarlo como plantilla si todavía no está definido ningún resultado.
	 * 
	 * @return el texto correspondiente
	 */
	public static String toNullString() {
		String textoResultado;
		textoResultado = "[ ][ ][ ]";
		return textoResultado;
	}
}
