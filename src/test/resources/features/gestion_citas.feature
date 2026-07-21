Feature: Gestion de citas del taller mecanico

  # Evaluacion T1 - Grupo A - Pregunta 04
  # Alumno: Ciro Jelsin Vasquez Malpartida - Codigo: i202401303
  # PLACA: VAS-303 | DIA: 13 de setiembre de 2026 | Mecanico: Ciro Vasquez
  # Contexto: el mecanico "Ciro Vasquez" ya tiene una cita programada el DIA de 10:00 a 12:00.

  Scenario: Registrar un mantenimiento ligero con otro mecanico libre
    Given que la hora actual simulada del taller es "2026-09-12T08:00"
    And el mecanico "Ciro Vasquez" con id 1 atiende el servicio "MANTENIMIENTO_LIGERO"
    And el mecanico con id 1 ya tiene una cita "MANTENIMIENTO_LIGERO" programada desde "2026-09-13T10:00" durante 2 horas
    And el mecanico "Ciro Vasquez Malpartida" con id 2 atiende el servicio "MANTENIMIENTO_LIGERO"
    When registro una cita "MANTENIMIENTO_LIGERO" para la placa "VAS-303" con el mecanico con id 2 desde "2026-09-13T10:00"
    Then la cita queda registrada en estado "PROGRAMADA"
    And la cita registrada tiene una duracion de 2 horas
    And se notifica el agendamiento exactamente una vez

  Scenario: Rechazar el registro cuando el mecanico ocupado inicia a las 11:00
    Given que la hora actual simulada del taller es "2026-09-12T08:00"
    And el mecanico "Ciro Vasquez" con id 1 atiende el servicio "MANTENIMIENTO_LIGERO"
    And el mecanico con id 1 ya tiene una cita "MANTENIMIENTO_LIGERO" programada desde "2026-09-13T10:00" durante 2 horas
    When registro una cita "MANTENIMIENTO_LIGERO" para la placa "VAS-303" con el mecanico con id 1 desde "2026-09-13T11:00"
    Then el sistema rechaza el registro con la excepcion "HorarioOcupadoException"
    And no se guarda ninguna cita

  Scenario: Aceptar el registro cuando el mecanico ocupado inicia a las 12:00
    Given que la hora actual simulada del taller es "2026-09-12T08:00"
    And el mecanico "Ciro Vasquez" con id 1 atiende el servicio "MANTENIMIENTO_LIGERO"
    And el mecanico con id 1 ya tiene una cita "MANTENIMIENTO_LIGERO" programada desde "2026-09-13T10:00" durante 2 horas
    When registro una cita "MANTENIMIENTO_LIGERO" para la placa "VAS-303" con el mecanico con id 1 desde "2026-09-13T12:00"
    Then la cita queda registrada en estado "PROGRAMADA"
    And la cita registrada tiene una duracion de 2 horas
    And se notifica el agendamiento exactamente una vez
