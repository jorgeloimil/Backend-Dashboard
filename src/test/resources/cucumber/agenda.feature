Feature: Agenda

  Scenario: La agenda junta los eventos del calendario configurado
    Given el calendario "Familia" publica el iCal de prueba
    When consulta la agenda de 7 días
    Then la respuesta es correcta
    And la agenda incluye "Dentista" del calendario "Familia"
    And la agenda no incluye "Reunión cancelada"

  Scenario: Un calendario caído no tumba la agenda
    Given el calendario "Familia" no responde
    When consulta la agenda de 7 días
    Then la respuesta es correcta
    And el calendario "Familia" aparece como caído

  Scenario: Pedir demasiados días es un error
    When consulta la agenda de 90 días
    Then la respuesta es un error de validación
