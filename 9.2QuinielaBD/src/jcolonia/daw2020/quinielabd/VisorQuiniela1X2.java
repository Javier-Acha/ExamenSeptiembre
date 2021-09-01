package jcolonia.daw2020.quinielabd;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;

public class VisorQuiniela1X2 {

	private JFrame frmVisorQuiniela1X2;
	private JPanel panelExterior;
	private JPanel panelEtiqueta;
	private JPanel panelBorde;
	private JScrollPane panelDeslizante;
	private JTable tablaDatos;
	private JButton botónCargar;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					VisorQuiniela1X2 window = new VisorQuiniela1X2();
					window.frmVisorQuiniela1X2.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public VisorQuiniela1X2() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmVisorQuiniela1X2 = new JFrame();
		frmVisorQuiniela1X2.setTitle("Visor Quiniela1X2");
		frmVisorQuiniela1X2.setBounds(100, 100, 800, 320);
		frmVisorQuiniela1X2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmVisorQuiniela1X2.getContentPane().add(getPanelExterior(), BorderLayout.CENTER);
	}

	private JPanel getPanelExterior() {
		if (panelExterior == null) {
			panelExterior = new JPanel();
			panelExterior.setBorder(new EmptyBorder(10, 10, 10, 10));
			panelExterior.setLayout(new BorderLayout(10, 10));
			panelExterior.add(getPanelEtiqueta(), BorderLayout.CENTER);
			panelExterior.add(getBotónCargar(), BorderLayout.SOUTH);
		}
		return panelExterior;
	}

	private JPanel getPanelEtiqueta() {
		if (panelEtiqueta == null) {
			panelEtiqueta = new JPanel();
			panelEtiqueta.setBorder(new TitledBorder(null, "Resultados", TitledBorder.LEADING, TitledBorder.TOP, null,
					new Color(59, 59, 59)));
			panelEtiqueta.setLayout(new BorderLayout(0, 0));
			panelEtiqueta.add(getPanelBorde(), BorderLayout.CENTER);
		}
		return panelEtiqueta;
	}

	private JPanel getPanelBorde() {
		if (panelBorde == null) {
			panelBorde = new JPanel();
			panelBorde.setBorder(new EmptyBorder(10, 10, 10, 10));
			panelBorde.setLayout(new BorderLayout(10, 10));
			panelBorde.add(getPanelDeslizante(), BorderLayout.CENTER);
		}
		return panelBorde;
	}

	private JScrollPane getPanelDeslizante() {
		if (panelDeslizante == null) {
			panelDeslizante = new JScrollPane();
			panelDeslizante.setViewportView(getTablaDatos());
		}
		return panelDeslizante;
	}

	private JTable getTablaDatos() {
		if (tablaDatos == null) {
			tablaDatos = new JTable(new DefaultTableModel(new Object[][] {},
					new String[] { "#", "Local", "Visitante", "Resultado", "Datos" }));
		}
		return tablaDatos;
	}

	private JButton getBotónCargar() {
		if (botónCargar == null) {
			botónCargar = new JButton("Cargar datos");
			botónCargar.setName("botónCargar");
			botónCargar.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evento) {
					String posición, local, visitante, resultado, datos;

					int i = 0;

					try (AccesoBD bd = new AccesoBD()) { // Cierre implícito con close()
						bd.abrirConexión();
						Vector<ElementoPartido1X2> lista = new Vector<ElementoPartido1X2>();
						bd.leer(lista);
						for (ElementoPartido1X2 partido : lista) {
							posición = String.format("%d", ++i);
							local = partido.getEquipoLocal();
							visitante = partido.getEquipoVisitante();
							resultado = partido.getResultado().toString();
							datos = partido.toStringPuntos();
							añadirFila(posición, local, visitante, resultado, datos);
						}
					} catch (AccesoBDException e) {
						// TODO Bloque catch generado automáticamente
						e.printStackTrace();
					}

				}
			});
			botónCargar.setMnemonic('A');
		}
		return botónCargar;
	}

	private void añadirFila(String posición, String local, String visitante, String resultado, String datos) {
		Vector<String> nuevaFila;
		DefaultTableModel modelo = (DefaultTableModel) getTablaDatos().getModel();
		nuevaFila = new Vector<String>(modelo.getColumnCount());
		nuevaFila.add(posición);
		nuevaFila.add(local);
		nuevaFila.add(visitante);
		nuevaFila.add(resultado);
		nuevaFila.add(datos);

		modelo.addRow(nuevaFila);
	}

	private void vaciarTabla() {
		DefaultTableModel modelo = (DefaultTableModel) getTablaDatos().getModel();
		modelo.setRowCount(0);
	}
}
