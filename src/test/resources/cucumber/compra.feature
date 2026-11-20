Feature: Lista de la compra compartida

  Scenario: Apuntar un artículo lo clasifica por pasillo
    Given la lista de la compra está vacía
    When apunta "Leche semidesnatada" con cantidad "6 bricks"
    Then la respuesta es correcta
    And la lista tiene "Leche semidesnatada" en el pasillo "LACTEOS_HUEVOS"

  Scenario: Apuntar dos veces lo mismo no lo duplica
    Given la lista de la compra está vacía
    When apunta "Plátanos" con cantidad "1 kg"
    And apunta "platanos" con cantidad "2 kg"
    Then la lista tiene 1 artículo
    And el artículo "Plátanos" tiene cantidad "2 kg"

  Scenario: Un artículo comprado vuelve a la lista al apuntarlo otra vez
    Given la lista de la compra está vacía
    And apunta "Huevos" con cantidad "12"
    And marca "Huevos" como comprado
    When apunta "huevos" con cantidad "6"
    Then el artículo "Huevos" está pendiente

  Scenario: Los comprados van al final y se pueden limpiar
    Given la lista de la compra está vacía
    And apunta "Pan" con cantidad "2"
    And apunta "Manzanas" con cantidad "1 kg"
    And marca "Pan" como comprado
    Then la lista está en el orden "Manzanas, Pan"
    When quita los comprados
    Then la lista tiene 1 artículo

  Scenario: Un nombre vacío se rechaza
    Given la lista de la compra está vacía
    When apunta "   " con cantidad ""
    Then la respuesta es un error de validación
