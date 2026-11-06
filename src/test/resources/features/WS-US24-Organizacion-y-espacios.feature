# language: es
@WS-US-24 @WS-US-29 @WS-US-31
Característica: Organización y espacios
  Como administrador de instalaciones
  Quiero crear mi organización y registrar sus espacios físicos
  Para ubicar los sensores Clair en cada ambiente monitoreado

  Escenario: Un administrador Premium registra varios espacios en su organización
    Dado que un administrador de instalaciones con plan "PREMIUM" ha iniciado sesión
    Cuando crea la organización "Oficinas Vanana"
    Y registra los siguientes espacios en la organización:
      | nombre            | estado |
      | Sala de reuniones | 201    |
      | Laboratorio IoT   | 201    |
      | Recepción         | 201    |
    Entonces cada espacio recibe el estado indicado
    Y la organización "Oficinas Vanana" aparece en su listado de organizaciones
    Y el listado de espacios de la organización contiene:
      | nombre            |
      | Sala de reuniones |
      | Laboratorio IoT   |
      | Recepción         |

  Escenario: El plan Freemium limita la organización a un solo espacio
    Dado que un administrador de instalaciones con plan "FREEMIUM" ha iniciado sesión
    Cuando crea la organización "Casa Moreira"
    Y registra los siguientes espacios en la organización:
      | nombre     | estado |
      | Dormitorio | 201    |
      | Cocina     | 409    |
    Entonces cada espacio recibe el estado indicado
    Y el listado de espacios de la organización contiene:
      | nombre     |
      | Dormitorio |
