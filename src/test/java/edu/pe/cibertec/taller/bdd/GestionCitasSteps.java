package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluacion T1 - Grupo A - Pregunta 04
 * Alumno: Ciro Jelsin Vasquez Malpartida - Codigo: i202401303
 * PLACA: VAS-303 | DIA: 13 de setiembre de 2026 | Mecanico: Ciro Vasquez
 */
public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	/** Agenda simulada de cada mecanico; el mock devuelve estas mismas listas. */
	private Map<Long, List<Cita>> agendaPorMecanico;

	private Cita citaRegistrada;
	private RuntimeException excepcionLanzada;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		agendaPorMecanico = new HashMap<>();
		citaRegistrada = null;
		excepcionLanzada = null;
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
	}

	// ----------------------------- Given -----------------------------

	@Given("que la hora actual simulada del taller es {string}")
	public void queLaHoraActualSimuladaDelTallerEs(String fechaHora) {
		// Arrange
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.parse(fechaHora));
	}

	@Given("el mecanico {string} con id {long} atiende el servicio {string}")
	public void elMecanicoConIdAtiendeElServicio(String nombre, Long id, String especialidad) {
		// Arrange
		Mecanico mecanico = new Mecanico(id, nombre, TipoServicio.valueOf(especialidad));
		List<Cita> agenda = agendaPorMecanico.computeIfAbsent(id, clave -> new ArrayList<>());
		when(repositorioMecanicos.findById(id)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado(id, EstadoCita.PROGRAMADA)).thenReturn(agenda);
	}

	@Given("el mecanico con id {long} ya tiene una cita {string} programada desde {string} durante {int} horas")
	public void elMecanicoConIdYaTieneUnaCitaProgramada(Long id, String tipo, String inicio, int duracionHoras) {
		// Arrange
		Mecanico mecanico = repositorioMecanicos.findById(id).orElseThrow();
		Cita existente = new Cita(100L, mecanico, "AAA-000", TipoServicio.valueOf(tipo),
				LocalDateTime.parse(inicio), duracionHoras, EstadoCita.PROGRAMADA);
		agendaPorMecanico.computeIfAbsent(id, clave -> new ArrayList<>()).add(existente);
	}

	// ------------------------------ When ------------------------------

	@When("registro una cita {string} para la placa {string} con el mecanico con id {long} desde {string}")
	public void registroUnaCita(String tipo, String placa, Long idMecanico, String inicio) {
		// Act
		try {
			citaRegistrada = servicioCitas.agendarCita(idMecanico, placa, TipoServicio.valueOf(tipo),
					LocalDateTime.parse(inicio));
		} catch (RuntimeException excepcion) {
			excepcionLanzada = excepcion;
		}
	}

	// ------------------------------ Then ------------------------------

	@Then("la cita queda registrada en estado {string}")
	public void laCitaQuedaRegistradaEnEstado(String estado) {
		// Assert
		assertNotNull(citaRegistrada);
		assertEquals(EstadoCita.valueOf(estado), citaRegistrada.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Then("la cita registrada tiene una duracion de {int} horas")
	public void laCitaRegistradaTieneUnaDuracionDe(int duracionHoras) {
		// Assert
		assertNotNull(citaRegistrada);
		assertEquals(duracionHoras, citaRegistrada.getDuracionHoras());
	}

	@Then("se notifica el agendamiento exactamente una vez")
	public void seNotificaElAgendamientoExactamenteUnaVez() {
		// Assert
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(citaRegistrada);
	}

	@Then("el sistema rechaza el registro con la excepcion {string}")
	public void elSistemaRechazaElRegistroConLaExcepcion(String nombreExcepcion) {
		// Assert
		assertNotNull(excepcionLanzada);
		assertEquals(nombreExcepcion, excepcionLanzada.getClass().getSimpleName());
	}

	@Then("no se guarda ninguna cita")
	public void noSeGuardaNingunaCita() {
		// Assert
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaAgendada(any(Cita.class));
	}
}
