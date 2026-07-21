package edu.pe.cibertec.taller.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.pe.cibertec.taller.excepcion.CitaNoCancelableException;
import edu.pe.cibertec.taller.excepcion.CitaNoEncontradaException;
import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.FechaInvalidaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.excepcion.SinDisponibilidadException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.ResultadoCancelacion;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Evaluacion T1 - Grupo A
 * Alumno: Ciro Jelsin Vasquez Malpartida - Codigo: i202401303
 *
 * Datos personales de la evaluacion:
 * - PLACA : VAS-303 (VAS de Vasquez + los ultimos 3 digitos del codigo)
 * - DIA : 13 (ultimo digito del codigo 3 + 10), en setiembre de 2026
 * - MECANICO: Ciro Vasquez (nombre + primer apellido)
 */
@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	private static final String PLACA = "VAS-303";
	private static final int DIA = 13;
	private static final int MES = 9;
	private static final int ANIO = 2026;
	private static final String MECANICO = "Ciro Vasquez";

	/** El reloj simulado del servicio se fija un dia antes del DIA, a las 08:00. */
	private static final LocalDateTime AHORA = LocalDateTime.of(ANIO, MES, DIA - 1, 8, 0);

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
	}

	/** Devuelve el DIA de la evaluacion (13/09/2026) a la hora indicada. */
	private LocalDateTime elDiaALas(int hora) {
		return LocalDateTime.of(ANIO, MES, DIA, hora, 0);
	}

	/** Devuelve el dia anterior al DIA (12/09/2026) a la hora indicada. */
	private LocalDateTime elDiaAnteriorALas(int hora) {
		return LocalDateTime.of(ANIO, MES, DIA - 1, hora, 0);
	}

	private Mecanico mecanicoCon(Long id, TipoServicio especialidad) {
		return new Mecanico(id, MECANICO, especialidad);
	}

	// =====================================================================
	// PREGUNTA 01: Registro de citas
	// =====================================================================

	@Test
	@DisplayName("P01 - Registrar un CAMBIO_ACEITE para VAS-303 el 13/09/2026 a las 10:00 lo guarda y notifica una sola vez")
	void registrarCambioAceiteExitoso() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		LocalDateTime inicio = elDiaALas(10);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(AHORA);
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaRegistrada = servicioCitas.agendarCita(1L, PLACA, TipoServicio.CAMBIO_ACEITE, inicio);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, citaRegistrada.getEstado());
		assertEquals(1, citaRegistrada.getDuracionHoras());
		assertEquals(PLACA, citaRegistrada.getPlacaVehiculo());
		assertEquals(inicio, citaRegistrada.getFechaHoraInicio());
		assertEquals(MECANICO, citaRegistrada.getMecanico().getNombre());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(citaRegistrada);
	}

	@Test
	@DisplayName("P01 - Registrar con el mecanico id 99 que no existe lanza MecanicoNoEncontradoException y no guarda nada")
	void registrarConMecanicoInexistente() {
		// Arrange
		when(repositorioMecanicos.findById(99L)).thenReturn(Optional.empty());

		// Act
		MecanicoNoEncontradoException excepcion = assertThrows(MecanicoNoEncontradoException.class,
				() -> servicioCitas.agendarCita(99L, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10)));

		// Assert
		assertEquals("No existe un mecanico con el id 99", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaAgendada(any(Cita.class));
	}

	@Test
	@DisplayName("P01 - Registrar una REPARACION_MOTOR con un mecanico de CAMBIO_ACEITE lanza EspecialidadIncorrectaException y no guarda nada")
	void registrarConEspecialidadIncorrecta() {
		// Arrange
		Mecanico mecanico = mecanicoCon(2L, TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(2L)).thenReturn(Optional.of(mecanico));

		// Act
		EspecialidadIncorrectaException excepcion = assertThrows(EspecialidadIncorrectaException.class,
				() -> servicioCitas.agendarCita(2L, PLACA, TipoServicio.REPARACION_MOTOR, elDiaALas(10)));

		// Assert
		assertEquals("El mecanico no atiende el servicio REPARACION_MOTOR", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaAgendada(any(Cita.class));
	}

	// =====================================================================
	// PREGUNTA 02: Horario de los servicios pesados
	// Regla observada en ServicioCitasImpl: si la hora es menor a 08 o mayor
	// o igual a 12, el servicio pesado se rechaza.
	// =====================================================================

	@Test
	@DisplayName("P02 - Una REPARACION_MOTOR el 13/09/2026 a las 07:00 se rechaza con HorarioNoPermitidoException")
	void reparacionMotorALasSieteSeRechaza() {
		// Arrange
		Mecanico mecanico = mecanicoCon(3L, TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(3L)).thenReturn(Optional.of(mecanico));

		// Act
		HorarioNoPermitidoException excepcion = assertThrows(HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(3L, PLACA, TipoServicio.REPARACION_MOTOR, elDiaALas(7)));

		// Assert
		assertEquals("Los servicios pesados solo se atienden entre las 08:00 y las 12:00", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("P02 - Una REPARACION_MOTOR el 13/09/2026 a las 08:00 se acepta y queda PROGRAMADA")
	void reparacionMotorALasOchoSeAcepta() {
		// Arrange
		Mecanico mecanico = mecanicoCon(3L, TipoServicio.REPARACION_MOTOR);
		LocalDateTime inicio = elDiaALas(8);
		when(repositorioMecanicos.findById(3L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(AHORA);
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaRegistrada = servicioCitas.agendarCita(3L, PLACA, TipoServicio.REPARACION_MOTOR, inicio);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, citaRegistrada.getEstado());
		assertEquals(4, citaRegistrada.getDuracionHoras());
		assertEquals(PLACA, citaRegistrada.getPlacaVehiculo());
		assertEquals(inicio, citaRegistrada.getFechaHoraInicio());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(citaRegistrada);
	}

	@Test
	@DisplayName("P02 - Una REPARACION_MOTOR el 13/09/2026 a las 11:00 se acepta aunque termine a las 15:00")
	void reparacionMotorALasOnceSeAcepta() {
		// Arrange
		Mecanico mecanico = mecanicoCon(3L, TipoServicio.REPARACION_MOTOR);
		LocalDateTime inicio = elDiaALas(11);
		when(repositorioMecanicos.findById(3L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(AHORA);
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaRegistrada = servicioCitas.agendarCita(3L, PLACA, TipoServicio.REPARACION_MOTOR, inicio);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, citaRegistrada.getEstado());
		assertEquals(4, citaRegistrada.getDuracionHoras());
		assertEquals(inicio, citaRegistrada.getFechaHoraInicio());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(citaRegistrada);
	}

	@Test
	@DisplayName("P02 - Una REPARACION_MOTOR el 13/09/2026 a las 12:00 se rechaza con HorarioNoPermitidoException")
	void reparacionMotorALasDoceSeRechaza() {
		// Arrange
		Mecanico mecanico = mecanicoCon(3L, TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(3L)).thenReturn(Optional.of(mecanico));

		// Act
		HorarioNoPermitidoException excepcion = assertThrows(HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(3L, PLACA, TipoServicio.REPARACION_MOTOR, elDiaALas(12)));

		// Assert
		assertEquals("Los servicios pesados solo se atienden entre las 08:00 y las 12:00", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	// =====================================================================
	// PREGUNTA 03: Cancelacion de citas
	// Cita CAMBIO_ACEITE de VAS-303 programada el 13/09/2026 a las 10:00.
	// =====================================================================

	@Test
	@DisplayName("P03 - Cancelar cuando faltan exactamente 24 horas no genera penalidad y deja la cita CANCELADA")
	void cancelarConExactamente24HorasDeAnticipacion() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		Cita cita = new Cita(10L, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.PROGRAMADA);
		when(repositorioCitas.findById(10L)).thenReturn(Optional.of(cita));
		when(proveedorFechaHora.ahora()).thenReturn(elDiaAnteriorALas(10));

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(10L);

		// Assert
		assertTrue(resultado.isExitoso());
		assertEquals(0.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		verify(repositorioCitas, times(1)).save(cita);
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("P03 - Cancelar cuando faltan 2 horas aplica una penalidad de 50.00")
	void cancelarConDosHorasDeAnticipacion() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		Cita cita = new Cita(11L, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.PROGRAMADA);
		when(repositorioCitas.findById(11L)).thenReturn(Optional.of(cita));
		when(proveedorFechaHora.ahora()).thenReturn(elDiaALas(8));

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(11L);

		// Assert
		assertTrue(resultado.isExitoso());
		assertEquals(50.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		verify(repositorioCitas, times(1)).save(cita);
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("P03 - Cancelar una cita ya ATENDIDA lanza CitaNoCancelableException, no la modifica ni notifica")
	void cancelarCitaYaAtendida() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		Cita cita = new Cita(12L, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.ATENDIDA);
		when(repositorioCitas.findById(12L)).thenReturn(Optional.of(cita));

		// Act
		CitaNoCancelableException excepcion = assertThrows(CitaNoCancelableException.class,
				() -> servicioCitas.cancelarCita(12L));

		// Assert
		assertEquals("Solo se pueden cancelar citas programadas", excepcion.getMessage());
		assertEquals(EstadoCita.ATENDIDA, cita.getEstado());
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaCancelada(any(Cita.class));
	}

	// =====================================================================
	// Cobertura adicional de las reglas de negocio del proyecto base
	// =====================================================================

	@Test
	@DisplayName("Extra - Agendar en una fecha del pasado lanza FechaInvalidaException")
	void agendarConFechaEnElPasado() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(AHORA);

		// Act
		FechaInvalidaException excepcion = assertThrows(FechaInvalidaException.class,
				() -> servicioCitas.agendarCita(1L, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaAnteriorALas(7)));

		// Assert
		assertEquals("La fecha de la cita debe ser posterior a la fecha actual", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Extra - Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
	void agendarConSuperposicion() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		Cita existente = new Cita(20L, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.PROGRAMADA);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(AHORA);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of(existente));

		// Act
		HorarioOcupadoException excepcion = assertThrows(HorarioOcupadoException.class,
				() -> servicioCitas.agendarCita(1L, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10)));

		// Assert
		assertEquals("El mecanico ya tiene una cita en ese horario", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Extra - Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		Cita existente = new Cita(21L, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(9), 1,
				EstadoCita.PROGRAMADA);
		LocalDateTime inicio = elDiaALas(10);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(AHORA);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of(existente));
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		// Act
		Cita citaRegistrada = servicioCitas.agendarCita(1L, PLACA, TipoServicio.CAMBIO_ACEITE, inicio);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, citaRegistrada.getEstado());
		assertEquals(inicio, citaRegistrada.getFechaHoraInicio());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(citaRegistrada);
	}

	@Test
	@DisplayName("Extra - Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		when(repositorioCitas.findById(99L)).thenReturn(Optional.empty());

		// Act
		CitaNoEncontradaException excepcion = assertThrows(CitaNoEncontradaException.class,
				() -> servicioCitas.cancelarCita(99L));

		// Assert
		assertEquals("No existe una cita con el id 99", excepcion.getMessage());
		verify(servicioNotificaciones, never()).notificarCitaCancelada(any(Cita.class));
	}

	@Test
	@DisplayName("Extra - Cancelar una cita que ya fue cancelada lanza CitaNoCancelableException")
	void cancelarCitaYaCancelada() {
		// Arrange
		Mecanico mecanico = mecanicoCon(1L, TipoServicio.CAMBIO_ACEITE);
		Cita cita = new Cita(22L, mecanico, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.CANCELADA);
		when(repositorioCitas.findById(22L)).thenReturn(Optional.of(cita));

		// Act
		CitaNoCancelableException excepcion = assertThrows(CitaNoCancelableException.class,
				() -> servicioCitas.cancelarCita(22L));

		// Assert
		assertEquals("Solo se pueden cancelar citas programadas", excepcion.getMessage());
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Extra - Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		Mecanico ocupado = new Mecanico(30L, MECANICO, TipoServicio.CAMBIO_ACEITE);
		Mecanico libre = new Mecanico(31L, MECANICO + " Malpartida", TipoServicio.CAMBIO_ACEITE);
		Cita existente = new Cita(23L, ocupado, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.PROGRAMADA);
		when(repositorioMecanicos.findByEspecialidad(TipoServicio.CAMBIO_ACEITE))
				.thenReturn(List.of(ocupado, libre));
		when(repositorioCitas.findByMecanicoIdAndEstado(30L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of(existente));
		when(repositorioCitas.findByMecanicoIdAndEstado(31L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of());

		// Act
		Mecanico encontrado = servicioCitas.buscarMecanicoDisponible(TipoServicio.CAMBIO_ACEITE, elDiaALas(10));

		// Assert
		assertEquals(libre.getId(), encontrado.getId());
		assertEquals(MECANICO + " Malpartida", encontrado.getNombre());
	}

	@Test
	@DisplayName("Extra - Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		Mecanico ocupado = mecanicoCon(30L, TipoServicio.CAMBIO_ACEITE);
		Cita existente = new Cita(24L, ocupado, PLACA, TipoServicio.CAMBIO_ACEITE, elDiaALas(10), 1,
				EstadoCita.PROGRAMADA);
		when(repositorioMecanicos.findByEspecialidad(TipoServicio.CAMBIO_ACEITE))
				.thenReturn(List.of(ocupado));
		when(repositorioCitas.findByMecanicoIdAndEstado(30L, EstadoCita.PROGRAMADA))
				.thenReturn(List.of(existente));

		// Act
		SinDisponibilidadException excepcion = assertThrows(SinDisponibilidadException.class,
				() -> servicioCitas.buscarMecanicoDisponible(TipoServicio.CAMBIO_ACEITE, elDiaALas(10)));

		// Assert
		assertEquals("No hay mecanicos disponibles para ese horario", excepcion.getMessage());
	}
}
