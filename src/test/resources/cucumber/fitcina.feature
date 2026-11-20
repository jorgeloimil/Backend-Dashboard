Feature: Fitcina

  Scenario: Hoy toca una rutina de Fitcina
    Given Fitcina tiene la rutina "Empuje" los viernes
    When consulta lo que toca hoy en Fitcina
    Then la respuesta es correcta
    And el estado de Fitcina es "OK"
    And hoy toca la rutina "Empuje"

  Scenario: Fitcina caída no rompe el panel
    Given Fitcina no responde
    When consulta lo que toca hoy en Fitcina
    Then la respuesta es correcta
    And el estado de Fitcina es "NO_DISPONIBLE"
